package com.example.gamehub.lobby

import com.example.gamehub.features.whereandwhen.model.WhereAndWhenGameState // Where & When import
import com.example.gamehub.features.whereandwhen.ui.gameChallenges // Where & When import
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
import android.util.Log

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

        Log.d("LobbyService", "Creating lobby with code: $code")
        Log.d("LobbyService", "Game ID: $gameId, Room Name: $roomName, Host: $hostName")

        val maxPlayers = when (gameId) {
            "battleships" -> 2
            "ohpardon" -> 4
            "triviatoe"   -> 2
            "codenames" -> 1
            "whereandwhen" -> 4 // Where & When max players
            else -> 2
        }

        // Battleships initial state
        val battleshipsState = mapOf(
            "player1Id"    to hostUid,
            "player2Id"    to null,
            "gameResult"   to null,
            "currentTurn"  to hostUid,
            "moves"        to emptyList<String>(),
            "powerUps"     to mapOf(hostUid to listOf("RADAR", "BOMB")),
            "energy"       to mapOf(hostUid to 3),
            "mapVotes"     to emptyMap<String, Int>(),
            "chosenMap"    to null,
            "powerUpMoves" to emptyList<String>()
        )

        // ohpardon initial state
        val ohpardonState = mapOf(
            "gameResult" to null
        )

        val triviatoeState = mapOf(
            "players"      to emptyList<Map<String, Any>>(),
            "board"        to emptyList<Map<String, Any>>(),
            "moves"        to emptyList<Map<String, Any>>(),
            "currentRound" to 0,
            "quizQuestion" to null,
            "answers"      to emptyMap<String, Any>(),
            "firstToMove"  to null,
            "currentTurn"  to null,
            "winner"       to null,
            "state"        to "QUESTION"
        )

        // Where & When initial state
        val whereAndWhenState: Map<String, Any?> = if (gameId == "whereandwhen") {
            val shuffledChallengeIds = gameChallenges.map { it.id }.shuffled()
            val firstWawChallengeId = shuffledChallengeIds.firstOrNull()
                ?: gameChallenges.firstOrNull()?.id
                ?: "jfk"
            mapOf(
                "currentRoundIndex" to 0,
                "currentChallengeId" to firstWawChallengeId,
                "roundStartTimeMillis" to 0L,
                "roundStatus" to WhereAndWhenGameState.STATUS_GUESSING,
                "playerGuesses" to emptyMap<String, Any>(),
                "roundResults" to mapOf(
                    "challengeId" to "",
                    "results" to emptyMap<String, Any>()
                ),
                "playersReadyForNextRound" to emptyMap<String, Boolean>(),
                "challengeOrder" to shuffledChallengeIds
            )
        } else {
            emptyMap() // Placeholder if not W&W, though initialGameState handles this
        }


        val initialGameState = when (gameId) {
            "battleships" -> mapOf("battleships" to battleshipsState)
            "ohpardon"    -> mapOf("ohpardon" to ohpardonState)
            "triviatoe"   -> mapOf("triviatoe" to triviatoeState)
            "whereandwhen" -> mapOf("whereandwhen" to whereAndWhenState)
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
            "whereandwhen" -> mapOf( // Where & When player object
                "uid" to hostUid,
                "name" to hostName,
                "totalScore" to 0
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
            "createdAt" to FieldValue.serverTimestamp(), // Only one createdAt
            "rematchVotes" to emptyMap<String, Boolean>()
        )

        rooms.document(code).set(roomData).await()
        Log.d("LobbyService", "Lobby created successfully with code: $code")
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
        if (currentPlayers.any { it["uid"] == user.uid }) {
            return gameId
        }
        if (currentPlayers.size >= maxPlayers) return null


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
            "whereandwhen" -> mapOf( // Where & When player object
                "uid" to user.uid,
                "name" to userName,
                "totalScore" to 0
            )
            else -> mapOf("uid" to user.uid, "name" to userName)
        }

        val updates = mutableMapOf<String, Any>(
            "players" to FieldValue.arrayUnion(playerObj)
        )
        if (gameId == "battleships") {
            val battleshipsStateMap = snap.get("gameState.battleships") as? Map<*, *>
            if (battleshipsStateMap?.get("player2Id") == null) {
                updates["gameState.battleships.player2Id"] = user.uid
            }
            updates["gameState.battleships.powerUps.${user.uid}"] = listOf("RADAR", "BOMB")
            updates["gameState.battleships.energy.${user.uid}"]   = 3
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

    suspend fun resetGameIfRematchReady(roomCode: String, gameId: String, playerUids: List<String>) {
        val snap = rooms.document(roomCode).get().await()
        val votes = snap.get("rematchVotes") as? Map<String, Boolean> ?: emptyMap()

        if (playerUids.all { votes[it] == true }) {
            val playersList = snap.get("players") as? List<Map<String, Any>> ?: return
            val firstPlayerUid = playersList.firstOrNull()?.get("uid") as? String ?: return
            resetGame(roomCode, gameId, playersList, firstPlayerUid)
        }
    }

    private suspend fun resetGame(
        roomCode: String,
        gameId: String,
        players: List<Map<String, Any>>,
        startingPlayerUid: String
    ) {
        val resetState: Map<String, Any?> = when (gameId) {
            "battleships" -> mapOf(
                "player1Id"    to players.getOrNull(0)?.get("uid"),
                "player2Id"    to players.getOrNull(1)?.get("uid"),
                "gameResult"   to null,
                "currentTurn"  to startingPlayerUid,
                "moves"        to emptyList<String>(),
                "powerUps"     to players.associate { it["uid"] as String to listOf("RADAR", "BOMB") },
                "energy"       to players.associate { it["uid"] as String to 3 },
                "mapVotes"     to emptyMap<String, Int>(),
                "chosenMap"    to null,
                "powerUpMoves" to emptyList<String>()
            )
            "ohpardon" -> mapOf(
                "gameResult" to null
            )
            "triviatoe"   -> mapOf(
                "players"      to players,
                "board"        to emptyList<Map<String, Any>>(),
                "moves"        to emptyList<Map<String, Any>>(),
                "currentRound" to 0,
                "quizQuestion" to null,
                "answers"      to emptyMap<String, Any>(),
                "firstToMove"  to null,
                "currentTurn"  to startingPlayerUid,
                "winner"       to null,
                "state"        to "QUESTION"
            )
            "whereandwhen" -> { // Where & When reset state
                val newShuffledOrder = gameChallenges.map { it.id }.shuffled()
                mapOf(
                    "currentRoundIndex" to 0,
                    "currentChallengeId" to (newShuffledOrder.firstOrNull() ?: gameChallenges.firstOrNull()?.id ?: "jfk"),
                    "roundStartTimeMillis" to 0L,
                    "roundStatus" to WhereAndWhenGameState.STATUS_GUESSING,
                    "playerGuesses" to emptyMap<String, Any>(),
                    "roundResults" to mapOf("challengeId" to "", "results" to emptyMap<String,Any>()),
                    "playersReadyForNextRound" to emptyMap<String, Boolean>(),
                    "challengeOrder" to newShuffledOrder
                )
            }
            else -> mapOf("gameResult" to null)
        }

        val resetVotes = players.associate {
            val uid = it["uid"] as? String ?: ""
            uid to false
        }

        val updatedPlayersArray = if (gameId == "whereandwhen") { // Where & When player score reset
            players.map { playerMap ->
                playerMap.toMutableMap().apply { this["totalScore"] = 0 }
            }
        } else {
            players
        }

        rooms.document(roomCode).update(
            mapOf(
                "status" to "playing",
                "rematchVotes" to resetVotes,
                "gameState.$gameId" to resetState,
                "players" to updatedPlayersArray
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