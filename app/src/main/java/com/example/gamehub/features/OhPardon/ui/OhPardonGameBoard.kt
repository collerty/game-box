package com.example.gamehub.features.ohpardon.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.example.gamehub.features.ohpardon.models.Player
import com.example.gamehub.features.ohpardon.ui.components.PawnView

enum class CellType {
    EMPTY, PATH, HOME, GOAL, ENTRY
}

data class BoardCell(
    val x: Int,
    val y: Int,
    val type: CellType,
    val pawn: PawnForUI? = null,
    val color: Color? = null
)

data class PawnForUI(val color: Color, val id: Int)

/**
 * Main component for rendering the Oh Pardon game board
 */
@Composable
fun GameBoard(
    board: List<List<BoardCell>>,
    onPawnClick: (Int) -> Unit,
    currentPlayer: Player?,
    selectedPawnId: Int? = null
) {
    Column {
        board.forEach { row ->
            Row {
                row.forEach { cell ->
                    BoardCellView(
                        cell = cell,
                        currentPlayer = currentPlayer,
                        onPawnClick = onPawnClick,
                        isSelected = cell.pawn?.id == selectedPawnId && cell.pawn?.color == currentPlayer?.color                    )
                }
            }
        }
    }
}

/**
 * Renders a single cell on the game board
 */
@Composable
fun BoardCellView(
    cell: BoardCell,
    currentPlayer: Player?,
    onPawnClick: (Int) -> Unit,
    isSelected: Boolean = false
) {
    val isMyPawn = cell.pawn?.color == currentPlayer?.color
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val cellSize = screenWidth / 12

    val backgroundColor = when (cell.type) {
        CellType.EMPTY -> Color.LightGray
        CellType.PATH -> cell.color ?: Color.White
        CellType.HOME -> cell.color ?: Color.Cyan
        CellType.GOAL -> cell.color ?: Color.Yellow
        CellType.ENTRY -> cell.color ?: Color.Black
    }

    Box(
        modifier = Modifier
            .size(cellSize)
            .border(1.dp, Color.Black)
            .background(backgroundColor)
            .clickable(enabled = isMyPawn) {
                cell.pawn?.let { onPawnClick(it.id) }
            },
        contentAlignment = Alignment.Center
    ) {
        cell.pawn?.let { pawn ->
            PawnView(
                pawn = pawn,
                cellSize = cellSize,
                isSelected = isSelected
            )
        }
    }
}
