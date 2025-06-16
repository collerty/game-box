package com.example.gamehub.features.battleships.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gamehub.R
import com.example.gamehub.features.battleships.model.Cell

@Composable
fun BattleshipMap(
    gridSize: Int = 10,
    cellSize: Dp = 32.dp,
    ships: List<Ship> = emptyList(),
    destroyedShips: List<Ship> = emptyList(), // <-- NEW
    mineCells: List<Cell> = emptyList(),
    triggeredMines: List<Cell> = emptyList(),
    attacks: Map<Cell, AttackResult> = emptyMap(),
    @DrawableRes waterSprite: Int = R.drawable.ocean_spritesheet,
    waterFps: Int = 16,
    onCellClick: ((row: Int, col: Int) -> Unit)? = null,
    highlightShip: ((row: Int, col: Int) -> Boolean)? = null,
    validCells: Set<Cell> = emptySet()
) {
    // Water animation frame setup (unchanged)
    val totalFrames = 16
    val perFrameMs = 1000 / waterFps
    val loopDurationMs = perFrameMs * totalFrames

    val transition = rememberInfiniteTransition()
    val frame by transition.animateValue(
        initialValue  = 0,
        targetValue   = totalFrames - 1,
        typeConverter = Int.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = loopDurationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = Modifier
            .size(cellSize * gridSize)
    ) {
        for (cell in validCells) {
            val row = cell.row
            val col = cell.col

            val isShipCell = ships.any { ship ->
                if (ship.orientation == Orientation.Horizontal) {
                    row == ship.startRow && col in ship.startCol until (ship.startCol + ship.size)
                } else {
                    col == ship.startCol && row in ship.startRow until (ship.startRow + ship.size)
                }
            }

            val borderTop    = Cell(row - 1, col) !in validCells
            val borderBottom = Cell(row + 1, col) !in validCells
            val borderLeft   = Cell(row, col - 1) !in validCells
            val borderRight  = Cell(row, col + 1) !in validCells

            Box(
                modifier = Modifier
                    .absoluteOffset(x = cellSize * col, y = cellSize * row)
                    .size(cellSize)
                    .clickable { onCellClick?.invoke(row, col) },
                contentAlignment = Alignment.Center   // <-- FIX 1: Center overlays!
            ) {
                when {
                    isShipCell -> {
                        BattleshipCell(size = cellSize, isShip = true)
                    }
                    else -> {
                        AnimatedWaterCell(
                            frame        = frame,
                            spriteRes    = waterSprite,
                            framesPerRow = 8,
                            totalFrames  = totalFrames,
                            size         = cellSize
                        )
                    }
                }

                // Borders for custom shape
                val borderColor = Color.Black
                val borderThickness = 3.dp

                if (borderTop) Box(
                    Modifier
                        .fillMaxWidth()
                        .height(borderThickness)
                        .align(Alignment.TopCenter)
                        .background(borderColor)
                )
                if (borderBottom) Box(
                    Modifier
                        .fillMaxWidth()
                        .height(borderThickness)
                        .align(Alignment.BottomCenter)
                        .background(borderColor)
                )
                if (borderLeft) Box(
                    Modifier
                        .fillMaxHeight()
                        .width(borderThickness)
                        .align(Alignment.CenterStart)
                        .background(borderColor)
                )
                if (borderRight) Box(
                    Modifier
                        .fillMaxHeight()
                        .width(borderThickness)
                        .align(Alignment.CenterEnd)
                        .background(borderColor)
                )

                if (highlightShip?.invoke(row, col) == true) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.White.copy(alpha = 0.1f))
                    )
                }

                if (cell in mineCells && cell !in triggeredMines) {
                    Image(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.mine_big),
                        contentDescription = "Mine",
                        modifier = Modifier.size(cellSize * 2f)
                    )
                }
                if (cell in triggeredMines) {
                    Image(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.mine_big),
                        contentDescription = "Triggered Mine",
                        modifier = Modifier.size(cellSize * 2f)
                    )
                } else if (attacks[cell] == AttackResult.Hit && cell !in triggeredMines) {
                    Image(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.hit_circle),
                        contentDescription = null,
                        modifier = Modifier.size(cellSize * 0.6f)
                    )
                } else if (attacks[cell] == AttackResult.Miss && cell !in triggeredMines) {
                    Image(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.miss_circle),
                        contentDescription = null,
                        modifier = Modifier.size(cellSize * 0.6f)
                    )
                }
            }
        }

        // --- Draw normal ship outlines (black) ---
        ships.forEach { ship ->
            val top = cellSize * ship.startRow
            val left = cellSize * ship.startCol
            val width = if (ship.orientation == Orientation.Horizontal) cellSize * ship.size else cellSize
            val height = if (ship.orientation == Orientation.Horizontal) cellSize else cellSize * ship.size
            Box(
                Modifier
                    .absoluteOffset(x = left, y = top)
                    .size(width, height)
                    .background(Color.Transparent)
                    .border(3.dp, Color.Black, RoundedCornerShape(1.dp))
            )
        }
        // --- Draw destroyed ship outlines (red) ---
        destroyedShips.forEach { ship ->
            val top = cellSize * ship.startRow
            val left = cellSize * ship.startCol
            val width = if (ship.orientation == Orientation.Horizontal) cellSize * ship.size else cellSize
            val height = if (ship.orientation == Orientation.Horizontal) cellSize else cellSize * ship.size
            Box(
                Modifier
                    .absoluteOffset(x = left, y = top)
                    .size(width, height)
                    .background(Color.Transparent)
                    .border(4.dp, Color.Red, RoundedCornerShape(1.dp))
            )
        }
    }
}

@Composable
fun BattleshipCell(
    size: Dp,
    isShip: Boolean
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(2.dp))
            .background(if (isShip) Color(0xFF4B8B1D) else Color(0xFF1A1A1A))
    )
}
