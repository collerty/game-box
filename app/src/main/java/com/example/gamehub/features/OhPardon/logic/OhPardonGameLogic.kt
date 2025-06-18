package com.example.gamehub.features.ohpardon.logic

import android.util.Log
import androidx.compose.ui.graphics.Color
import com.example.gamehub.features.ohpardon.models.GameRoom
import com.example.gamehub.features.ohpardon.models.Player

data class MoveResult(
    val isValid: Boolean,
    val message: String = "",
    val updatedPlayers: List<Player> = emptyList(),
    val nextPlayerUid: String = "",
    val isCapture: Boolean = false,
    val isGameOver: Boolean = false,
    val winner: Player? = null
)

class OhPardonGameLogic {

    fun canRollDice(gameRoom: GameRoom, playerUid: String?): Boolean {
        val gameState = gameRoom.gameState
        
        // Check if it's the player's turn
        if (gameState.currentTurnUid != playerUid) {
            Log.w("OhPardonLogic", "Not player's turn to roll dice")
            return false
        }
        
        // Check if dice already rolled this turn
        if (gameState.diceRoll != null) {
            Log.w("OhPardonLogic", "Dice already rolled")
            return false
        }
        
        return true
    }

    fun getNextPlayerUid(game: GameRoom, currentUid: String): String {
        val index = game.players.indexOfFirst { it.uid == currentUid }
        val nextIndex = (index + 1) % game.players.size
        return game.players[nextIndex].uid
    }

    fun calculateMove(currentGame: GameRoom, currentUserUid: String, pawnId: String): MoveResult {
        val gameState = currentGame.gameState

        if (gameState.currentTurnUid != currentUserUid) {
            return MoveResult(isValid = false, message = "Not your turn!")
        }

        val diceRoll = gameState.diceRoll
        if (diceRoll == null) {
            return MoveResult(isValid = false, message = "Roll the dice first!")
        }

        val player = currentGame.players.find { it.uid == currentUserUid }
        val pawn = player?.pawns?.find { it.id == "pawn$pawnId" }

        if (player == null || pawn == null) {
            Log.e("OhPardonLogic", "Player or Pawn not found")
            return MoveResult(isValid = false, message = "Invalid player or pawn")
        }

        val currentPos = pawn.position
        val newPos = getNewPosition(currentPos, diceRoll)
        
        if (newPos == null) {
            return MoveResult(isValid = false, message = "Invalid move!")
        }

        val newAbsPos = relativeToAbsolute(newPos, player.color)

        // Check own pawn blocking
        val isBlockedByOwn = player.pawns.any {
            it.id != pawn.id && it.position == newPos
        }

        // Check if enemy pawn is in their own start cell (relative 0)
        val isBlockedByProtectedEnemy = currentGame.players.any { enemy ->
            if (enemy.uid == currentUserUid) return@any false
            val enemyStartAbs = getBoardOffset(enemy.color)
            val hasProtectedPawn = enemy.pawns.any { it.position == 0 }
            newAbsPos == enemyStartAbs && hasProtectedPawn
        }

        if (isBlockedByOwn || isBlockedByProtectedEnemy) {
            val message = when {
                isBlockedByProtectedEnemy -> "Can't move to opponent's protected start cell."
                isBlockedByOwn -> "Can't move to a tile occupied by your own pawn."
                else -> "Invalid move."
            }
            return MoveResult(isValid = false, message = message)
        }

        // Handle move and possible capture
        var wasCapture = false
        val updatedPlayers = currentGame.players.map { p ->
            if (p.uid == currentUserUid) {
                val updatedPawns = p.pawns.map {
                    if (it.id == pawn.id) it.copy(position = newPos) else it
                }
                p.copy(pawns = updatedPawns)
            } else {
                val updatedPawns = p.pawns.map {
                    val absEnemyPos = relativeToAbsolute(it.position, p.color)
                    if (absEnemyPos == newAbsPos && it.position != 0 && newAbsPos < 40) {
                        Log.d("OhPardonLogic", "Captured pawn ${it.id} from ${p.uid}")
                        wasCapture = true
                        it.copy(position = -1)
                    } else it
                }
                p.copy(pawns = updatedPawns)
            }
        }

        // Check win condition
        val winner = updatedPlayers.find { p ->
            p.uid == currentUserUid && p.pawns.map { it.position }.toSet() == setOf(40, 41, 42, 43)
        }

        val nextPlayerUid = getNextPlayerUid(currentGame, currentUserUid)

        return MoveResult(
            isValid = true,
            updatedPlayers = updatedPlayers,
            nextPlayerUid = nextPlayerUid,
            isCapture = wasCapture,
            isGameOver = winner != null,
            winner = winner
        )
    }

    private fun getNewPosition(currentPos: Int, diceRoll: Int): Int? {
        return when {
            currentPos == -1 && diceRoll == 6 -> 0 // Starting point relative
            currentPos in 0 until 40 -> {
                val stepsTaken = currentPos
                val newSteps = stepsTaken + diceRoll
                if (newSteps > 43) null
                else if (newSteps >= 40) 40 + (newSteps - 40)
                else newSteps
            }
            currentPos in 40..43 -> {
                val newVictoryPos = currentPos + diceRoll
                if (newVictoryPos > 43) null else newVictoryPos
            }
            else -> null
        }
    }

    private fun getBoardOffset(color: Color): Int = when (color) {
        Color.Red -> 0
        Color.Blue -> 10
        Color.Green -> 20
        Color.Yellow -> 30
        else -> 0
    }

    private fun relativeToAbsolute(pos: Int, playerColor: Color): Int {
        return when {
            pos in 0 until 40 -> (getBoardOffset(playerColor) + pos) % 40
            pos >= 40 -> pos // victory
            else -> -1 // home
        }
    }
}
