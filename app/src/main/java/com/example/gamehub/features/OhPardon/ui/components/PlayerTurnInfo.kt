package com.example.gamehub.features.ohpardon.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gamehub.features.ohpardon.models.Player

@Composable
fun PlayerTurnInfo(
    currentTurnPlayer: Player?,
    currentDiceRoll: Int?,
    pixelFont: FontFamily,
    modifier: Modifier = Modifier
) {
    if (currentTurnPlayer == null) return

    Column(modifier = modifier) {
        Text(
            text = "It's ${currentTurnPlayer.name}'s turn!",
            style = TextStyle(
                fontFamily = pixelFont,
                fontSize = 20.sp,
                color = Color.White
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        currentDiceRoll?.let {
            Text(
                text = "${currentTurnPlayer.name} rolled a $it!",
                style = TextStyle(
                    fontFamily = pixelFont,
                    fontSize = 16.sp,
                    color = Color.White
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

