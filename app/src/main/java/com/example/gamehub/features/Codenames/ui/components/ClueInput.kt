package com.example.gamehub.features.codenames.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gamehub.ui.SpriteMenuButton
import androidx.compose.material3.Text
import com.example.gamehub.ui.theme.ArcadeClassic

@Composable
fun ClueInput(
    clueText: String,
    onClueTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = clueText,
            onValueChange = onClueTextChange,
            label = { Text("Enter clue and number (e.g., 'APPLE 3')", fontFamily = ArcadeClassic) },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled
        )
        Spacer(modifier = Modifier.height(4.dp))
        SpriteMenuButton(
            text = "Submit Clue",
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth()
        )
    }
} 