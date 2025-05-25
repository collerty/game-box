package com.example.gamehub.lobby

import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

object LobbyService {
    private val firestore = FirebaseFirestore.getInstance()
    private val rooms = firestore.collection("rooms")
    private val auth = Firebase.auth

    /** Host a new game room */
    suspend fun host(
        gameId: String,
        roomName: String,
        hostName: String,
        password: String?,
        selectedColor: String? = null, // Only for ohpardon
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
            "ohpardon" -> 4
            else -> 4
        }

        // Battleships initial state
        val battleshipsState = mapOf(
            "player1Id"    to hostUid,
            "player2Id"    to null,
            "gameResult"   to null,
            "currentTurn"  to hostUid,
            "moves"        to emptyList<String>(),
            "powerUps"     to mapOf(hostUid to listOf("RADAR", "BOMB")),
            "energy"       to mapOf(hostUid to 5),
            "mapVotes"     to emptyMap<String, Int>(),
            "chosenMap"    to null,
            "powerUpMoves" to emptyList<String>()
        )

        // ohpardon initial state (customize if needed)
        val ohpardonState = mapOf(
            "gameResult" to null
        )

        val initialGameState = when (gameId) {
            "battleships" -> mapOf("battleships" to battleshipsState)
            "ohpardon"    -> mapOf("ohpardon" to ohpardonState)
            else          -> mapOf(gameId to mapOf("gameResult" to null))
        }

        val playerObj = when (gameId) {
            "ohpardon" -> mapOf(
                "uid" to hostUid,
                "name" to hostName,
                "color" to selectedColor,
                "pawns" to mapOf(
                    "pawn0" to -1,
                    "pawn1" to -1,
                    "pawn2" to -1,
                    "pawn3" to -1
                )
            )
            else -> mapOf("uid" to hostUid, "name" to hostName)
        }

        val roomData = mapOf<String, Any?>(
            "gameId" to gameId,
            "name" to roomName,
            "hostUid" to hostUid,
            "hostName" to hostName,
            "password" to password?.hashCode(),
            "maxPlayers" to maxPlayers,
            "status" to "waiting",
            "players" to listOf(playerObj),
            "gameState" to initialGameState,
            "createdAt" to FieldValue.serverTimestamp(),
            "rematchVotes" to emptyMap<String, Boolean>(),
            "createdAt" to FieldValue.serverTimestamp()
        )

