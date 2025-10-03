package com.example.gamehub.features.battleships.model

import com.example.gamehub.lobby.model.Move
import com.google.firebase.Timestamp



data class Ship(
    val startRow: Int = 0,
    val startCol: Int = 0,
    val size: Int = 1,
    val orientation: String = "Horizontal"
) {
    fun covers(row: Int, col: Int): Boolean =
        if (orientation == "Horizontal") {
            row == startRow && col in startCol until (startCol + size)
        } else {
            col == startCol && row in startRow until (startRow + size)
        }
}

data class BattleshipsGameState(
    val ready: Map<String, Boolean> = emptyMap(),
    val currentTurnUid: String = "",
    val moves: List<Move> = emptyList(),
    val ships: Map<String, List<Ship>> = emptyMap(), // Map<PlayerId, List<Ship>>
    val energy: Map<String, Int> = emptyMap(), // Map<PlayerId, EnergyAmount>
    val placedMines: Map<String, List<Cell>> = emptyMap(), // Map<PlayerId, List<MineCell>>
    val triggeredMines: Map<String, List<Cell>> = emptyMap(), // Map<PlayerId, List<TriggeredCell>>
    val powerUpMoves: List<Move> = emptyList(),
    val gameResult: String? = null,
    val currentAttack: Map<String, Any>? = null // Stores x, y, playerId, startedAt for animation
)

data class BattleshipsGameRoom(
    val roomCode: String = "",
    val name: String = "",
    val hostUid: String = "",
    val player1Id: String? = null,
    val player2Id: String? = null,
    val status: String = "waiting", // "waiting", "playing", "over", "ended"
    val chosenMap: Int? = null,
    val gameState: BattleshipsGameState = BattleshipsGameState(),
    val createdAt: Timestamp? = null
)