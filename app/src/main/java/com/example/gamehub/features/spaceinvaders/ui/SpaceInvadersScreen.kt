package com.example.gamehub.features.spaceinvaders.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gamehub.features.spaceinvaders.classes.SpaceInvadersViewModel
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gamehub.features.spaceinvaders.classes.EnemyType


@Composable
fun SpaceInvadersScreen(viewModel: SpaceInvadersViewModel = viewModel()) {
    val engine = viewModel.gameEngine
    val tick by viewModel.tick
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val density = LocalDensity.current
    val screenWidthPx = with(density) { screenWidthDp.dp.toPx() }
    val screenHeightDp = configuration.screenHeightDp
    val screenHeightPx = with(density) { screenHeightDp.dp.toPx() }
    val context = LocalContext.current
    val activity = context as? Activity


    // Set screen width in engine
    LaunchedEffect(screenWidthPx) {
        engine.screenWidthPx = screenWidthPx
    }

    // Set screen height in engine
    LaunchedEffect(screenHeightPx) {
        engine.screenHeightPx = screenHeightPx
        engine.playerController.setPlayer(
            engine.player.copy(y = screenHeightPx - 100f)
        )
    }


    // Lock orientation to landscape
    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }



    Box(modifier = Modifier.fillMaxSize()) {
        // Game Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val frame = tick // Do not remove

            // Draw player
            drawRect(
                color = Color.Green,
                topLeft = Offset(engine.player.x, engine.player.y),
                size = Size(100f, 30f)
            )

            // Draw bullets
            engine.playerController.playerBullets.forEach { bullet ->
                if (bullet.isActive) {
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(bullet.x, bullet.y),
                        size = Size(10f, 20f) // width, height of bullet
                    )
                }
            }


            // Draw enemies
            engine.enemyController.enemies.forEach { row ->
                row.forEach { enemy ->
                    if (enemy.isAlive) {
                        val color = when (enemy.type) {
                            EnemyType.SHOOTER -> Color.Red
                            EnemyType.MIDDLE -> Color.Yellow
                            EnemyType.BOTTOM -> Color.Blue
                        }
                        drawRect(
                            color = color,
                            topLeft = Offset(enemy.x, enemy.y),
                            size = Size(60f, 40f)
                        )
                    }
                }
            }
        }


        // Overlay control buttons
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // Left Button
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color.Gray, shape = CircleShape)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                engine.isMovingLeft = true
                                tryAwaitRelease()
                                engine.isMovingLeft = false
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("←", color = Color.White, fontSize = 24.sp)
            }

            // Right Button
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color.Gray, shape = CircleShape)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                engine.isMovingRight = true
                                tryAwaitRelease()
                                engine.isMovingRight = false
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("→", color = Color.White, fontSize = 24.sp)
            }

            // Shoot Button
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color.Red, shape = CircleShape)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                engine.playerController.shootBullet()
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("Shoot", color = Color.White, fontSize = 18.sp)
            }

        }
    }
}