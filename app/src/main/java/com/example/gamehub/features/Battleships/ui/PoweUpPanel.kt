package com.example.gamehub.features.battleships.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gamehub.lobby.model.PowerUp

/**
 * A panel showing available Power-Ups for the current player.
 * @param available list of PowerUps the player can use
 * @param onUse callback when a PowerUp is selected
 */
@Composable
fun PowerUpPanel(
    available: List<PowerUp>,
    onUse: (PowerUp) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(
            text = "Power-Ups",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (available.isEmpty()) {
            Text("No power-ups available.")
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                available.forEach { pu ->
                    Button(
                        onClick = { onUse(pu) }
                    ) {
                        Text(text = pu.name)
                    }
                }
            }
        }
    }
}
