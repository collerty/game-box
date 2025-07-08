package com.example.gamehub.features.codenames.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.gamehub.ui.theme.ArcadeClassic

@Composable
fun PhaseStatusText(
    currentTurn: String,
    isMasterPhase: Boolean,
    modifier: Modifier = Modifier
) {
    val phaseText = if (isMasterPhase) "Master Phase" else "Guessing Phase"
    val color = if (currentTurn == "RED") Color.Red else Color.Blue
    Text(
        text = "Current Turn: $currentTurn ($phaseText)",
        style = MaterialTheme.typography.titleMedium,
        color = color,
        fontFamily = ArcadeClassic,
        modifier = modifier
    )
} 