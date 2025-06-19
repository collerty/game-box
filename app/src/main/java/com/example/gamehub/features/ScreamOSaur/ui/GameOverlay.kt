package com.example.gamehub.features.ScreamOSaur.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gamehub.features.ScreamOSaur.model.GameState

@Composable
fun GameOverlay(
    gameState: GameState,
    score: Int,
    onStart: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (gameState) {
            GameState.READY -> {
                ReadyOverlay(onStart)
            }
            GameState.GAME_OVER -> {
                GameOverOverlay(score, onRestart)
            }
            GameState.PAUSED -> {
                PausedOverlay()
            }
            GameState.PLAYING -> { /* No overlay */ }
        }
    }
}

@Composable
private fun ReadyOverlay(onStart: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("ROAR to make the dinosaur jump!", fontSize = 18.sp, color = Color.Black)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onStart, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
            Text("Start Game", fontSize = 20.sp, color = Color.White)
        }
    }
}

@Composable
private fun GameOverOverlay(score: Int, onRestart: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Game Over!", fontSize = 24.sp, color = Color.Red)
        Text("Score: $score", fontSize = 20.sp, color = Color.Red)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRestart, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
            Text("Play Again", fontSize = 20.sp, color = Color.White)
        }
    }
}

@Composable
private fun PausedOverlay() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Text("PAUSED", fontSize = 30.sp, color = Color.White)
    }
}

