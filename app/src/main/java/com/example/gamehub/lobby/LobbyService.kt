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
    private val rooms = firestore.collection("rooms")
    private val auth = Firebase.auth

    suspend fun host(
        gameId: String,
        roomName: String,
        hostName: String,
        password: String?
    ): String {
        val hostUid = auth.currentUser?.uid ?: throw IllegalStateException("Must be signed in to host")
        val code = UUID.randomUUID().toString().replace("-", "").take(6).uppercase()

        val maxPlayers = when (gameId) {
            "battleships" -> 2
            "ohpardon" -> 4
            else -> 4
        }

        val initialGameState = mapOf(
            gameId to mapOf(
                "gameResult" to null
            )
        )

        val roomData = mapOf<String, Any?>(
            "gameId" to gameId,
            "name" to roomName,
            "hostUid" to hostUid,
            "hostName" to hostName,
            "password" to password?.hashCode(),
            "maxPlayers" to maxPlayers,
            "status" to "waiting",
            "players" to listOf(
                mapOf("uid" to hostUid, "name" to hostName)
            ),
            "gameState" to initialGameState,
            "rematchVotes" to emptyMap<String, Boolean>(),
            "createdAt" to FieldValue.serverTimestamp()
        )

        rooms.document(code).set(roomData).await()
        return code
    }

    suspend fun join(
        code: String,
        userName: String,
        password: String?
    ): String? {
        val user = auth.currentUser ?: return null
        val snap = rooms.document(code).get().await()
        if (!snap.exists()) return null

        val expectedHash = snap.getLong("password")?.toInt()
        if (expectedHash != null && expectedHash != password?.hashCode()) return null

        val maxPlayers = snap.getLong("maxPlayers")?.toInt() ?: return null
        val currentPlayers = (snap.get("players") as? List<*>)?.size ?: 0
        if (currentPlayers >= maxPlayers) return null

        val playerData = mapOf("uid" to user.uid, "name" to userName)
        rooms.document(code).update("players", FieldValue.arrayUnion(playerData)).await()

        return snap.getString("gameId")
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

    suspend fun battleshipsSurrender(roomCode: String, gameId: String, surrenderingUid: String) {
        val snap = rooms.document(roomCode).get().await()
        val players = snap.get("players") as? List<Map<String, Any>> ?: return

        val opponent = players.firstOrNull { it["uid"] != surrenderingUid }?.get("uid") as? String ?: return

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

    suspend fun battleshipsVoteRematch(roomCode: String, playerUid: String) {
        if (playerUid.isBlank()) return
        rooms.document(roomCode)
            .update("rematchVotes.$playerUid", true)
            .await()
    }

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
        // 1) new empty gameState for battleships
        val resetState = mapOf(
            "currentTurn" to startingPlayerUid,
            "moves"       to emptyList<String>(),
            "gameResult"  to null
        )
        // 2) reset rematchVotes back to all false
        val resetRematch = players.associate { (it["uid"] as? String ?: "") to false }

        // 3) clear mapVotes & chosenMap
        rooms.document(roomCode).update(
            mapOf(
                "status"                          to "started",
                "rematchVotes"                    to resetRematch,
                "gameState.$gameId"               to resetState,
                // clear out the map‐voting fields:
                "gameState.$gameId.mapVotes"      to emptyMap<String, Int>(),
                "gameState.$gameId.chosenMap"     to null
            ) as Map<String, Any?>
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