        rooms.document(code).set(roomData).await()
        return code
    }

    /** Join an existing game room */
    suspend fun join(
        code: String,
        userName: String,
        password: String?,
        selectedColor: String? = null // Only for ohpardon
    ): String? {
        val user = auth.currentUser ?: return null
        val snap = rooms.document(code).get().await()
        if (!snap.exists()) return null

        val expectedHash = snap.getLong("password")?.toInt()
        if (expectedHash != null && expectedHash != password?.hashCode()) return null

        val gameId = snap.getString("gameId") ?: return null
        val maxPlayers = snap.getLong("maxPlayers")?.toInt() ?: return null
        val currentPlayers = (snap.get("players") as? List<Map<String, Any>>)?.toMutableList() ?: return null
        if (currentPlayers.size >= maxPlayers) return null

        // Color check for ohpardon
        if (gameId == "ohpardon") {
            if (selectedColor == null) return null
            val takenColors = currentPlayers.mapNotNull { it["color"] as? String }
            if (takenColors.contains(selectedColor)) {
                throw IllegalStateException("ColorAlreadyTaken")
            }
        }

        val playerObj = when (gameId) {
            "ohpardon" -> mapOf(
                "uid" to user.uid,
                "name" to userName,
                "color" to selectedColor,
                "pawns" to mapOf(
                    "pawn0" to -1,
                    "pawn1" to -1,
                    "pawn2" to -1,
                    "pawn3" to -1
                )
            )
            else -> mapOf("uid" to user.uid, "name" to userName)
        }

        // --- Battleships-specific state update ---
        val updates = mutableMapOf<String, Any>(
            "players" to FieldValue.arrayUnion(playerObj)
        )
        if (gameId == "battleships") {
            // Patch: set player2Id if not already set
            val battleshipsState = snap.get("gameState.battleships") as? Map<*, *>
            if (battleshipsState?.get("player2Id") == null) {
                updates["gameState.battleships.player2Id"] = user.uid
            }
            updates["gameState.battleships.powerUps.${user.uid}"] = listOf("RADAR", "BOMB")
            updates["gameState.battleships.energy.${user.uid}"]   = 5
        }

        rooms.document(code).update(updates).await()
        return gameId
    }

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
                    val name = doc.getString("name") ?: return@mapNotNull null
                    val host = doc.getString("hostName") ?: "?"
                    val players = (doc.get("players") as? List<*>)?.size ?: 0
                    val maxP = doc.getLong("maxPlayers")?.toInt() ?: 0
                    val locked = doc.getLong("password") != null
                    RoomSummary(doc.id, name, host, players, maxP, locked)
                }
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    /** Surrender in Battleships */
    suspend fun surrender(
        roomCode: String,
        gameId: String,
        surrenderingUid: String
    ) {
        val snap = rooms.document(roomCode).get().await()
        val players = snap.get("players") as? List<Map<String, Any>> ?: return
        val opponent = players
            .firstOrNull { it["uid"] != surrenderingUid }
            ?.get("uid") as? String ?: return

        val rematchVotes = players.associate {
            val uid = it["uid"] as? String ?: ""
            uid to false
        }

        rooms.document(roomCode).update(
            mapOf(
                "status" to "ended",
                "gameState.$gameId.gameResult" to mapOf(
                    "winner" to opponent,
                    "loser" to surrenderingUid,
                    "reason" to "surrender"
                ),
                "rematchVotes" to rematchVotes
            )
        ).await()
    }

    suspend fun voteRematch(roomCode: String, playerUid: String) {
        if (playerUid.isBlank()) return
        rooms.document(roomCode).update("rematchVotes.$playerUid", true).await()
    }

    /** Reset the game if all have rematch-voted */
    suspend fun resetGameIfRematchReady(roomCode: String, gameId: String, playerUids: List<String>) {
        val snap = rooms.document(roomCode).get().await()
        val votes = snap.get("rematchVotes") as? Map<String, Boolean> ?: emptyMap()

        // Make sure everyone voted rematch
        if (playerUids.all { votes[it] == true }) {
            val playersList = snap.get("players") as? List<Map<String, Any>> ?: return
            val firstPlayerUid = playersList.firstOrNull()?.get("uid") as? String ?: return
            resetGame(roomCode, gameId, playersList, firstPlayerUid)
        }
    }

    /** Game-specific reset! */
    private suspend fun resetGame(
        roomCode: String,
        gameId: String,
        players: List<Map<String, Any>>,
        startingPlayerUid: String
    ) {
        val resetState = when (gameId) {
            "battleships" -> mapOf(
                "player1Id"    to players.getOrNull(0)?.get("uid"),
                "player2Id"    to players.getOrNull(1)?.get("uid"),
                "gameResult"   to null,
                "currentTurn"  to startingPlayerUid,
                "moves"        to emptyList<String>(),
                "powerUps"     to players.associate { it["uid"] as String to listOf("RADAR", "BOMB") },
                "energy"       to players.associate { it["uid"] as String to 5 },
                "mapVotes"     to emptyMap<String, Int>(),
                "chosenMap"    to null,
                "powerUpMoves" to emptyList<String>()
            )
            "ohpardon" -> mapOf(
                "gameResult" to null
                // Add other ohpardon state fields if needed
            )
            else -> mapOf("gameResult" to null)
        }

        val resetVotes = players.associate {
            val uid = it["uid"] as? String ?: ""
            uid to false
        }

        rooms.document(roomCode).update(
            mapOf(
                "status" to "playing",
                "rematchVotes" to resetVotes,
                "gameState.$gameId" to resetState
            )
        ).await()
    }

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
