package com.example.gamehub.features.MemoryMatching.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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

private val BUTTON_TEXT_SIZE = 18.sp

@Composable
fun EndGameOverlay(
    isWin: Boolean,
    loseReason: String?,
    flips: Int,
    timeLeft: Int,
    onRestart: () -> Unit,
    onChangeDifficulty: () -> Unit,
    overlayColor: Color,
    winTitleColor: Color,
    loseTitleColor: Color,
    detailColor: Color,
    gameButtonColors: ButtonColors,
    gameButtonBorder: BorderStroke?,
    gameButtonElevation: ButtonElevation?,
    gameButtonPadding: PaddingValues
) {
    AnimatedVisibility(
        visible = true, // This should be controlled by the parent
        enter = fadeIn(animationSpec = tween(1000)) + scaleIn(animationSpec = tween(1000)),
        exit = fadeOut(animationSpec = tween(500)) + scaleOut(animationSpec = tween(500))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(overlayColor)
                .clickable(enabled = false, onClick = {}),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isWin) {
                    Text("YOU WIN!", fontSize = 60.sp, fontWeight = FontWeight.Bold, color = winTitleColor, textAlign = TextAlign.Center)
                    Text("Flips: $flips", fontSize = 28.sp, color = detailColor)
                    Text("Time Left: ${timeLeft}s", fontSize = 24.sp, color = detailColor)
                } else {
                    Text(loseReason ?: "GAME OVER", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = loseTitleColor, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp))
                    Text("Flips: $flips", fontSize = 26.sp, color = detailColor)
                    if (loseReason == "Time's Up!") {
                        Text("You ran out of time!", fontSize = 22.sp, color = detailColor)
                    } else if (loseReason == "Too many mistakes!") {
                        Text("Exceeded max mistakes!", fontSize = 22.sp, color = detailColor)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onRestart,
                    shape = RectangleShape, colors = gameButtonColors, border = gameButtonBorder, elevation = gameButtonElevation, contentPadding = gameButtonPadding
                ) {
                    Text(if (isWin) "Play Again" else "Try Again", fontSize = BUTTON_TEXT_SIZE)
                }
                Button(
                    onClick = onChangeDifficulty,
                    shape = RectangleShape, colors = gameButtonColors, border = gameButtonBorder, elevation = gameButtonElevation, contentPadding = gameButtonPadding
                ) {
                    Text("Change Difficulty", fontSize = BUTTON_TEXT_SIZE)
                }
            }
        }
    }
}

