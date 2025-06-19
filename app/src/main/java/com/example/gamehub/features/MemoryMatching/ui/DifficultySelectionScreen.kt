package com.example.gamehub.features.MemoryMatching.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gamehub.features.MemoryMatching.model.GameDifficulty

private val BUTTON_TEXT_SIZE = 18.sp

@Composable
fun DifficultySelectionContent(
    difficulties: List<GameDifficulty>,
    onDifficultySelected: (GameDifficulty) -> Unit,
    backgroundColor: Color,
    textColor: Color,
    buttonColors: ButtonColors,
    buttonBorder: BorderStroke?,
    buttonPadding: PaddingValues,
    buttonElevation: ButtonElevation?
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Select Difficulty",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 28.dp)
            )
            difficulties.forEach { difficulty ->
                Button(
                    onClick = { onDifficultySelected(difficulty) },
                    modifier = Modifier.fillMaxWidth(0.8f),
                    shape = RectangleShape,
                    colors = buttonColors,
                    border = buttonBorder,
                    elevation = buttonElevation,
                    contentPadding = buttonPadding
                ) {
                    Text(
                        text = difficulty.displayName,
                        fontSize = BUTTON_TEXT_SIZE,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

