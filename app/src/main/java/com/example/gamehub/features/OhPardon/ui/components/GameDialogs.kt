package com.example.gamehub.features.ohpardon.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState

@Composable
fun GameDialogs(
    showVictoryDialog: MutableState<Boolean>,
    showExitDialog: MutableState<Boolean>,
    winnerName: String,
    isHost: Boolean,
    onVictoryConfirm: () -> Unit,
    onVictoryDismiss: () -> Unit,
    onExitConfirm: () -> Unit,
    onExitDismiss: () -> Unit
) {
    // Victory Dialog
    if (showVictoryDialog.value) {
        AlertDialog(
            onDismissRequest = { /* Prevent dismissal on tap outside */ },
            title = { Text(text = "🎉 Game Over") },
            text = { Text(text = "$winnerName has won the game!") },
            confirmButton = {
                TextButton(onClick = {
                    showVictoryDialog.value = false
                    onVictoryConfirm()
                }) {
                    Text(if (isHost) "Close Room" else "Exit")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showVictoryDialog.value = false
                    onVictoryDismiss()
                }) {
                    Text("Stay")
                }
            }
        )
    }

    // Exit Dialog
    if (showExitDialog.value) {
        AlertDialog(
            onDismissRequest = { showExitDialog.value = false },
            title = { Text(text = "Exit Game") },
            text = { Text(text = "Are you sure you want to exit the game?") },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog.value = false
                    onExitConfirm()
                }) {
                    Text(if (isHost) "Exit & Close Room" else "Exit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog.value = false }) {
                    Text("Stay")
                }
            }
        )
    }
}
