package com.example.gamehub.features.battleships.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity

@Composable
fun DebugCellCentersOverlay(
    boardOffset: Offset,
    cellSizePx: Float,
    gridSize: Int = 10
) {
    val density = LocalDensity.current
    Box(Modifier.fillMaxSize()) {
        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                val centerX = boardOffset.x + col * cellSizePx + cellSizePx / 2f
                val centerY = boardOffset.y + row * cellSizePx + cellSizePx / 2f
                Box(
                    Modifier
                        .absoluteOffset(
                            x = with(density) { centerX.toDp() } - 8.dp,
                            y = with(density) { centerY.toDp() } - 8.dp
                        )
                        .size(16.dp)
                        .background(Color.Red.copy(alpha = 0.6f), shape = CircleShape)
                )
            }
        }
    }
}
