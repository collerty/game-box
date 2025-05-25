package com.example.gamehub.lobby

import com.example.gamehub.features.battleships.model.Cell
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
            val bs = (gs["battleships"] as? Map<*, *>)?.entries?.associate { it.key as String to it.value } ?: return@addSnapshotListener

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
        val bs = (gs["battleships"] as? Map<*, *>)?.entries?.associate { it.key as String to it.value } ?: error("Missing battleships")

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

        val move = Move(x, y, playerId)
        val newMoves = current.moves + move

        // --- MINE RETALIATION LOGIC ---
        val mineRetaliationUpdates = mutableMapOf<String, Any>()
        var newPlacedMines = current.placedMines.toMutableMap()

        // 1. Check if player hits a mine on opponent's board
        val defenderMines = current.placedMines[opponent] ?: emptyList()
        val moveCell = Cell(y, x)
        val hitMine = defenderMines.contains(moveCell)

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

        if (hitMine) {
            // 2. Remove mine that was hit
            val updatedOpponentMines = defenderMines.filter { it != moveCell }
            newPlacedMines[opponent] = updatedOpponentMines
            val prevTriggered = current.triggeredMines[opponent] ?: emptyList()
            val newTriggered = prevTriggered + moveCell
            val newTriggeredMines = current.triggeredMines.toMutableMap()
            newTriggeredMines[opponent] = newTriggered

            // 3. Retaliate: pick 3 random valid (not yet attacked) cells on attacker's board
            val attackedCells = newMoves.filter { it.playerId == opponent }.map { Cell(it.y, it.x) }.toSet()
            val allCells = (0 until 10).flatMap { row -> (0 until 10).map { col -> Cell(row, col) } }
            val unhitCells = allCells.filter { it !in attackedCells }

            // Pick up to 3 random
            val retaliationTargets = unhitCells.shuffled().take(3)

            val retaliationMoves = retaliationTargets.map { cell ->
                Move(cell.col, cell.row, opponent)
            }
            // Add retaliation moves to moves list
            val finalMoves = newMoves + retaliationMoves

            // You could also update a "triggeredMines" list in your state for board drawing

            // Now continue with this new move list
            val updated = current.copy(
                moves = finalMoves,
                currentTurn = opponent,
                placedMines = newPlacedMines,
                triggeredMines = newTriggeredMines
            )

            // Also add to Firestore update!
            mineRetaliationUpdates["gameState.battleships.moves"] = updated.moves
            mineRetaliationUpdates["gameState.battleships.currentTurn"] = updated.currentTurn
            mineRetaliationUpdates["gameState.battleships.placedMines"] =
                updated.placedMines.mapValues { it.value.map { c -> mapOf("row" to c.row, "col" to c.col) } }

            // Win check logic as in your code:
            val p1Ships = updated.ships[updated.player1Id] ?: emptyList()
            val p2Ships = updated.player2Id?.let { updated.ships[it] } ?: emptyList()
            val moves = updated.moves

            val isP1Sunk = isAllShipsSunk(p1Ships, moves, updated.player1Id)
            val isP2Sunk = isAllShipsSunk(p2Ships, moves, updated.player2Id ?: "")

            val winnerUid: String? = when {
                p1Ships.isNotEmpty() && isP1Sunk -> updated.player2Id
                p2Ships.isNotEmpty() && isP2Sunk -> updated.player1Id
                else -> null
            }
            if (!winnerUid.isNullOrEmpty()) {
                mineRetaliationUpdates["gameState.battleships.gameResult"] = winnerUid
            }

            room.update(mineRetaliationUpdates).await()
            return
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
