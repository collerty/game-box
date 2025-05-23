package com.example.gamehub.lobby

import com.example.gamehub.features.battleships.ui.Orientation
import com.example.gamehub.lobby.codec.BattleshipsCodec
import com.example.gamehub.lobby.model.GameSession
import com.example.gamehub.lobby.model.Move
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await

class FirestoreSession(
    private val roomCode: String,
    private val codec: BattleshipsCodec
) {
    private val db = FirebaseFirestore.getInstance()
    private val room = db.collection("rooms").document(roomCode)

    /**
     * Listen to the nested `gameState.battleships` map,
     * decode it via your codec, and emit as a GameSession.
     */
    val stateFlow = callbackFlow<GameSession> {
        val registration = room.addSnapshotListener { snap, _ ->
            val gs = snap?.get("gameState") as? Map<*, *> ?: return@addSnapshotListener
            val bs = gs["battleships"] as? Map<String, Any?> ?: return@addSnapshotListener
            println(
                "FirestoreSession DEBUG: snapshot received, gameResult=${bs["gameResult"]}, moves=${bs["moves"]}, currentTurn=${bs["currentTurn"]}, rematchVotes=${bs["rematchVotes"]}, status=${snap.get("status")}"
            )
            trySend(codec.decode(bs))
        }
        awaitClose { registration.remove() }
    }.stateIn(
        scope = CoroutineScope(Dispatchers.Main),
        started = SharingStarted.Eagerly,
        initialValue = GameSession.empty(roomCode)
    )

    /**
     * Fire one shot at (x,y) and switch turns, then write
     * *only* the nested `gameState.battleships` map back.
     */
    suspend fun submitMove(x: Int, y: Int, playerId: String) {
        val snap = room.get().await()
        val gs = snap.get("gameState") as? Map<*, *> ?: error("Missing gameState")
        val bs = gs["battleships"] as? Map<String, Any?> ?: error("Missing battleships")

        val current = codec.decode(bs)
        println("DEBUG: submitMove: playerId=$playerId, player1Id=${current.player1Id}, player2Id=${current.player2Id}, gameResult=${current.gameResult}")

        // Don't allow moves after game is already won
        if (!current.gameResult.isNullOrEmpty()) {
            println("DEBUG: Game already has result (${current.gameResult}), skipping move.")
            return
        }

        // Can't play if no opponent!
        val opponent = if (playerId == current.player1Id) {
            current.player2Id ?: run {
                println("DEBUG: No opponent yet, move blocked.")
                return
            }
        } else {
            current.player1Id
        }

        val updated = current.copy(
            moves = current.moves + Move(x, y, playerId),
            currentTurn = opponent
        )

        val p1Ships = current.ships[current.player1Id] ?: emptyList()
        val p2Ships = current.player2Id?.let { current.ships[it] } ?: emptyList()
        val moves = updated.moves

        println(
            "DEBUG: p1Ships=${p1Ships.size} (${p1Ships}), p2Ships=${p2Ships.size} (${p2Ships}), moves=${moves.size} (${
                moves.map { "(${it.x},${it.y},${it.playerId})" }
            })"
        )

        fun isAllShipsSunk(
            ships: List<com.example.gamehub.lobby.model.Ship>,
            moves: List<Move>,
            ownerId: String
        ): Boolean {
            if (ships.isEmpty()) {
                println("DEBUG: isAllShipsSunk: No ships for ownerId=$ownerId, returning false")
                return false
            }
            ships.forEach { ship ->
                val shipCells = if (ship.orientation == Orientation.Horizontal) {
                    (0 until ship.size).map { offset -> Pair(ship.startRow, ship.startCol + offset) }
                } else {
                    (0 until ship.size).map { offset -> Pair(ship.startRow + offset, ship.startCol) }
                }
                shipCells.forEach { (row, col) ->
                    val hit = moves.any { it.x == col && it.y == row && it.playerId != ownerId }
                    println("DEBUG: isAllShipsSunk: shipCell=($row, $col), hit=$hit")
                }
            }
            return ships.all { ship ->
                val shipCells = if (ship.orientation == Orientation.Horizontal) {
                    (0 until ship.size).map { offset -> Pair(ship.startRow, ship.startCol + offset) }
                } else {
                    (0 until ship.size).map { offset -> Pair(ship.startRow + offset, ship.startCol) }
                }
                val allHit = shipCells.all { (row, col) ->
                    moves.any { it.x == col && it.y == row && it.playerId != ownerId }
                }
                println("DEBUG: isAllShipsSunk: ship $ship allHit=$allHit")
                allHit
            }
        }

        val isP1Sunk = isAllShipsSunk(p1Ships, moves, current.player1Id)
        val isP2Sunk = isAllShipsSunk(p2Ships, moves, current.player2Id ?: "")

        println("DEBUG: isAllShipsSunk(p1Ships, ...)=$isP1Sunk")
        println("DEBUG: isAllShipsSunk(p2Ships, ...)=$isP2Sunk")

        val winnerUid: String? = when {
            p1Ships.isNotEmpty() && isP1Sunk -> current.player2Id
            p2Ships.isNotEmpty() && isP2Sunk -> current.player1Id
            else -> null
        }

        println("DEBUG: winnerUid=$winnerUid")

        val updates = mutableMapOf<String, Any>(
            "gameState.battleships.moves" to updated.moves,
            "gameState.battleships.currentTurn" to updated.currentTurn
        )
        if (!winnerUid.isNullOrEmpty()) {
            println("DEBUG: Winner detected! Setting gameResult=$winnerUid")
            updates["gameState.battleships.gameResult"] = winnerUid
        }

        println("DEBUG: Firestore update: $updates")
        room.update(updates).await()
    }
}
