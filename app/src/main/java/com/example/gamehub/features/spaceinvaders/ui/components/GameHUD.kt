package com.example.gamehub.features.spaceinvaders.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gamehub.features.spaceinvaders.ui.theme.SpaceInvadersTheme

@Composable
fun GameHUD(
    lives: Int,
    score: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        GameText(
            text = "Lives: $lives",
            fontSize = SpaceInvadersTheme.FontSizes.extraLarge
        )
        GameText(
            text = "Score: $score",
            fontSize = SpaceInvadersTheme.FontSizes.extraLarge,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

