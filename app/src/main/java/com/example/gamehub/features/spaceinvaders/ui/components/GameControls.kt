package com.example.gamehub.features.spaceinvaders.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gamehub.features.spaceinvaders.ui.theme.SpaceInvadersTheme

@Composable
fun GameControls(
    onMoveLeft: (Boolean) -> Unit,
    onMoveRight: (Boolean) -> Unit,
    onShoot: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // Left Button
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(SpaceInvadersTheme.greenTextColor.copy(alpha = 0.7f), shape = CircleShape)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            onMoveLeft(true)
                            tryAwaitRelease()
                            onMoveLeft(false)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text("←", color = Color.Black, fontSize = 24.sp)
        }

        // Right Button
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(SpaceInvadersTheme.greenTextColor.copy(alpha = 0.7f), shape = CircleShape)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            onMoveRight(true)
                            tryAwaitRelease()
                            onMoveRight(false)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text("→", color = Color.Black, fontSize = 24.sp)
        }
    }
}

@Composable
fun ShootButton(
    onShoot: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(80.dp)
            .background(Color.Red.copy(alpha = 0.7f), shape = CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onShoot() })
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Shoot",
            color = Color.Black,
            fontFamily = SpaceInvadersTheme.gameBoxFont,
            fontSize = SpaceInvadersTheme.FontSizes.normal
        )
    }
}

