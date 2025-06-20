package com.example.gamehub.lobby

import com.example.gamehub.features.battleships.model.Cell
import com.example.gamehub.lobby.codec.BattleshipsCodec
import com.example.gamehub.lobby.model.GameSession
import com.example.gamehub.lobby.model.Move
import com.example.gamehub.lobby.model.coveredCells
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
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
            val bs =
                (gs["battleships"] as? Map<*, *>)?.entries?.associate { it.key as String to it.value }
                    ?: return@addSnapshotListener
            trySend(codec.decode(bs))
        }
        awaitClose { registration.remove() }
    }.stateIn(
        scope = CoroutineScope(Dispatchers.Main),
        started = SharingStarted.Eagerly,
        initialValue = GameSession.empty(roomCode)
    )

    /**
     * Fire one shot at (x,y), update Firestore, and award energy.
     */
    suspend fun submitMove(x: Int, y: Int, playerId: String) {
        val snap = room.get().await()
        val gs = snap.get("gameState") as? Map<*, *> ?: error("Missing gameState")
        val bs =
            (gs["battleships"] as? Map<*, *>)?.entries?.associate { it.key as String to it.value }
                ?: error("Missing battleships")

        val current = codec.decode(bs)

        // Don't allow moves after game is already won
        if (!current.gameResult.isNullOrEmpty()) return

        // Can't play if no opponent!
        val opponent = if (playerId == current.player1Id) {
            current.player2Id ?: return
        } else {
            current.player1Id
        }

        // Already defined above: opponent, playerId, etc.
        val moveCell = Cell(y, x)
        val defenderMines = current.placedMines[opponent] ?: emptyList()
        val hitMine = defenderMines.contains(moveCell)

        val newPlacedMines = current.placedMines.toMutableMap()
        val newMoves = current.moves + Move(x, y, playerId)

        // --- Calculate destroyed ships before ---
        val myShipsBefore = current.ships[playerId] ?: emptyList()
        val oppShipsBefore = current.ships[opponent] ?: emptyList()
        val myMovesBefore = current.moves.filter { it.playerId == playerId }
        val oppMovesBefore = current.moves.filter { it.playerId == opponent }
        val myDestroyedShipsBefore = myShipsBefore.count { ship ->
            ship.coveredCells().all { cell -> oppMovesBefore.any { it.x == cell.col && it.y == cell.row } }
        }
        val oppDestroyedShipsBefore = oppShipsBefore.count { ship ->
            ship.coveredCells().all { cell -> myMovesBefore.any { it.x == cell.col && it.y == cell.row } }
        }

        if (hitMine) {
            // 1. Remove mine that was hit
            val updatedOpponentMines = defenderMines.filter { it != moveCell }
            newPlacedMines[opponent] = updatedOpponentMines
            val prevTriggered = current.triggeredMines[opponent] ?: emptyList()
            val newTriggered = prevTriggered + moveCell
            val newTriggeredMines = current.triggeredMines.toMutableMap()
            newTriggeredMines[opponent] = newTriggered

            // 2. Retaliate: pick 3 random valid (not yet attacked) cells on attacker's board
            val attackedCells = newMoves.filter { it.playerId == opponent }.map { Cell(it.y, it.x) }.toSet()
            val allCells = (0 until 10).flatMap { row -> (0 until 10).map { col -> Cell(row, col) } }
            val unhitCells = allCells.filter { it !in attackedCells }
            val retaliationTargets = unhitCells.shuffled().take(3)
            val retaliationMoves = retaliationTargets.map { cell -> Move(cell.col, cell.row, opponent) }

            // 3. Add retaliation moves to moves list
            val finalMoves = newMoves + retaliationMoves

            // 4. Calculate destroyed ships before and after retaliation
            val myShipsDestroyedBefore = myShipsBefore.count { ship ->
                ship.coveredCells().all { cell -> oppMovesBefore.any { it.x == cell.col && it.y == cell.row } }
            }
            val oppShipsDestroyedBefore = oppShipsBefore.count { ship ->
                ship.coveredCells().all { cell -> myMovesBefore.any { it.x == cell.col && it.y == cell.row } }
            }

            val myMovesAfter = finalMoves.filter { it.playerId == playerId }
            val oppMovesAfter = finalMoves.filter { it.playerId == opponent }
            val myShipsDestroyedAfter = myShipsBefore.count { ship ->
                ship.coveredCells().all { cell -> oppMovesAfter.any { it.x == cell.col && it.y == cell.row } }
            }
            val oppShipsDestroyedAfter = oppShipsBefore.count { ship ->
                ship.coveredCells().all { cell -> myMovesAfter.any { it.x == cell.col && it.y == cell.row } }
            }

            val newlyMyDestroyed = myShipsDestroyedAfter - myShipsDestroyedBefore
            val newlyOppDestroyed = oppShipsDestroyedAfter - oppShipsDestroyedBefore

            // 5. Energy update logic (attacker = mine owner/opponent, defender = you)
            val attackerGain = newlyOppDestroyed // mine owner gains 1 per attacker ship sunk
            val defenderGain = newlyMyDestroyed * 2 // you gain 2 per own ship sunk

            // 6. Firestore update map
            val mineRetaliationUpdates = mutableMapOf<String, Any>(
                "gameState.battleships.moves" to finalMoves,
                "gameState.battleships.currentTurn" to opponent,
                "gameState.battleships.placedMines" to newPlacedMines.mapValues { it.value.map { c -> mapOf("row" to c.row, "col" to c.col) } },
                "gameState.battleships.triggeredMines" to newTriggeredMines.mapValues { it.value.map { c -> mapOf("row" to c.row, "col" to c.col) } }
            )

            if (attackerGain > 0) {
                mineRetaliationUpdates["gameState.battleships.energy.$opponent"] = FieldValue.increment(attackerGain.toLong())
            }
            if (defenderGain > 0) {
                mineRetaliationUpdates["gameState.battleships.energy.$playerId"] = FieldValue.increment(defenderGain.toLong())
            }

            // 7. Win check logic
            val p1Ships = current.ships[current.player1Id] ?: emptyList()
            val p2Ships = current.player2Id?.let { current.ships[it] } ?: emptyList()
            fun isAllShipsSunk(
                ships: List<com.example.gamehub.lobby.model.Ship>,
                moves: List<Move>,
                ownerId: String
            ): Boolean {
                return ships.all { ship ->
                    val shipCells = ship.coveredCells()
                    shipCells.all { cell -> moves.any { it.x == cell.col && it.y == cell.row && it.playerId != ownerId } }
                }
            }
            val isP1Sunk = isAllShipsSunk(p1Ships, finalMoves, current.player1Id)
            val isP2Sunk = isAllShipsSunk(p2Ships, finalMoves, current.player2Id ?: "")
            val winnerUid: String? = when {
                p1Ships.isNotEmpty() && isP1Sunk -> current.player2Id
                p2Ships.isNotEmpty() && isP2Sunk -> current.player1Id
                else -> null
            }
            if (!winnerUid.isNullOrEmpty()) {
                mineRetaliationUpdates["gameState.battleships.gameResult"] = winnerUid
            }

            println("DEBUG: Retaliation mine energy: attackerGain=$attackerGain defenderGain=$defenderGain")
            room.update(mineRetaliationUpdates).await()
            return
        }

        // ---- Normal attack logic here! ----
        val updated = current.copy(
            moves = current.moves + Move(x, y, playerId),
            currentTurn = opponent
        )

        // --- Calculate destroyed ships after ---
        val myShipsAfter = updated.ships[playerId] ?: emptyList()
        val oppShipsAfter = updated.ships[opponent] ?: emptyList()
        val myMovesAfter = updated.moves.filter { it.playerId == playerId }
        val oppMovesAfter = updated.moves.filter { it.playerId == opponent }
        val myDestroyedShipsAfter = myShipsAfter.count { ship ->
            ship.coveredCells().all { cell -> oppMovesAfter.any { it.x == cell.col && it.y == cell.row } }
        }
        val oppDestroyedShipsAfter = oppShipsAfter.count { ship ->
            ship.coveredCells().all { cell -> myMovesAfter.any { it.x == cell.col && it.y == cell.row } }
        }
        val newlyMyDestroyed = myDestroyedShipsAfter - myDestroyedShipsBefore
        val newlyOppDestroyed = oppDestroyedShipsAfter - oppDestroyedShipsBefore

        // --- Prepare Firestore update ---
        val updates = mutableMapOf<String, Any>(
            "gameState.battleships.moves" to updated.moves,
            "gameState.battleships.currentTurn" to updated.currentTurn
        )

        // Check for win
        val p1Ships = current.ships[current.player1Id] ?: emptyList()
        val p2Ships = current.player2Id?.let { current.ships[it] } ?: emptyList()
        fun isAllShipsSunk(ships: List<com.example.gamehub.lobby.model.Ship>, moves: List<Move>, ownerId: String): Boolean {
            return ships.all { ship ->
                val shipCells = ship.coveredCells()
                shipCells.all { cell -> moves.any { it.x == cell.col && it.y == cell.row && it.playerId != ownerId } }
            }
        }
        val moves = updated.moves
        val isP1Sunk = isAllShipsSunk(p1Ships, moves, current.player1Id)
        val isP2Sunk = isAllShipsSunk(p2Ships, moves, current.player2Id ?: "")
        val winnerUid: String? = when {
            p1Ships.isNotEmpty() && isP1Sunk -> current.player2Id
            p2Ships.isNotEmpty() && isP2Sunk -> current.player1Id
            else -> null
        }
        if (!winnerUid.isNullOrEmpty()) {
            updates["gameState.battleships.gameResult"] = winnerUid
        }

        // --- Energy logic: attacker gets 1, defender gets 2 per ship sunk ---
        val attackerGain = newlyOppDestroyed      // 1 per enemy ship sunk
        val defenderGain = newlyOppDestroyed*2   // 2 per own ship sunk

        println("DEBUG ENERGY: attackerGain=$attackerGain defenderGain=$defenderGain newlyOppDestroyed=$newlyOppDestroyed newlyMyDestroyed=$newlyMyDestroyed")

        if (attackerGain > 0) {
            updates["gameState.battleships.energy.$playerId"] = FieldValue.increment(attackerGain.toLong())
        }
        if (defenderGain > 0) {
            updates["gameState.battleships.energy.$opponent"] = FieldValue.increment(defenderGain.toLong())
        }

        if (updates.isNotEmpty()) {
            println("Energy update: $updates")
            room.update(updates).await()
        }
    }
}
