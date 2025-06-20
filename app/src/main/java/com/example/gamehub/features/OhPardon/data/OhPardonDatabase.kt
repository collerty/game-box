package com.example.gamehub.features.ohpardon.data

import android.util.Log
import androidx.compose.ui.graphics.Color
import com.example.gamehub.features.ohpardon.models.GameRoom
import com.example.gamehub.features.ohpardon.models.GameState
import com.example.gamehub.features.ohpardon.models.Pawn
import com.example.gamehub.features.ohpardon.models.Player
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class OhPardonDatabase(private val roomCode: String) {

    private val firestore = FirebaseFirestore.getInstance()
    private var listenerRegistration: ListenerRegistration? = null

    fun listenToRoomChanges(onRoomUpdated: (GameRoom) -> Unit) {
        listenerRegistration = firestore.collection("rooms")
            .document(roomCode)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("OhPardonRepo", "Firestore error", e)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val data = snapshot.data
                    if (data != null) {
                        val gameRoom = parseGameRoom(data)
                        onRoomUpdated(gameRoom)
                    }
                }
            }
    }

    fun updateDiceRoll(diceRoll: Int) {
        firestore.collection("rooms").document(roomCode)
            .update("gameState.ohpardon.diceRoll", diceRoll)
            .addOnSuccessListener {
                Log.d("OhPardonRepo", "Dice rolled: $diceRoll")
            }
            .addOnFailureListener {
                Log.e("OhPardonRepo", "Failed to update dice roll", it)
            }
    }

    fun skipTurn(nextPlayerUid: String) {
        val updates = mapOf(
            "gameState.ohpardon.diceRoll" to null,
            "gameState.ohpardon.currentPlayer" to nextPlayerUid
        )
        firestore.collection("rooms")
            .document(roomCode)
            .update(updates)
    }

    fun updateGameState(updatedPlayers: List<Player>, nextPlayerUid: String, isGameOver: Boolean) {
        val updatedPlayerMaps = updatedPlayers.map { p ->
            mapOf(
                "uid" to p.uid,
                "name" to p.name,
                "color" to when (p.color) {
                    Color.Red -> "red"
                    Color.Green -> "green"
                    Color.Blue -> "blue"
                    Color.Yellow -> "yellow"
                    else -> "red"
                },
                "pawns" to p.pawns.associate { it.id to it.position }
            )
        }

        val updates = mapOf(
            "players" to updatedPlayerMaps,
            "gameState.ohpardon.diceRoll" to null,
            "gameState.ohpardon.currentPlayer" to nextPlayerUid
        )

        firestore.collection("rooms")
            .document(roomCode)
            .update(updates)
            .addOnSuccessListener {
                Log.d("OhPardonRepo", "Game state updated")
            }
            .addOnFailureListener {
                Log.e("OhPardonRepo", "Failed to update game state", it)
            }
    }

    fun endGame(winnerName: String) {
        val updates = mapOf(
            "gameState.ohpardon.gameResult" to "$winnerName wins!",
            "status" to "over"
        )

        firestore.collection("rooms")
            .document(roomCode)
            .update(updates)
            .addOnSuccessListener {
                Log.d("OhPardonRepo", "Game over. $winnerName wins!")
            }
            .addOnFailureListener {
                Log.e("OhPardonRepo", "Failed to update game over state", it)
            }
    }

    fun parseColor(colorStr: String): Color {
        return when (colorStr.lowercase()) {
            "red" -> Color.Red
            "green" -> Color.Green
            "blue" -> Color.Blue
            "yellow" -> Color.Yellow
            else -> Color.Red
        }
    }

    private fun parsePlayer(data: Map<String, Any?>): Player {
        val pawnsMap = data["pawns"] as? Map<String, Long> ?: emptyMap()

        val pawns = pawnsMap.map { (key, position) ->
            Pawn(id = key, position = position.toInt())
        }

        return Player(
            uid = data["uid"] as? String ?: "",
            name = data["name"] as? String ?: "",
            color = parseColor(data["color"] as? String ?: "red"),
            pawns = pawns
        )
    }

    private fun parseGameState(data: Map<String, Any?>?): GameState {
        val currentPlayer = (data?.get("currentPlayer") as? String)
            ?: (data?.get("gameState.ohpardon.currentPlayer") as? String)
            ?: ""

        return GameState(
            currentTurnUid = currentPlayer,
            diceRoll = (data?.get("diceRoll") as? Long)?.toInt(),
            gameResult = data?.get("gameResult") as? String ?: "Ongoing"
        )
    }

    private fun parseGameRoom(data: Map<String, Any?>): GameRoom {
        val playersList = (data["players"] as? List<Map<String, Any?>>)?.map { playerData ->
            parsePlayer(playerData)
        } ?: emptyList()

        val gameStateMap = data["gameState"] as? Map<String, Any?>
        val ohPardonState = gameStateMap?.get("ohpardon") as? Map<String, Any?>

        return GameRoom(
            gameId = data["gameId"] as? String ?: "",
            name = data["name"] as? String ?: "",
            hostUid = data["hostUid"] as? String ?: "",
            hostName = data["hostName"] as? String ?: "",
            passwordHash = (data["password"] as? Long)?.toInt(),
            maxPlayers = (data["maxPlayers"] as? Long)?.toInt() ?: 4,
            status = data["status"] as? String ?: "waiting",
            players = playersList,
            gameState = parseGameState(ohPardonState),
            createdAt = data["createdAt"] as? Timestamp
        )
    }

    fun removeListeners() {
        listenerRegistration?.remove()
    }
}
