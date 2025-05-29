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

    // Lock orientation to landscape
    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        engine.player = engine.player.copy(y = screenHeightPx - 100f) // 100f = player height
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Game Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                color = Color.Green,
                topLeft = Offset(engine.player.x, engine.player.y),
                size = Size(100f, 30f)
            )
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
        }
    }
}