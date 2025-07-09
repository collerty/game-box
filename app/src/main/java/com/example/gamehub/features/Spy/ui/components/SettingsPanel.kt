package com.example.gamehub.features.spy.ui.components

import com.example.gamehub.ui.SpriteIconButton
import com.example.gamehub.features.spy.service.SpyGameService.SettingsSummary
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color

const val MIN_PLAYERS = 3
const val MAX_PLAYERS = 10
const val MIN_SPIES = 1
const val MIN_TIMER = 1
const val MAX_TIMER = 30

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
        Text(text = "Players: ${summary.players}", fontSize = 24.sp, color = Color(0xFFEEEEEE))
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            SpriteIconButton(text = "-", onClick = { if (summary.players > MIN_PLAYERS) onPlayersChange(summary.players - 1) })
            Spacer(modifier = Modifier.width(8.dp))
            SpriteIconButton(text = "+", onClick = { if (summary.players < MAX_PLAYERS) onPlayersChange(summary.players + 1) })
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Spies: ${summary.spies}", fontSize = 24.sp, color = Color(0xFFEEEEEE))
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            SpriteIconButton(text = "-", onClick = { if (summary.spies > MIN_SPIES) onSpiesChange(summary.spies - 1) })
            Spacer(modifier = Modifier.width(8.dp))
            SpriteIconButton(text = "+", onClick = { if (summary.spies < summary.players - 1) onSpiesChange(summary.spies + 1) })
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Timer: ${summary.timer} min", fontSize = 24.sp, color = Color(0xFFEEEEEE))
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            SpriteIconButton(text = "-", onClick = { if (summary.timer > MIN_TIMER) onTimerChange(summary.timer - 1) })
            Spacer(modifier = Modifier.width(8.dp))
            SpriteIconButton(text = "+", onClick = { if (summary.timer < MAX_TIMER) onTimerChange(summary.timer + 1) })
        }
    }
} 