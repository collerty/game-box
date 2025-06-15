package com.example.gamehub.features.battleships.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Modifier.Companion.any
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gamehub.R
import com.example.gamehub.features.battleships.model.Cell

// You'll also need AttackResult enum (as in your BoardGrid)

@Composable
fun BattleshipMap(
    gridSize: Int = 10,
    cellSize: Dp = 32.dp,
    ships: List<Ship> = emptyList(),

    mineCells: List<Cell> = emptyList(),            // Cells with (armed) mines
    triggeredMines: List<Cell> = emptyList(),       // Cells where a mine exploded
    attacks: Map<Cell, AttackResult> = emptyMap(),  // Usual hit/miss overlays

    @DrawableRes waterSprite: Int = R.drawable.ocean_spritesheet,
    waterFps: Int = 16,
    onCellClick: ((row: Int, col: Int) -> Unit)? = null,
    highlightShip: ((row: Int, col: Int) -> Boolean)? = null,
) {
    // 1) Compute a single shared frame [0..15] at waterFps
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

    // --- Fixed size for the whole grid ---
    Box(
        modifier = Modifier
            .size(cellSize * gridSize)
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize() // exactly the size of the box
        ) {
            for (row in 0 until gridSize) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0 until gridSize) {
                        val isShipCell = ships.any { ship ->
                            if (ship.orientation == Orientation.Horizontal) {
                                row == ship.startRow &&
                                        col in ship.startCol until (ship.startCol + ship.size)
                            } else {
                                col == ship.startCol &&
                                        row in ship.startRow until (ship.startRow + ship.size)
                            }
                        }

                        val cell = Cell(row, col)

                        Box(
                            modifier = Modifier
                                .size(cellSize)
                                .clickable { onCellClick?.invoke(row, col) }
                        ) {
                            if (isShipCell) {
                                BattleshipCell(size = cellSize, isShip = true)
                            } else {
                                AnimatedWaterCell(
                                    frame        = frame,
                                    spriteRes    = waterSprite,
                                    framesPerRow = 8,
                                    totalFrames  = totalFrames,
                                    size         = cellSize
                                )
                            }

                            // --- Ship placement highlight ---
                            if (highlightShip?.invoke(row, col) == true) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(Color.White.copy(alpha = 0.1f))
                                )
                            }

                            // --- MINE PNG overlays ---

                            // Draw mine PNG if present and not triggered
                            if (cell in mineCells && cell !in triggeredMines) {
                                Image(
                                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.mine_big),
                                    contentDescription = "Mine",
                                    modifier = Modifier.size(cellSize * 2f)
                                )
                            }

                            // Draw mine PNG if triggered (you can use a different drawable for explosion if you want)
                            if (cell in triggeredMines) {
                                Image(
                                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.mine_big), // Or explosion
                                    contentDescription = "Triggered Mine",
                                    modifier = Modifier.size(cellSize * 2f)
                                )
                            }
                            // If no triggered mine, but it's a hit, draw hit circle (do NOT cover a triggered mine)
                            else if (attacks[cell] == AttackResult.Hit && cell !in triggeredMines) {
                                Image(
                                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.hit_circle),
                                    contentDescription = null,
                                    modifier = Modifier.size(cellSize * 0.6f)
                                )
                            }
                            // If miss, draw miss circle
                            else if (attacks[cell] == AttackResult.Miss && cell !in triggeredMines) {
                                Image(
                                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.miss_circle),
                                    contentDescription = null,
                                    modifier = Modifier.size(cellSize * 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Draw ship outlines (one black rectangle per ship, square corners) ---
        ships.forEach { ship ->
            val top = cellSize * ship.startRow
            val left = cellSize * ship.startCol
            val width = if (ship.orientation == Orientation.Horizontal) cellSize * ship.size else cellSize
            val height = if (ship.orientation == Orientation.Horizontal) cellSize else cellSize * ship.size
            Box(
                Modifier
                    .absoluteOffset(x = left, y = top)
                    .size(width, height)
                    .border(2.dp, Color.Black, RectangleShape)
            )
        }

        // --- Grid lines ---
        Box(modifier = Modifier.matchParentSize()) {
            for (i in 1 until gridSize) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .offset(x = cellSize * i)
                        .background(Color.Gray)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .offset(y = cellSize * i)
                        .background(Color.Gray)
                )
            }
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
