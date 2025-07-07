package com.example.gamehub.features.spaceinvaders.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gamehub.features.spaceinvaders.ui.components.GameButton
import com.example.gamehub.features.spaceinvaders.ui.components.GameText
import com.example.gamehub.features.spaceinvaders.ui.theme.SpaceInvadersTheme

@Composable
fun SpaceInvadersGameOverScreen(
    score: Int,
    onRestart: () -> Unit,
    onExit: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceInvadersTheme.backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            GameText(
                text = "Game Over",
                fontSize = SpaceInvadersTheme.FontSizes.large
            )
            GameText(
                text = "Score: $score",
                fontSize = SpaceInvadersTheme.FontSizes.medium,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
            )

            GameButton(
                text = "Restart",
                onClick = onRestart,
                fontSize = SpaceInvadersTheme.FontSizes.normal
            )

            GameButton(
                text = "Exit Game",
                onClick = onExit,
                fontSize = SpaceInvadersTheme.FontSizes.normal
            )
        }
    }
}
