package com.example.gamehub.features.spaceinvaders.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SpaceInvadersGameOverScreen(
    score: Int,
    onRestart: () -> Unit,
    onExit: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Game Over", fontSize = 36.sp, color = Color.White)
            Text("Score: $score", fontSize = 24.sp, color = Color.White, modifier = Modifier.padding(16.dp))

            Button(onClick = onRestart, modifier = Modifier.padding(8.dp)) {
                Text("Restart")
            }

            Button(onClick = onExit, modifier = Modifier.padding(8.dp)) {
                Text("Exit Game")
            }
        }
    }
}
