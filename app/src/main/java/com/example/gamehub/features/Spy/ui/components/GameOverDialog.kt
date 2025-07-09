package com.example.gamehub.features.spy.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun GameOverDialog(onRestart: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Game Over") },
        text = { Text("The game has ended. Would you like to play again?") },
        confirmButton = {
            Button(onClick = onRestart) {
                Text("Restart")
            }
        },
        dismissButton = {}
    )
} 