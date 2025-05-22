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
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

// Constants for the game
private const val PLAYER_WIDTH_DP = 40f
private const val PLAYER_HEIGHT_DP = 60f
private const val ACCELEROMETER_SENSITIVITY = 4.0f
private const val GRAVITY = 0.4f
private const val INITIAL_JUMP_VELOCITY = -11.5f
private const val PLATFORM_HEIGHT_DP = 15f
private const val PLATFORM_WIDTH_DP = 70f
private const val SCROLL_THRESHOLD_ON_SCREEN_Y_FACTOR = 0.6f // When player is above 55% from top of *visible screen*, scroll
private const val MAX_PLATFORMS_ON_SCREEN = 15 // Max platforms to manage
private const val INITIAL_PLATFORM_COUNT = 5 // Start with a few platforms


data class PlatformState(
    val id: Int,
    var x: Float, // World X Dp from left
    var y: Float  // World Y Dp from top (smaller Y is higher)
)

@Composable
fun JorisJumpScreen() {
    val context = LocalContext.current

    // Player state (all in world coordinates, except X which is screen-relative for accelerometer)
    var playerXPositionScreenDp by remember { mutableStateOf(0f) } // X on screen
    var playerYPositionWorldDp by remember { mutableStateOf(0f) } // Y in world
    var playerVelocityY by remember { mutableStateOf(0f) }

    var platforms by remember { mutableStateOf<List<PlatformState>>(emptyList()) }
    var gameRunning by remember { mutableStateOf(true) }
    var score by remember { mutableStateOf(0) }
    var nextPlatformId by remember { mutableStateOf(0) } // Reset in performGameReset

    var totalScrollOffsetDp by remember { mutableStateOf(0f) } // How much the world has scrolled up

    // Screen dimensions
    var screenWidthDp by remember { mutableStateOf(0f) }
    var screenHeightDp by remember { mutableStateOf(0f) }

    // Initialization and sensor related
    var playerAndScreenInitialized by remember { mutableStateOf(false) }
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val accelerometer = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    var rawTiltXForDebug by remember { mutableStateOf(0f) }


    fun performGameReset() {
        if (screenWidthDp == 0f || screenHeightDp == 0f) {
            Log.e("JorisJump", "Reset Aborted: Screen dimensions not yet available.")
            return
        }
        Log.d("JorisJump", "Performing game reset. Screen: $screenWidthDp x $screenHeightDp")

        totalScrollOffsetDp = 0f
        playerXPositionScreenDp = (screenWidthDp / 2) - (PLAYER_WIDTH_DP / 2)
        // Initial player Y in world coordinates, slightly above the first platform to land on it
        playerYPositionWorldDp = screenHeightDp - PLAYER_HEIGHT_DP - PLATFORM_HEIGHT_DP - 30f
        playerVelocityY = 0f // Start by falling onto the first platform

        val initialPlatforms = mutableListOf<PlatformState>()
        // First platform for player to land on, relatively low in the initial screen view
        var currentY = screenHeightDp - PLATFORM_HEIGHT_DP - 5f // World Y of first platform's top
        initialPlatforms.add(
            PlatformState(
                id = 0,
                x = (screenWidthDp / 2) - (PLATFORM_WIDTH_DP / 2), // Center first platform
                y = currentY
            )
        )
        nextPlatformId = 1 // Start ID for next generated platforms

        // Generate subsequent initial platforms progressively higher
        for (i in 1 until INITIAL_PLATFORM_COUNT) {
            val nextX = Random.nextFloat() * (screenWidthDp - PLATFORM_WIDTH_DP)
            // Make the gap larger for initial platforms to spread them out more
            currentY -= (PLATFORM_HEIGHT_DP * Random.nextInt(4, 9)).toFloat() + Random.nextFloat() * PLATFORM_HEIGHT_DP * 2.5f
            initialPlatforms.add(PlatformState(id = nextPlatformId++, x = nextX, y = currentY))
        }
        platforms = initialPlatforms
        nextPlatformId = initialPlatforms.size

        score = 0
        gameRunning = true
        Log.d("JorisJump", "Game Reset. PlayerY_world: $playerYPositionWorldDp, Platforms: ${platforms.size}")
    }


    DisposableEffect(accelerometer, playerAndScreenInitialized) {
        if (accelerometer == null) {
            Log.e("JorisJump", "Accelerometer not available!")
            return@DisposableEffect onDispose {}
        }
        if (!playerAndScreenInitialized) return@DisposableEffect onDispose {}

        val sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (!gameRunning) return
                event?.let {
                    if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        val tiltX = it.values[0]
                        rawTiltXForDebug = tiltX
                        playerXPositionScreenDp -= tiltX * ACCELEROMETER_SENSITIVITY

                        if ((playerXPositionScreenDp + PLAYER_WIDTH_DP) < 0) {
                            playerXPositionScreenDp = screenWidthDp
                        } else if (playerXPositionScreenDp > screenWidthDp) {
                            playerXPositionScreenDp = 0f - PLAYER_WIDTH_DP
                        }
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensorManager.unregisterListener(sensorEventListener) }
    }

    LaunchedEffect(gameRunning, playerAndScreenInitialized) {
        if (!gameRunning || !playerAndScreenInitialized) return@LaunchedEffect

        while (gameRunning) {
            // Player Physics
            playerVelocityY += GRAVITY
            playerYPositionWorldDp += playerVelocityY

            // Collision Detection
            val playerBottomWorldDp = playerYPositionWorldDp + PLAYER_HEIGHT_DP
            val playerRightScreenDp = playerXPositionScreenDp + PLAYER_WIDTH_DP

            platforms.forEach { platform ->
                if (playerVelocityY > 0 &&
                    playerBottomWorldDp >= platform.y && playerYPositionWorldDp < platform.y && // Crossed platform top
                    playerRightScreenDp > platform.x && playerXPositionScreenDp < (platform.x + PLATFORM_WIDTH_DP) // X overlap
                ) {
                    playerYPositionWorldDp = platform.y - PLAYER_HEIGHT_DP // Snap to top
                    playerVelocityY = INITIAL_JUMP_VELOCITY
                }
            }

            // Camera Scrolling
            // Player's Y position on the actual screen: playerYPositionWorldDp - totalScrollOffsetDp
            val playerYOnScreen = playerYPositionWorldDp - totalScrollOffsetDp
            val scrollThresholdActualScreenY = screenHeightDp * SCROLL_THRESHOLD_ON_SCREEN_Y_FACTOR

            if (playerYOnScreen < scrollThresholdActualScreenY) {
                val scrollAmount = scrollThresholdActualScreenY - playerYOnScreen
                totalScrollOffsetDp -= scrollAmount // World "moves up", so offset decreases (less to subtract)
                Log.d("JorisJump_Scroll", "Scrolled. New totalScrollOffsetDp: $totalScrollOffsetDp")
            }

            // Platform Management
            val currentPlatformsMutable = platforms.toMutableList()
            currentPlatformsMutable.removeAll { it.y > totalScrollOffsetDp + screenHeightDp + PLATFORM_HEIGHT_DP * 2 } // Remove if well off bottom

            // Top of the visible screen in world coordinates: totalScrollOffsetDp
            // We want to ensure platforms exist up to one screen height *above* this.
            // So, the target "highest Y" for platforms should be totalScrollOffsetDp - screenHeightDp.
            val targetHighestWorldYForPlatforms = totalScrollOffsetDp - screenHeightDp

            var lastGeneratedPlatformY = currentPlatformsMutable.minOfOrNull { it.y } // Get the actual highest platform
                ?: (totalScrollOffsetDp + screenHeightDp / 2) // Default: if no platforms, start generating from mid-screen upward

            var generationAttempts = 0 // Safety break for the while loop

            // Keep generating platforms as long as we need more AND the last one generated is still "below" our target horizon
            while (currentPlatformsMutable.size < MAX_PLATFORMS_ON_SCREEN && lastGeneratedPlatformY > targetHighestWorldYForPlatforms && generationAttempts < MAX_PLATFORMS_ON_SCREEN * 2) {
                generationAttempts++
                val newX = Random.nextFloat() * (screenWidthDp - PLATFORM_WIDTH_DP)
                // Place the new platform a random distance above the 'lastGeneratedPlatformY'
                val newY = lastGeneratedPlatformY - (PLATFORM_HEIGHT_DP * (Random.nextInt(2, 6))).toFloat() - (Random.nextFloat() * PLATFORM_HEIGHT_DP * 2)

                // Ensure platforms don't overlap too much horizontally with the one just below it (simplistic check)
                // This is a very basic attempt to avoid direct stacking, can be improved
                val platformJustBelow = currentPlatformsMutable.firstOrNull() // Assuming list is somewhat sorted or we just check the last added
                var validPosition = true
                if (platformJustBelow != null) {
                    if (kotlin.math.abs(newX - platformJustBelow.x) < PLATFORM_WIDTH_DP / 2 && kotlin.math.abs(newY - platformJustBelow.y) < PLATFORM_HEIGHT_DP * 3) {
                        // Too close, try a different X (or skip this generation attempt)
                        // For simplicity, let's just log and allow it, but this is where you'd add smarter placement
                        // Log.d("JorisJump_Gen", "Potential overlap, newX: $newX, prevX: ${platformJustBelow.x}")
                    }
                }

                if(validPosition) {
                    currentPlatformsMutable.add(0, PlatformState(id = nextPlatformId++, x = newX, y = newY)) // Add to beginning
                    lastGeneratedPlatformY = newY // Update the Y of the last platform we just generated
                    score += 1 // Smaller score increment
                    Log.d("JorisJump_Gen", "WHILE: Generated platform $nextPlatformId at Y(world):$newY. LastGenY: $lastGeneratedPlatformY. Horizon: $targetHighestWorldYForPlatforms. Count: ${currentPlatformsMutable.size}")
                } else {
                    // Could decrement generationAttempts if we skip, to allow more real attempts
                }
            }
            platforms = currentPlatformsMutable.toList()

            // Game Over Check: If player's top (world Y) is below the visible screen bottom (world Y)
            if (playerYPositionWorldDp > totalScrollOffsetDp + screenHeightDp) {
                gameRunning = false
                Log.d("JorisJump", "Game Over - Fell off. Final Score: $score")
            }

            delay(16)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF87CEEB))
    ) {
        if (!playerAndScreenInitialized && this.maxWidth > 0.dp && this.maxHeight > 0.dp) {
            screenWidthDp = this.maxWidth.value
            screenHeightDp = this.maxHeight.value
            performGameReset()
            playerAndScreenInitialized = true
        }

        if (playerAndScreenInitialized) {
            // Draw Platforms (adjust Y by totalScrollOffsetDp for on-screen position)
            platforms.forEach { platform ->
                Box(
                    modifier = Modifier
                        .absoluteOffset(
                            x = platform.x.dp,
                            y = (platform.y - totalScrollOffsetDp).dp
                        )
                        .size(PLATFORM_WIDTH_DP.dp, PLATFORM_HEIGHT_DP.dp)
                        .background(Color.DarkGray)
                )
            }

            // Draw Player (adjust Y by totalScrollOffsetDp for on-screen position)
            Box(
                modifier = Modifier
                    .absoluteOffset(
                        x = playerXPositionScreenDp.dp,
                        y = (playerYPositionWorldDp - totalScrollOffsetDp).dp
                    )
                    .size(PLAYER_WIDTH_DP.dp, PLAYER_HEIGHT_DP.dp)
                    .background(Color.Green)
            ) {
                Text("J", color = Color.Black, modifier = Modifier.align(Alignment.Center))
            }

            // Score Text
            Text(
                "Score: $score",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.Black,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
            )

            // Game Over UI
            if (!gameRunning) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("GAME OVER", style = MaterialTheme.typography.headlineMedium, color = Color.Red)
                    Text("Final Score: $score", style = MaterialTheme.typography.headlineSmall, color = Color.Black)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        Log.d("JorisJump", "Restart button clicked.")
                        performGameReset()
                    }) { Text("Restart Game") }
                }
            }
        }

        // Debug Info Column
        Column(modifier = Modifier.align(Alignment.TopStart).padding(8.dp).background(Color.Black.copy(alpha = 0.3f))) {
            Text("PlayerX(scr): ${"%.1f".format(playerXPositionScreenDp)} | TiltX: ${"%.2f".format(rawTiltXForDebug)}", color = Color.White, fontSize = 10.sp)
            Text("PlayerY(world): ${"%.1f".format(playerYPositionWorldDp)} | PlayerY(scr): ${"%.1f".format(playerYPositionWorldDp - totalScrollOffsetDp)} | Vy: ${"%.1f".format(playerVelocityY)}", color = Color.White, fontSize = 10.sp)
            Text("ScrollOffset: ${"%.1f".format(totalScrollOffsetDp)}", color = Color.White, fontSize = 10.sp)
            Text("Scrn: ${"%.0f".format(screenWidthDp)}x${"%.0f".format(screenHeightDp)}", color = Color.White, fontSize = 10.sp)
            Text(if (gameRunning) "Running" else "Game Over", color = if (gameRunning) Color.Green else Color.Red, fontSize = 10.sp)
            Text("Platforms: ${platforms.size} | NextID: $nextPlatformId", color = Color.White, fontSize = 10.sp)
            if (accelerometer == null) Text("ACCEL N/A!", color = Color.Red, fontSize = 10.sp)
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
                    .offset(y = (-70).dp) // Approx player position on screen
                    .size(PLAYER_WIDTH_DP.dp, PLAYER_HEIGHT_DP.dp)
                    .background(Color.Green)
            ) { Text("J", color = Color.Black, modifier = Modifier.align(Alignment.Center)) }

            // Approx first platform on screen
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-70 + PLAYER_HEIGHT_DP + 5).dp) // Below player
                    .size(PLATFORM_WIDTH_DP.dp, PLATFORM_HEIGHT_DP.dp)
                    .background(Color.DarkGray)
            )
            // Approx second platform higher up on screen
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-150).dp, x = 30.dp)
                    .size(PLATFORM_WIDTH_DP.dp, PLATFORM_HEIGHT_DP.dp)
                    .background(Color.DarkGray)
            )

            Text("Score: 0", style = MaterialTheme.typography.headlineSmall, color = Color.Black,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp))
            Text("Preview (No Dynamic Logic)", modifier = Modifier.align(Alignment.Center).padding(16.dp), color = Color.Black)
        }
    }
}