package com.example.gamehub.features.codenames.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gamehub.ui.theme.ArcadeClassic

@Composable
fun TeamPanel(
    teamName: String,
    wordsRemaining: Int,
    clues: List<String>,
    timerSeconds: Int?,
    isCurrentTurn: Boolean,
    modifier: Modifier = Modifier
) {
    val teamColor = when (teamName.uppercase()) {
        "RED" -> Color.Red
        "BLUE" -> Color.Blue
        else -> Color.Gray
    }
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(color = teamColor.copy(alpha = 0.7f), shape = RoundedCornerShape(8.dp))
            .border(1.dp, teamColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = wordsRemaining.toString(),
            style = MaterialTheme.typography.displayLarge,
            color = Color.White,
            fontFamily = ArcadeClassic
        )
        Text(
            "$teamName Team",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            fontFamily = ArcadeClassic
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Log",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontFamily = ArcadeClassic
            )
            if (isCurrentTurn && timerSeconds != null) {
                Text(
                    text = "${timerSeconds}s",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontFamily = ArcadeClassic
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            clues.forEach { clue ->
                Text(
                    text = clue,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    fontFamily = ArcadeClassic
                )
            }
        }
    }
} 