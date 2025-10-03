package com.example.gamehub.repository.implementations

import android.util.Log
import com.example.gamehub.features.battleships.model.BattleshipsGameRoom
import com.example.gamehub.features.battleships.model.BattleshipsGameState
import com.example.gamehub.features.battleships.model.Cell
import com.example.gamehub.features.battleships.model.Ship
import com.example.gamehub.lobby.model.Move
import com.example.gamehub.repository.interfaces.IBattleShipsRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

/**
 * BattleshipsRepository provides Firestore operations for the Battleships game.
 */
class BattleshipsRepository : BaseRepository("rooms"), IBattleShipsRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private var listenerRegistration: ListenerRegistration? = null
    private var currentRoomCode: String? = null

    // Flows for UI observation
    private val _gameRoom = MutableStateFlow<BattleshipsGameRoom?>(null)
    override val gameRoom: StateFlow<BattleshipsGameRoom?> = _gameRoom

    private val _rematchVotes = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    override val rematchVotes: StateFlow<Map<String, Boolean>> = _rematchVotes


    // --- Lifecycle/Listening ---

    override fun joinRoom(roomCode: String) {
        if (currentRoomCode != roomCode) {
            leaveRoom()
            currentRoomCode = roomCode
            listenToRoomChanges(roomCode)
        }
    }

    private fun listenToRoomChanges(roomCode: String) {
        listenerRegistration = firestore.collection("rooms")
            .document(roomCode)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("BattleshipsRepo", "Firestore error", e)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val data = snapshot.data
                    if (data != null) {
                        val gameRoom = parseGameRoom(roomCode, data)
                        _gameRoom.value = gameRoom

                        // Also update rematch votes state from the snapshot
                        val bs = (data["gameState"] as? Map<*, *>)?.get("battleships") as? Map<*, *>
                        val votes = (bs?.get("rematchVotes") as? Map<*, *>)
                            ?.mapKeys { it.key.toString() }
                            ?.mapValues { it.value == true } ?: emptyMap()
                        _rematchVotes.value = votes
                    }
                }
            }
    }

    override fun leaveRoom() {
        listenerRegistration?.remove()
        listenerRegistration = null
        currentRoomCode = null
        _gameRoom.value = null
        _rematchVotes.value = emptyMap()
    }

    override fun deleteRoom(roomCode: String, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        firestore.collection("rooms").document(roomCode)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { exception -> onError(exception) }
    }

    // --- Game Actions (Core) ---

    // Note: The original submitMove logic for updating the next player turn is simplified here.
    override fun submitMove(x: Int, y: Int, playerId: String) {
        currentRoomCode?.let { roomCode ->
            val moveMap = mapOf(
                "x" to x,
                "y" to y,
                "playerId" to playerId
            )

            // Logic to determine next player UID (simplified, better handled by backend/Cloud Functions)
            val currentRoom = _gameRoom.value
            val nextPlayerUid = if (currentRoom?.player1Id == playerId) currentRoom.player2Id else currentRoom?.player1Id

            val updates = mapOf(
                "gameState.battleships.moves" to FieldValue.arrayUnion(moveMap),
                "gameState.battleships.currentTurnUid" to nextPlayerUid
            )

            firestore.collection("rooms").document(roomCode)
                .update(updates)
                .addOnFailureListener { Log.e("BattleshipsRepo", "Failed to submit move", it) }
        }
    }

    override fun setPlayerReady(playerId: String, ships: List<Ship>) {
        currentRoomCode?.let { roomCode ->
            // ... (Ship mapping logic remains the same) ...
            // Simplified for brevity, assume maps are correctly created
            val updates = mapOf(
                "gameState.battleships.ships.$playerId" to ships.map { mapOf("startRow" to it.startRow, "startCol" to it.startCol, "size" to it.size, "orientation" to it.orientation) },
                "gameState.battleships.ready.$playerId" to true
            )

            firestore.collection("rooms").document(roomCode)
                .update(updates)
                .addOnFailureListener { Log.e("BattleshipsRepo", "Failed to set player ready state.", it) }
        }
    }

    override fun surrender(playerId: String) {
        currentRoomCode?.let { roomCode ->
            val opponentId = if (playerId == _gameRoom.value?.player1Id) _gameRoom.value?.player2Id else _gameRoom.value?.player1Id

            val updates = mapOf(
                "status" to "over",
                "gameState.battleships.surrendered" to playerId,
                "gameState.battleships.gameResult" to opponentId
            )
            firestore.collection("rooms").document(roomCode)
                .update(updates)
                .addOnFailureListener { Log.e("BattleshipsRepo", "Failed to handle surrender", it) }
        }
    }

    // --- Game Actions (Animation/Turn Management) ---

    override fun updateCurrentAttack(x: Int, y: Int, playerId: String, startedAt: Long) {
        currentRoomCode?.let { roomCode ->
            val attackMap = mapOf(
                "x" to x,
                "y" to y,
                "playerId" to playerId,
                "startedAt" to startedAt
            )
            firestore.collection("rooms").document(roomCode)
                .update("gameState.battleships.currentAttack", attackMap)
                .addOnFailureListener { Log.e("BattleshipsRepo", "Failed to update current attack", it) }
        }
    }

    override fun clearCurrentAttack() {
        currentRoomCode?.let { roomCode ->
            firestore.collection("rooms").document(roomCode)
                .update("gameState.battleships.currentAttack", FieldValue.delete())
                .addOnFailureListener { Log.e("BattleshipsRepo", "Failed to clear current attack", it) }
        }
    }

    // --- Game Actions (Power-ups/Energy) ---

    override suspend fun updateEnergy(playerId: String, amount: Int) {
        currentRoomCode?.let { roomCode ->
            firestore.collection("rooms").document(roomCode)
                .update("gameState.battleships.energy.$playerId", FieldValue.increment(amount.toLong()))
                .await()
        }
    }

    override suspend fun placeMine(playerId: String, newMine: Cell) {
        currentRoomCode?.let { roomCode ->
            val mineMap = mapOf("row" to newMine.row, "col" to newMine.col)
            firestore.collection("rooms").document(roomCode)
                // Use arrayUnion to safely add the new mine without overwriting existing ones
                .update("gameState.battleships.placedMines.$playerId", FieldValue.arrayUnion(mineMap))
                .await()
        }
    }

    // --- Game Actions (Post-game/Lobby) ---

    override suspend fun voteForRematch(playerId: String) {
        currentRoomCode?.let { roomCode ->
            firestore.collection("rooms").document(roomCode)
                .update("gameState.battleships.rematchVotes.$playerId", true)
                .await()
        }
    }

    override suspend fun endGame(roomCode: String) {
        firestore.collection("rooms").document(roomCode)
            .update("status", "ended")
            .await()
    }

    override suspend fun resetGame(roomCode: String) {
        // Complex update to reset game state for a rematch
        firestore.collection("rooms").document(roomCode)
            .update(mapOf(
                "gameState.battleships.moves" to emptyList<Any>(),
                "gameState.battleships.availablePowerUps" to emptyMap<String,Any>(),
                "gameState.battleships.energy" to emptyMap<String,Any>(),
                "gameState.battleships.powerUpMoves" to emptyList<Any>(),
                "gameState.battleships.mapVotes" to emptyMap<String,Any>(),
                "gameState.battleships.chosenMap" to null,
                "gameState.battleships.ready" to emptyMap<String,Any>(),
                "gameState.battleships.ships" to emptyMap<String,Any>(),
                "gameState.battleships.gameResult" to null,
                "gameState.battleships.rematchVotes" to emptyMap<String,Any>(),
                "gameState.battleships.surrendered" to null,
                "status" to "waiting" // Optionally change status back to waiting
            )).await()
    }

    // --- Parsing Logic (Kept for completeness) ---

    private fun parseMove(data: Map<String, Any?>): Move {
        return Move(
            x = (data["x"] as? Long)?.toInt() ?: 0,
            y = (data["y"] as? Long)?.toInt() ?: 0,
            playerId = data["playerId"] as? String ?: ""
        )
    }

    private fun parseShip(data: Map<String, Any?>): Ship {
        val orientationString = data["orientation"] as? String ?: "Horizontal"
        return Ship(
            startRow = (data["startRow"] as? Long)?.toInt() ?: 0,
            startCol = (data["startCol"] as? Long)?.toInt() ?: 0,
            size = (data["size"] as? Long)?.toInt() ?: 1,
            orientation = orientationString
        )
    }

    private fun parseCell(data: Map<String, Any?>): Cell {
        return Cell(
            row = (data["row"] as? Long)?.toInt() ?: 0,
            col = (data["col"] as? Long)?.toInt() ?: 0
        )
    }


    private fun parseBattleshipsGameState(data: Map<String, Any?>?): BattleshipsGameState {
        if (data == null) return BattleshipsGameState()

        val rawMoves = data["moves"] as? List<Map<String, Any?>> ?: emptyList()
        val moves = rawMoves.mapNotNull { moveMap -> try { parseMove(moveMap) } catch (e: Exception) { null } }

        val rawShips = data["ships"] as? Map<String, List<Map<String, Any?>>> ?: emptyMap()
        val ships = rawShips.mapValues { (_, shipList) -> shipList.mapNotNull { shipMap -> try { parseShip(shipMap) } catch (e: Exception) { null } } }

        val rawEnergy = data["energy"] as? Map<String, Long> ?: emptyMap()
        val energy = rawEnergy.mapValues { (_, value) -> value.toInt() }

        // Parse Mines (Need to handle lists of maps to list of Cells)
        val rawMines = data["placedMines"] as? Map<String, List<Map<String, Any?>>> ?: emptyMap()
        val placedMines = rawMines.mapValues { (_, mineList) -> mineList.mapNotNull { mineMap -> try { parseCell(mineMap) } catch (e: Exception) { null } } }

        // Assuming triggeredMines has the same structure
        val rawTriggeredMines = data["triggeredMines"] as? Map<String, List<Map<String, Any?>>> ?: emptyMap()
        val triggeredMines = rawTriggeredMines.mapValues { (_, mineList) -> mineList.mapNotNull { mineMap -> try { parseCell(mineMap) } catch (e: Exception) { null } } }


        val rawCurrentAttack = data["currentAttack"] as? Map<String, Any>

        return BattleshipsGameState(
            currentTurnUid = data["currentTurnUid"] as? String ?: "",
            moves = moves,
            ships = ships,
            energy = energy,
            placedMines = placedMines,
            triggeredMines = triggeredMines,
            gameResult = data["gameResult"] as? String,
            currentAttack = rawCurrentAttack
        )
    }

    private fun parseGameRoom(roomCode: String, data: Map<String, Any?>): BattleshipsGameRoom {
        val gameStateMap = data["gameState"] as? Map<String, Any?>
        val battleshipsState = gameStateMap?.get("battleships") as? Map<String, Any?>

        return BattleshipsGameRoom(
            roomCode = roomCode,
            name = data["name"] as? String ?: "",
            hostUid = data["hostUid"] as? String ?: "",
            player1Id = data["player1Id"] as? String,
            player2Id = data["player2Id"] as? String,
            status = data["status"] as? String ?: "waiting",
            chosenMap = (data["chosenMap"] as? Long)?.toInt(),
            gameState = parseBattleshipsGameState(battleshipsState),
            createdAt = data["createdAt"] as? Timestamp
        )
    }
}