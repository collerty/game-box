package com.example.gamehub.features.spaceinvaders.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
    val retroFont = FontFamily(Font(R.font.space_invaders, FontWeight.Normal))
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
                fontSize = 36.sp,
                color = greenTextColor,
                fontFamily = retroFont
            )
            Text(
                "Score: $score",
                fontSize = 20.sp,
                color = greenTextColor,
                fontFamily = retroFont,
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
                Text("Restart", color = greenTextColor, fontFamily = retroFont, fontSize = 18.sp)
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
                Text("Exit Game", color = greenTextColor, fontFamily = retroFont, fontSize = 18.sp)
            }
        }
    }
}
