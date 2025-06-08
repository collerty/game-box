package com.example.gamehub.lobby

import com.example.gamehub.features.whereandwhen.model.WhereAndWhenGameState // Added
import com.example.gamehub.features.whereandwhen.ui.gameChallenges // Added
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
            "whereandwhen" -> 4 // MODIFIED: Added whereandwhen
            else -> 4
        }

        // Battleships initial state
        val battleshipsState = mapOf(
            "player1Id"    to hostUid,
            "player2Id"    to null,
            "gameResult"   to null,
            "currentTurn"  to hostUid,
            "moves"        to emptyList<Any>(), // Changed to Any for flexibility if types differ
            "ships"        to emptyMap<String, Any>(), // Added for completeness
            "powerUps"     to mapOf(hostUid to listOf("RADAR", "BOMB")),
            "energy"       to mapOf(hostUid to 3),
            "mapVotes"     to emptyMap<String, Int>(),
            "chosenMap"    to null,
            "powerUpMoves" to emptyList<String>(),
            "placedMines"  to emptyMap<String, Any>(), // Added
            "triggeredMines" to emptyMap<String, Any>() // Added
        )

        // OhPardon initial state
        val ohpardonState = mapOf(
            "currentPlayer" to hostUid, // OhPardon might need current player
            "gameResult" to null,
            "diceRoll" to null // Example, adjust as per OhPardon's actual needs
            // Add other OhPardon specific initial fields here
        )

        // Where & When initial state
        val shuffledChallengeIds = gameChallenges.map { it.id }.shuffled() // Shuffle all challenge IDs
        val firstWawChallengeId = shuffledChallengeIds.firstOrNull() // Get the first from shuffled list
            ?: gameChallenges.firstOrNull()?.id // Fallback if shuffle is empty (e.g. gameChallenges is empty)
            ?: "jfk" // Absolute fallback

        val whereAndWhenState = mapOf(
            "currentRoundIndex" to 0,
            "currentChallengeId" to firstWawChallengeId, // Use the first from shuffled list
            "roundStartTimeMillis" to 0L, // Will be set by host when game actually starts
            "roundStatus" to WhereAndWhenGameState.STATUS_GUESSING,
            "playerGuesses" to emptyMap<String, Any>(),
            "roundResults" to mapOf(
                "challengeId" to "",
                "results" to emptyMap<String, Any>()
            ),
            "playersReadyForNextRound" to emptyMap<String, Boolean>(),
            "challengeOrder" to shuffledChallengeIds // Store the full shuffled order
        )

        val initialGameState = when (gameId) {
            "battleships" -> mapOf("battleships" to battleshipsState)
            "ohpardon"    -> mapOf("ohpardon" to ohpardonState)
            "whereandwhen" -> mapOf("whereandwhen" to whereAndWhenState)
            else          -> mapOf(gameId to mapOf("gameResult" to null)) // Generic fallback
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
                // OhPardon might also need an initial totalScore for players array
                // "totalScore" to 0
            )
            "whereandwhen" -> mapOf( // MODIFIED: Added
                "uid" to hostUid,
                "name" to hostName,
                "totalScore" to 0
            )
            else -> mapOf( // Default for Battleships and others
                "uid" to hostUid,
                "name" to hostName
                // Battleships players don't have totalScore in this top-level array
            )
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
            "rematchVotes" to emptyMap<String, Boolean>()
            // Note: Your provided code had two "createdAt" fields, I kept one.
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
        if (currentPlayers.any { it["uid"] == user.uid }) {
            // User is already in the room, perhaps a rejoin attempt or error.
            // Depending on game logic, you might allow rejoin or show an error.
            // For now, let's treat it as if they successfully "joined" again if they are already in.
            return gameId
        }
        if (currentPlayers.size >= maxPlayers) return null


        // Color check for ohpardon
        if (gameId == "ohpardon") {
            if (selectedColor == null) return null // OhPardon requires a color to join
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
                // "totalScore" to 0 // if OhPardon players need it in the main array
            )
            "whereandwhen" -> mapOf( // MODIFIED: Added
                "uid" to user.uid,
                "name" to userName,
                "totalScore" to 0
            )
            else -> mapOf( // Default for Battleships and others
                "uid" to user.uid,
                "name" to userName
            )
        }

        val updates = mutableMapOf<String, Any>(
            "players" to FieldValue.arrayUnion(playerObj)
        )

        if (gameId == "battleships") {
            val battleshipsGs = snap.get("gameState.battleships") as? Map<String, Any?>
            // Only set player2Id if it's not already set and there's room (which is checked by maxPlayers)
            if (battleshipsGs?.get("player2Id") == null && currentPlayers.size < maxPlayers) {
                updates["gameState.battleships.player2Id"] = user.uid
            }
            // Initialize energy and powerUps for the joining player in Battleships
            updates["gameState.battleships.powerUps.${user.uid}"] = listOf("RADAR", "BOMB")
            updates["gameState.battleships.energy.${user.uid}"]   = 3
        }
        // For "whereandwhen", no specific gameState updates are needed for the joining player here.
        // Their `playerObj` with `totalScore = 0` is added to the main `players` array.

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
                    val playersList = (doc.get("players") as? List<*>)
                    val players = playersList?.size ?: 0
                    // Ensure all players in the list are indeed maps, otherwise, count might be wrong
                    // This check is more robust if player data could be malformed.
                    // val validPlayersCount = playersList?.count { it is Map<*, *> } ?: 0
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
        gameId: String, // This param was unused, but good for context
        surrenderingUid: String
    ) {
        val snap = rooms.document(roomCode).get().await()
        val players = snap.get("players") as? List<Map<String, Any>> ?: return

        // Determine opponent based on game type if necessary
        // For Battleships (2 players), it's simple:
        val opponent = players
            .firstOrNull { (it["uid"] as? String) != surrenderingUid }
            ?.get("uid") as? String

        if (opponent == null && gameId == "battleships") {
            // This case shouldn't happen in a 2-player game if both players are present.
            // Could mean the room is in an inconsistent state or only one player.
            // For robustness, might log an error or handle appropriately.
            // For now, we'll proceed, but gameResult might be problematic.
            // Perhaps set gameResult to something like "opponent_left" or handle in client.
            rooms.document(roomCode).update("status", "ended").await() // End game without a clear winner
            return
        }


        val gameSpecificResultPath = "gameState.$gameId.gameResult"
        val updateMap = mutableMapOf<String, Any>(
            "status" to "ended"
        )

        if (opponent != null) { // Only set winner/loser if opponent is found
            updateMap[gameSpecificResultPath] = mapOf(
                "winner" to opponent,
                "loser" to surrenderingUid,
                "reason" to "surrender"
            )
        } else if (gameId != "battleships") {
            // For games with >2 players, surrender logic might be different
            // For MVP, we might just mark the player as "surrendered" and let game continue or end based on rules.
            // This example keeps it simple, similar to Battleships.
            // If only one player remains, they could be the winner.
            if (players.size == 1 && players.first()["uid"] == surrenderingUid) {
                updateMap[gameSpecificResultPath] = mapOf("reason" to "last_player_surrendered")
            }
        }


        // Assuming rematchVotes is a general feature for all games
        val rematchVotes = players.associate {
            val uid = it["uid"] as? String ?: ""
            uid to false // Reset votes
        }
        updateMap["rematchVotes"] = rematchVotes

        rooms.document(roomCode).update(updateMap).await()
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
            val firstPlayerUid = playersList.firstOrNull()?.get("uid") as? String ?: return // Or determine starting player by game rules
            resetGame(roomCode, gameId, playersList, firstPlayerUid)
        }
    }

    private suspend fun resetGame(
        roomCode: String,
        gameId: String,
        players: List<Map<String, Any>>, // List of player objects (Map<String, Any>)
        startingPlayerUid: String
    ) {
        val resetState = when (gameId) {
            "battleships" -> mapOf(
                "player1Id"    to players.getOrNull(0)?.get("uid"),
                "player2Id"    to players.getOrNull(1)?.get("uid"),
                "gameResult"   to null,
                "currentTurn"  to startingPlayerUid,
                "moves"        to emptyList<Any>(),
                "ships"        to emptyMap<String, Any>(),
                "powerUps"     to players.associate { (it["uid"] as String) to listOf("RADAR", "BOMB") },
                "energy"       to players.associate { (it["uid"] as String) to 3 },
                "mapVotes"     to emptyMap<String, Int>(),
                "chosenMap"    to null,
                "powerUpMoves" to emptyList<String>(),
                "placedMines"  to emptyMap<String, Any>(),
                "triggeredMines" to emptyMap<String, Any>()
            )
            "ohpardon" -> mapOf(
                "currentPlayer" to startingPlayerUid,
                "gameResult" to null,
                "diceRoll" to null
                // Reset other OhPardon specific fields (like pawn positions within each player in the main players array if not here)
            )
            "whereandwhen" -> { // MODIFIED: Added
                val firstWawChallengeIdReset = gameChallenges.firstOrNull()?.id ?: "jfk"
                mapOf(
                    "currentRoundIndex" to 0,
                    "currentChallengeId" to firstWawChallengeIdReset,
                    "roundStartTimeMillis" to 0L, // To be set by host/logic when round starts
                    "roundStatus" to WhereAndWhenGameState.STATUS_GUESSING,
                    "playerGuesses" to emptyMap<String, Any>(),
                    "roundResults" to mapOf(
                        "challengeId" to "",
                        "results" to emptyMap<String, Any>()
                    ),
                    "playersReadyForNextRound" to emptyMap<String, Boolean>()
                )
            }
            else -> mapOf("gameResult" to null) // Generic
        }

        val resetVotes = players.associate {
            (it["uid"] as? String ?: "") to false
        }

        // For Where & When, also reset totalScore for each player in the main players array
        val updatedPlayersArray = if (gameId == "whereandwhen") {
            players.map { playerMap ->
                playerMap.toMutableMap().apply { this["totalScore"] = 0 }
            }
        } else {
            players // No change for other games unless they also need this
        }

        rooms.document(roomCode).update(
            mapOf(
                "status" to "playing", // Or "waiting_for_start" if host needs to explicitly start round 1
                "rematchVotes" to resetVotes,
                "gameState.$gameId" to resetState,
                "players" to updatedPlayersArray // Update players array if scores were reset
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