package com.example.gamehub.features.codenames.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.gamehub.ui.theme.ArcadeClassic

@Composable
fun GameOverDialog(
    winner: String?,
    onReturnToLobby: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (winner != null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = when (winner) {
                        "RED" -> "Red Team Wins!"
                        "BLUE" -> "Blue Team Wins!"
                        else -> "Game Over"
                    },
                    style = MaterialTheme.typography.headlineLarge,
                    color = when (winner) {
                        "RED" -> Color.Red
                        "BLUE" -> Color.Blue
                        else -> Color.White
                    },
                    fontFamily = ArcadeClassic
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = when (winner) {
                        "RED" -> "Red team found all their words!"
                        "BLUE" -> "Blue team found all their words!"
                        else -> "Game ended"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontFamily = ArcadeClassic
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onReturnToLobby,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (winner) {
                            "RED" -> Color.Red
                            "BLUE" -> Color.Blue
                            else -> Color.Gray
                        }
                    )
                ) {
                    Text("Return to Lobby", color = Color.White, fontFamily = ArcadeClassic)
                }
            }
        }
    }
} 