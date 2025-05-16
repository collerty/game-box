package com.example.gamehub.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LocalGameScreen(gameId: String) {
    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Single‐Player: ${gameId.replaceFirstChar { it.uppercaseChar() }}",
                style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            Text("⧗ Loading… (insert your game here)",
                style = MaterialTheme.typography.bodyLarge)
        }
    }
}
