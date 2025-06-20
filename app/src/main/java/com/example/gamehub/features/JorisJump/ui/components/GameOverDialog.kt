package com.example.gamehub.features.JorisJump.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gamehub.R
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.shape.RoundedCornerShape

private val pixelFontFamily = FontFamily(
    Font(R.font.arcade_classic, FontWeight.Normal)
)

@Composable
fun GameOverDialog(score: Int, onRestart: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "GAME OVER",
                style = TextStyle(
                    fontFamily = pixelFontFamily,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFF44336),
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.7f),
                        offset = Offset(3f, 4f),
                        blurRadius = 5f
                    )
                )
            )
            Text(
                "Final Score: $score",
                style = TextStyle(
                    fontFamily = pixelFontFamily,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.5f),
                        offset = Offset(2f, 3f),
                        blurRadius = 3f
                    )
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50),
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Restart Game",
                    style = TextStyle(fontFamily = pixelFontFamily, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                )
            }
        }
    }
} 