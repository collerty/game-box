package com.example.gamehub.features.ohpardon.ui.mappers

import androidx.compose.ui.graphics.Color
import com.example.gamehub.features.ohpardon.models.ColorConfig
import com.example.gamehub.features.ohpardon.models.Player
import com.example.gamehub.features.ohpardon.ui.BoardCell
import com.example.gamehub.features.ohpardon.ui.CellType
import com.example.gamehub.features.ohpardon.ui.PawnForUI

class BoardMapper {

    private val colorConfigs = mapOf(
        Color.Red to ColorConfig(
            homePositions = listOf(0 to 0, 1 to 0, 0 to 1, 1 to 1),
            goalPath = listOf(1 to 5, 2 to 5, 3 to 5, 4 to 5),
            entryCell = 0 to 4
        ),
        Color.Blue to ColorConfig(
            homePositions = listOf(9 to 0, 10 to 0, 9 to 1, 10 to 1),
            goalPath = listOf(5 to 1, 5 to 2, 5 to 3, 5 to 4),
            entryCell = 6 to 0
        ),
        Color.Green to ColorConfig(
            homePositions = listOf(9 to 10, 10 to 10, 9 to 9, 10 to 9),
            goalPath = listOf(9 to 5, 8 to 5, 7 to 5, 6 to 5),
            entryCell = 10 to 6
        ),
        Color.Yellow to ColorConfig(
            homePositions = listOf(0 to 10, 0 to 9, 1 to 10, 1 to 9),
            goalPath = listOf(5 to 9, 5 to 8, 5 to 7, 5 to 6),
            entryCell = 4 to 10
        )
    )

    private val nonColoredPath = listOf(
        Pair(1, 4), Pair(2, 4), Pair(3, 4), Pair(4, 4),
        Pair(4, 3), Pair(4, 2), Pair(4, 1), Pair(4, 0),
        Pair(5, 0), Pair(6, 1), Pair(6, 2), Pair(6, 3),
        Pair(6, 4), Pair(7, 4), Pair(8, 4), Pair(9, 4),
        Pair(10, 4), Pair(10, 5), Pair(9, 6), Pair(8, 6),
        Pair(7, 6), Pair(6, 6), Pair(6, 7), Pair(6, 8),
        Pair(6, 9), Pair(6, 10), Pair(5, 10), Pair(4, 9),
        Pair(4, 8), Pair(4, 7), Pair(4, 6), Pair(3, 6),
        Pair(2, 6), Pair(1, 6), Pair(0, 6), Pair(0, 5)
    )

    fun createBoard(players: List<Player>): List<List<BoardCell>> {
        val board = MutableList(11) { y ->
            MutableList(11) { x ->
                BoardCell(x, y, type = CellType.EMPTY)
            }
        }

        // Mark home, goal, and entry cells based on colorConfigs
        colorConfigs.forEach { (color, config) ->
            config.homePositions.forEach { (x, y) ->
                board[y][x] = BoardCell(x, y, type = CellType.HOME, color = color)
            }
            config.goalPath.forEach { (x, y) ->
                board[y][x] = BoardCell(x, y, type = CellType.GOAL, color = color)
            }
            val (ex, ey) = config.entryCell
            board[ey][ex] = BoardCell(ex, ey, type = CellType.ENTRY, color = color)
        }

        nonColoredPath.forEach { (x, y) ->
            board[y][x] = BoardCell(x, y, type = CellType.PATH, color = Color.White)
        }

        // Place pawns
        players.forEach { player ->
            player.pawns.forEach { pawn ->
                val pawnId = pawn.id.filter { it.isDigit() }.toIntOrNull() ?: 0
                val (x, y) = getCoordinatesFromPosition(pawn.position, player.color, pawnId)
                board[y][x] = board[y][x].copy(
                    pawn = PawnForUI(color = player.color, id = pawnId)
                )
            }
        }

        return board
    }

    private fun getCoordinatesFromPosition(
        position: Int,
        color: Color,
        pawnIndex: Int = 0
    ): Pair<Int, Int> {
        val offset = when (color) {
            Color.Red -> 0
            Color.Green -> 20
            Color.Blue -> 10
            Color.Yellow -> 30
            else -> 0
        }

        val config = colorConfigs[color]

        // Define shared path
        val fullPath = listOf(
            Pair(0, 4),
            Pair(1, 4), Pair(2, 4), Pair(3, 4), Pair(4, 4),
            Pair(4, 3), Pair(4, 2), Pair(4, 1), Pair(4, 0),
            Pair(5, 0), Pair(6, 0), Pair(6, 1), Pair(6, 2), Pair(6, 3),
            Pair(6, 4), Pair(7, 4), Pair(8, 4), Pair(9, 4),
            Pair(10, 4), Pair(10, 5), Pair(10, 6), Pair(9, 6), Pair(8, 6),
            Pair(7, 6), Pair(6, 6), Pair(6, 7), Pair(6, 8),
            Pair(6, 9), Pair(6, 10), Pair(5, 10), Pair(4, 10), Pair(4, 9),
            Pair(4, 8), Pair(4, 7), Pair(4, 6), Pair(3, 6),
            Pair(2, 6), Pair(1, 6), Pair(0, 6), Pair(0, 5)
        )

        return when {
            position == -1 -> {
                // Home: choose index (e.g., pawnIndex) to spread pawns uniquely
                config?.homePositions?.getOrNull(pawnIndex % 4) ?: (0 to 0)
            }

            position in 0..39 -> {
                val index = (position + offset) % 40
                fullPath.getOrElse(index) { 5 to 5 }
            }

            position in 40..43 -> {
                config?.goalPath?.getOrNull(position - 40) ?: (5 to 5)
            }

            else -> (5 to 5) // fallback to center
        }
    }
}
