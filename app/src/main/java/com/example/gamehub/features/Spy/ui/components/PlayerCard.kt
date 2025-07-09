package com.example.gamehub.features.spy.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Player ${info.playerNumber}")
        Spacer(modifier = Modifier.height(8.dp))
        if (!info.isRoleRevealed) {
            Button(onClick = onRevealRole) {
                Text("Reveal Role")
            }
        } else {
            Text(text = info.role ?: "")
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onAdvancePlayer) {
                Text("Next Player")
            }
        }
    }
} 