package com.example.gamehub.features.jorisjump.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay


// Constants for the game
private const val PLAYER_WIDTH_DP = 40f // Use Float for consistency
private const val PLAYER_HEIGHT_DP = 60f
private const val ACCELEROMETER_SENSITIVITY = 4.0f // You found this works well
private const val GRAVITY = 0.4f // Adjusted gravity slightly
private const val INITIAL_JUMP_VELOCITY = -11f // Adjusted jump velocity
private const val PLATFORM_HEIGHT_DP = 15f
private const val PLATFORM_WIDTH_DP = 70f

data class PlatformState(
    val id: Int,
    var x: Float, // Dp from left
    var y: Float  // Dp from top
)

@Composable
fun JorisJumpScreen() {
    val context = LocalContext.current

    var playerXPositionDp by remember { mutableStateOf(0f) }
    var playerYPositionDp by remember { mutableStateOf(0f) }
    var playerVelocityY by remember { mutableStateOf(0f) }
    var playerInitialized by remember { mutableStateOf(false) }

    var platforms by remember { mutableStateOf<List<PlatformState>>(emptyList()) }
    var gameRunning by remember { mutableStateOf(true) }

    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    val accelerometer = remember {
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    var screenWidthDp by remember { mutableStateOf(0f) }
    var screenHeightDpState by remember { mutableStateOf(0f) } // For game over check
    var rawTiltXForDebug by remember { mutableStateOf(0f) }

    DisposableEffect(accelerometer, screenWidthDp, playerInitialized) {
        if (accelerometer == null) {
            Log.e("JorisJump", "Accelerometer not available!")
            return@DisposableEffect onDispose {}
        }

        val sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (!playerInitialized || screenWidthDp == 0f || !gameRunning) return

                event?.let {
                    if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        val tiltX = it.values[0]
                        rawTiltXForDebug = tiltX

                        // Assuming positive tiltX for left physical tilt makes player move left (decrease X)
                        playerXPositionDp -= tiltX * ACCELEROMETER_SENSITIVITY


                        if ((playerXPositionDp + PLAYER_WIDTH_DP) < 0) {
                            playerXPositionDp = screenWidthDp
                        } else if (playerXPositionDp > screenWidthDp) {
                            playerXPositionDp = 0f - PLAYER_WIDTH_DP
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensorManager.unregisterListener(sensorEventListener) }
    }

    // Game Loop
    LaunchedEffect(gameRunning, playerInitialized) { // Re-evaluate if gameRunning or playerInitialized changes
        if (!gameRunning || !playerInitialized) return@LaunchedEffect // Only run if game is active and player is set

        while (gameRunning) {
            playerVelocityY += GRAVITY
            playerYPositionDp += playerVelocityY

            val playerBottom = playerYPositionDp + PLAYER_HEIGHT_DP
            val playerRight = playerXPositionDp + PLAYER_WIDTH_DP

            var landedOnPlatformThisFrame = false
            platforms.forEach { platform ->
                val platformTop = platform.y
                val platformBottom = platform.y + PLATFORM_HEIGHT_DP
                val platformLeft = platform.x
                val platformRight = platform.x + PLATFORM_WIDTH_DP

                // Collision check: Player is falling AND player's feet are within platform's Y range AND player's X overlaps platform's X
                if (playerVelocityY > 0 && // Must be falling
                    playerBottom >= platformTop && playerYPositionDp < platformTop && // Player's feet crossed the platform top this frame
                    playerRight > platformLeft && playerXPositionDp < platformRight // Player X overlaps
                ) {
                    playerYPositionDp = platformTop - PLAYER_HEIGHT_DP // Snap to top
                    playerVelocityY = INITIAL_JUMP_VELOCITY      // Jump!
                    landedOnPlatformThisFrame = true
                    // In a real game, you might play a sound or add score here
                }
            }

            if (screenHeightDpState > 0f && playerYPositionDp > screenHeightDpState) {
                gameRunning = false // Stop the game
                Log.d("JorisJump", "Game Over - Fell off bottom")
            }

            delay(16) // ~60 FPS
        }
    }


    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF87CEEB))
    ) {
        if (!playerInitialized && this.maxWidth > 0.dp && this.maxHeight > 0.dp) {
            screenWidthDp = this.maxWidth.value
            screenHeightDpState = this.maxHeight.value

            playerXPositionDp = (screenWidthDp / 2) - (PLAYER_WIDTH_DP / 2)
            playerYPositionDp = screenHeightDpState - PLAYER_HEIGHT_DP - PLATFORM_HEIGHT_DP - 5f // Start on a platform
            playerVelocityY = INITIAL_JUMP_VELOCITY // Start with an initial jump

            platforms = listOf(
                PlatformState(id = 0, x = (screenWidthDp / 2) - (PLATFORM_WIDTH_DP / 2), y = screenHeightDpState - PLATFORM_HEIGHT_DP - 5f),
                PlatformState(id = 1, x = (screenWidthDp * 0.2f), y = screenHeightDpState - (PLATFORM_HEIGHT_DP * 6) - 5f), // Platforms higher up
                PlatformState(id = 2, x = (screenWidthDp * 0.7f), y = screenHeightDpState - (PLATFORM_HEIGHT_DP * 12) - 5f),
                PlatformState(id = 3, x = (screenWidthDp * 0.4f), y = screenHeightDpState - (PLATFORM_HEIGHT_DP * 18) - 5f)
            )
            playerInitialized = true
            Log.d("JorisJump", "Player and platforms initialized. Screen: $screenWidthDp x $screenHeightDpState")
        }

        if (playerInitialized) {
            platforms.forEach { platform ->
                Box(
                    modifier = Modifier
                        .absoluteOffset(x = platform.x.dp, y = platform.y.dp)
                        .size(PLATFORM_WIDTH_DP.dp, PLATFORM_HEIGHT_DP.dp)
                        .background(Color.DarkGray)
                )
            }

            Box(
                modifier = Modifier
                    .absoluteOffset(x = playerXPositionDp.dp, y = playerYPositionDp.dp)
                    .size(PLAYER_WIDTH_DP.dp, PLAYER_HEIGHT_DP.dp)
                    .background(Color.Green)
            ) {
                Text("J", color = Color.Black, modifier = Modifier.align(Alignment.Center))
            }

            if (!gameRunning) {
                Text(
                    "GAME OVER",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        Column(modifier = Modifier.align(Alignment.TopStart).padding(8.dp).background(Color.Black.copy(alpha = 0.3f))) {
            Text("PlayerX: ${"%.1f".format(playerXPositionDp)} | TiltX: ${"%.2f".format(rawTiltXForDebug)}", color = Color.White)
            Text("PlayerY: ${"%.1f".format(playerYPositionDp)} | Vy: ${"%.1f".format(playerVelocityY)}", color = Color.White)
            Text("ScreenW: ${"%.0f".format(screenWidthDp)} | ScreenH: ${"%.0f".format(screenHeightDpState)}", color = Color.White)
            Text(if (gameRunning) "Running" else "Game Over", color = if (gameRunning) Color.Green else Color.Red)
            if (accelerometer == null) {
                Text("ACCELEROMETER N/A!", color = Color.Red)
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun JorisJumpScreenPreview() {
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF87CEEB))) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-20).dp)
                    .size(PLAYER_WIDTH_DP.dp, PLAYER_HEIGHT_DP.dp)
                    .background(Color.Green)
            ) {
                Text("J", color = Color.Black, modifier = Modifier.align(Alignment.Center))
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-20-PLATFORM_HEIGHT_DP-5).dp, x= 0.dp) // Platform under J
                    .size(PLATFORM_WIDTH_DP.dp, PLATFORM_HEIGHT_DP.dp)
                    .background(Color.DarkGray)
            )
            Text(
                text = "Preview Mode (No Sensors/Logic)",
                modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
                color = Color.Black
            )
        }
    }
}