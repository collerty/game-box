package com.example.gamehub.features.spy.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gamehub.features.spy.service.SpyGameService.PlayerCardInfo

@Composable
fun PlayerCard(
    info: PlayerCardInfo,
    onRevealRole: () -> Unit,
    onAdvancePlayer: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Player ${info.playerNumber}")
            Spacer(modifier = Modifier.height(24.dp))
            if (!info.isRoleRevealed) {
                Button(onClick = onRevealRole) {
                    Text("Tap to reveal role")
                }
            } else {
                Text(text = info.role ?: "", modifier = Modifier.padding(16.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onAdvancePlayer) {
                    Text("Next Player")
                }
            }
        }
    }
} 