package com.example.gamehub.features.codenames.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.gamehub.ui.theme.ArcadeClassic
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme

@Composable
fun ClueHistory(
    clues: List<String>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
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