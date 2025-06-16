package com.example.gamehub.features.spaceinvaders.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gamehub.R

@Composable
fun SpaceInvadersGameOverScreen(
    score: Int,
    onRestart: () -> Unit,
    onExit: () -> Unit,
) {
    val gameBoxFont = FontFamily(Font(R.font.gamebox_font, FontWeight.Bold))
    val greenTextColor = Color(0xFF00FF00)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Game Over",
                fontSize = 48.sp,
                color = greenTextColor,
                fontFamily = gameBoxFont
            )
            Text(
                "Score: $score",
                fontSize = 32.sp,
                color = greenTextColor,
                fontFamily = gameBoxFont,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
            )

            Button(
                onClick = onRestart,
                modifier = Modifier
                    .padding(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    contentColor = Color(0xFF00FF00) // Green
                )
            ) {
                Text("Restart", color = greenTextColor, fontFamily = gameBoxFont, fontSize = 28.sp)
            }

            Button(
                onClick = onExit,
                modifier = Modifier
                    .padding(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    contentColor = Color(0xFF00FF00) // Green
                )
            ) {
                Text("Exit Game", color = greenTextColor, fontFamily = gameBoxFont, fontSize = 28.sp)
            }
        }
    }
}
