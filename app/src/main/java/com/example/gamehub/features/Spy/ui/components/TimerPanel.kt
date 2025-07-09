package com.example.gamehub.features.spy.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color

@Composable
fun TimerPanel(timer: Int?, onVibrationEvent: (() -> Unit)? = null) {
    if (timer == null) return
    val minutes = timer / 60
    val seconds = timer % 60
    val currentOnVibrationEvent = rememberUpdatedState(onVibrationEvent)
    LaunchedEffect(timer) {
        if (timer == 0) {
            currentOnVibrationEvent.value?.invoke()
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Time", fontSize = 36.sp, color = Color(0xFFEEEEEE))
            Text(text = String.format("%02d:%02d", minutes, seconds), fontSize = 64.sp, color = Color(0xFFEEEEEE))
        }
    }
} 