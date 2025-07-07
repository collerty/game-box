package com.example.gamehub.features.spaceinvaders.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gamehub.features.spaceinvaders.classes.PlayerScore
import com.example.gamehub.features.spaceinvaders.ui.theme.SpaceInvadersTheme

@Composable
fun HighScoresList(
    highScores: List<PlayerScore>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            GameText(
                text = "High Scores",
                fontSize = SpaceInvadersTheme.FontSizes.extraLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(highScores) { score ->
            GameText(
                text = "${score.player}: ${score.score}",
                fontSize = SpaceInvadersTheme.FontSizes.small,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

