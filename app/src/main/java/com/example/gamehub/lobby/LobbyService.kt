package com.example.gamehub.lobby

import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import java.util.UUID

object LobbyService {
    private val firestore = FirebaseFirestore.getInstance()
    private val rooms     = firestore.collection("rooms")
    private val auth      = Firebase.auth

    /** Host a new game room */
    suspend fun host(
        gameId: String,
        roomName: String,
        hostName: String,
        password: String?
    ): String {
        val hostUid = auth.currentUser?.uid
            ?: throw IllegalStateException("Must be signed in to host")
        val code = UUID.randomUUID()
            .toString()
            .replace("-", "")
            .take(6)
            .uppercase()

        val maxPlayers = when (gameId) {
            "battleships" -> 2
            "ohpardon"    -> 4
            else           -> 4
        }

        // --- INITIAL GAME STATE FOR BATTLESHIPS ---
        val initialGameState = mapOf(
            gameId to mapOf(
                "player1Id"   to hostUid,
                "player2Id"   to null,
                "gameResult"  to null,
                "currentTurn" to hostUid,
                "moves"       to emptyList<String>(),
                "powerUps"    to mapOf(hostUid to listOf("RADAR", "BOMB")),
                "mapVotes"    to emptyMap<String, Int>(),
                "chosenMap"   to null
            )
        )


        val roomData = mapOf<String, Any?>(
            "gameId"       to gameId,
            "name"         to roomName,
            "hostUid"      to hostUid,
            "hostName"     to hostName,
            "password"     to password?.hashCode(),
            "maxPlayers"   to maxPlayers,
            "status"       to "waiting",
            "players"      to listOf(mapOf("uid" to hostUid, "name" to hostName)),
            "gameState"    to initialGameState,
            "rematchVotes" to emptyMap<String, Boolean>(),
            "createdAt"    to FieldValue.serverTimestamp()
        )

        rooms.document(code).set(roomData).await()
        return code
    }

    /** Join an existing game room */
    suspend fun join(
        code: String,
        userName: String,
        password: String?
    ): String? {
        val user = auth.currentUser ?: return null
        val snap = rooms.document(code).get().await()
        if (!snap.exists()) return null

        // check password
        val expectedHash = snap.getLong("password")?.toInt()
        if (expectedHash != null && expectedHash != password?.hashCode()) return null

        // pull gameId safely
        val gameIdValue = snap.getString("gameId") ?: return null

        // enforce maxPlayers
        val maxPlayers     = snap.getLong("maxPlayers")?.toInt() ?: return null
        val currentPlayers = (snap.get("players") as? List<*>)?.size ?: 0
        if (currentPlayers >= maxPlayers) return null

        // add to players list
        val playerData = mapOf("uid" to user.uid, "name" to userName)
        // 1) Add the joiner to the top-level players list
        // 2) If Battleships, also seed their power-ups
        val updates = mutableMapOf<String, Any>(
            "players" to FieldValue.arrayUnion(playerData)
        )
        if (snap.getString("gameId") == "battleships") {
            updates["gameState.battleships.powerUps.${user.uid}"] =
                listOf("RADAR", "BOMB")
        }
        // Apply both updates in one shot
        rooms.document(code).update(updates).await()   // :contentReference[oaicite:0]{index=0}

        return snap.getString("gameId")
    }

    /** Stream of public rooms for a given game */
    fun publicRoomsFlow(gameId: String): Flow<List<RoomSummary>> = callbackFlow {
        val registration = rooms
            .whereEqualTo("gameId", gameId)
            .whereEqualTo("status", "waiting")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { qs, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }
                val list = qs!!.documents.mapNotNull { doc ->
                    val name    = doc.getString("name")     ?: return@mapNotNull null
                    val host    = doc.getString("hostName") ?: "?"
                    val players = (doc.get("players") as? List<*>)?.size ?: 0
                    val maxP    = doc.getLong("maxPlayers")?.toInt() ?: 0
                    val locked  = doc.getLong("password") != null
                    RoomSummary(doc.id, name, host, players, maxP, locked)
                }
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    /** Surrender in Battleships */
    suspend fun battleshipsSurrender(
        roomCode: String,
        gameId: String,
        surrenderingUid: String
    ) {
        val snap    = rooms.document(roomCode).get().await()
        val players = snap.get("players") as? List<Map<String, Any>> ?: return
        val opponent = players
            .firstOrNull { it["uid"] != surrenderingUid }
            ?.get("uid") as? String ?: return

        val rematchVotes = players.associate {
            (it["uid"] as? String ?: "") to false
        }

        rooms.document(roomCode).update(
            mapOf(
                "status"                          to "ended",
                "gameState.$gameId.gameResult"   to mapOf(
                    "winner" to opponent,
                    "loser"  to surrenderingUid,
                    "reason" to "surrender"
                ),
                "rematchVotes"                    to rematchVotes
            )
        ).await()
    }

    /** Vote for a rematch */
    suspend fun battleshipsVoteRematch(roomCode: String, playerUid: String) {
        if (playerUid.isBlank()) return
        rooms.document(roomCode)
            .update("rematchVotes.$playerUid", true)
            .await()
    }

    /** Reset the game if all have rematch-voted */
    suspend fun battleshipsResetGameIfRematchReady(
        roomCode: String,
        gameId: String,
        playerUids: List<String>
    ) {
        val snap  = rooms.document(roomCode).get().await()
        val votes = snap.get("rematchVotes") as? Map<String, Boolean> ?: emptyMap()
        if (playerUids.all { votes[it] == true }) {
            val playersList = snap.get("players") as? List<Map<String, Any>> ?: return
            val firstPlayer = playersList.firstOrNull()?.get("uid") as? String ?: return
            battleshipsResetGame(roomCode, gameId, playersList, firstPlayer)
        }
    }

    private suspend fun battleshipsResetGame(
        roomCode: String,
        gameId: String,
        players: List<Map<String, Any>>,
        startingPlayerUid: String
    ) {
        // clear state & votes
        val resetState = mapOf(
            "currentTurn" to startingPlayerUid,
            "moves"       to emptyList<String>(),
            "gameResult"  to null
        )
        val resetRematch = players.associate {
            (it["uid"] as? String ?: "") to false
        }

        rooms.document(roomCode).update(
            mapOf(
                "status"                          to "started",
                "rematchVotes"                    to resetRematch,
                "gameState.$gameId"               to resetState,
                "gameState.$gameId.mapVotes"      to emptyMap<String, Int>(),
                "gameState.$gameId.chosenMap"     to null
            ) as Map<String, Any?>
        ).await()
    }

    /** Delete a room entirely */
    suspend fun deleteRoom(roomCode: String) {
        rooms.document(roomCode).delete().await()
    }

    data class RoomSummary(
        val code: String,
        val name: String,
        val hostName: String,
        val currentPlayers: Int,
        val maxPlayers: Int,
        val hasPassword: Boolean
    )
}
