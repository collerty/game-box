package com.example.gamehub.features.codenames.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.gamehub.ui.theme.ArcadeClassic

@Composable
fun TimerPanel(
    seconds: Int,
    label: String? = null,
    color: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Text(
        text = (label?.let { "$it: " } ?: "") + "${seconds}s",
        style = MaterialTheme.typography.titleMedium,
        color = color,
        fontFamily = ArcadeClassic,
        modifier = modifier
    )
} 