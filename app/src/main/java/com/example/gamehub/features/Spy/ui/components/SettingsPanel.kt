package com.example.gamehub.features.spy.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gamehub.features.spy.service.SpyGameService.SettingsSummary

@Composable
fun SettingsPanel(
    summary: SettingsSummary?,
    onPlayersChange: (Int) -> Unit,
    onSpiesChange: (Int) -> Unit,
    onTimerChange: (Int) -> Unit
) {
    if (summary == null) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Players: ${summary.players}")
        Row {
            Button(onClick = { if (summary.players > 3) onPlayersChange(summary.players - 1) }) { Text("-") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { if (summary.players < 10) onPlayersChange(summary.players + 1) }) { Text("+") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Spies: ${summary.spies}")
        Row {
            Button(onClick = { if (summary.spies > 1) onSpiesChange(summary.spies - 1) }) { Text("-") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { if (summary.spies < summary.players - 1) onSpiesChange(summary.spies + 1) }) { Text("+") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Timer: ${summary.timer} min")
        Row {
            Button(onClick = { if (summary.timer > 1) onTimerChange(summary.timer - 1) }) { Text("-") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { if (summary.timer < 30) onTimerChange(summary.timer + 1) }) { Text("+") }
        }
    }
} 