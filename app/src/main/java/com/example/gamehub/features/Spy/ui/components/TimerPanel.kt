package com.example.gamehub.features.spy.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TimerPanel(timer: Int?) {
    if (timer == null) return
    val minutes = timer / 60
    val seconds = timer % 60
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = String.format("%02d:%02d", minutes, seconds), fontSize = 32.sp)
    }
} 