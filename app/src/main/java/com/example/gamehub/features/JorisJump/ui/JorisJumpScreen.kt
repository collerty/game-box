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
private const val INITIAL_JUMP_VELOCITY = -11.5f // Fine-tuned jump
private const val PLATFORM_HEIGHT_DP = 15f
private const val PLATFORM_WIDTH_DP = 70f
private const val SCROLL_THRESHOLD_FACTOR = 0.4f // When player is above 40% from top (of visible screen), scroll
private const val MAX_PLATFORMS_ON_SCREEN = 7 // Max platforms on screen at once
private const val INITIAL_PLATFORM_COUNT = 4


data class PlatformState(
    val id: Int,
    var x: Float, // Dp from left
    var y: Float  // Dp from top
)

// Helper to generate initial platforms
private fun generateInitialPlatforms(screenWidth: Float, screenHeight: Float, count: Int): List<PlatformState> {
    val initialPlatforms = mutableListOf<PlatformState>()
    // First platform directly under player
    initialPlatforms.add(
        PlatformState(
            id = 0,
            x = (screenWidth / 2) - (PLATFORM_WIDTH_DP / 2),
            y = screenHeight - PLATFORM_HEIGHT_DP - 5f // A bit below player's starting feet
        )
    )
    for (i in 1 until count) {
        initialPlatforms.add(
            PlatformState(
                id = i,
                x = Random.nextFloat() * (screenWidth - PLATFORM_WIDTH_DP),
                // Spread them out upwards from the first platform
                y = initialPlatforms.last().y - (PLATFORM_HEIGHT_DP * Random.nextInt(4, 8))
            )
        )
    }
    return initialPlatforms
}


@Composable
fun JorisJumpScreen() {
    val context = LocalContext.current

    var playerXPositionDp by remember { mutableStateOf(0f) }
    var playerYPositionDp by remember { mutableStateOf(0f) }
    var playerVelocityY by remember { mutableStateOf(0f) }
    var playerInitialized by remember { mutableStateOf(false) }

    var platforms by remember { mutableStateOf<List<PlatformState>>(emptyList()) }
    var gameRunning by remember { mutableStateOf(true) }
    var score by remember { mutableStateOf(0) }
    var highestYPlayerReachedInWorld by remember { mutableStateOf(Float.MAX_VALUE) } // Tracks the "highest" point player physically reached (smaller Y is higher)
    var nextPlatformId by remember { mutableStateOf(INITIAL_PLATFORM_COUNT) }


    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    val accelerometer = remember {
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    var screenWidthDp by remember { mutableStateOf(0f) }
    var screenHeightDp by remember { mutableStateOf(0f) }
    var rawTiltXForDebug by remember { mutableStateOf(0f) }

    fun performGameReset() {
        if (screenWidthDp == 0f || screenHeightDp == 0f) {
            Log.e("JorisJump", "Cannot reset, screen dimensions not ready.")
            // Attempt to re-initialize playerInitialized to false if it causes issues
            // This might force BoxWithConstraints to re-measure and re-trigger init
            // playerInitialized = false
            return
        }
        Log.d("JorisJump", "Performing game reset. Screen: $screenWidthDp x $screenHeightDp")
        playerXPositionDp = (screenWidthDp / 2) - (PLAYER_WIDTH_DP / 2)
        // Start player slightly above the first platform to ensure they land on it
        playerYPositionDp = screenHeightDp - PLAYER_HEIGHT_DP - PLATFORM_HEIGHT_DP - 25f
        playerVelocityY = 0f // Start falling onto the first platform initially
        platforms = generateInitialPlatforms(screenWidthDp, screenHeightDp, INITIAL_PLATFORM_COUNT)
        highestYPlayerReachedInWorld = playerYPositionDp // Reset to current player Y
        score = 0
        nextPlatformId = platforms.size
        gameRunning = true
        // playerInitialized is already true or will be set true by BoxWithConstraints
        // If reset is called AFTER initial init, playerInitialized should remain true.
        // Forcing a re-init from BoxWithConstraints might be needed if reset state is funky:
        // playerInitialized = false // <-- uncomment this if restart feels off
    }


    DisposableEffect(accelerometer, playerInitialized) { // Removed screenWidthDp as direct key, handled by playerInitialized check
        if (accelerometer == null) {
            Log.e("JorisJump", "Accelerometer not available!")
            return@DisposableEffect onDispose {}
        }
        if (!playerInitialized) return@DisposableEffect onDispose { }


        val sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (!gameRunning || screenWidthDp == 0f) return // Check gameRunning and screenWidth

                event?.let {
                    if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        val tiltX = it.values[0]
                        rawTiltXForDebug = tiltX
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

    LaunchedEffect(gameRunning, playerInitialized) {
        if (!gameRunning || !playerInitialized) return@LaunchedEffect

        while (gameRunning) {
            playerVelocityY += GRAVITY
            playerYPositionDp += playerVelocityY

            if (playerYPositionDp < highestYPlayerReachedInWorld) {
                highestYPlayerReachedInWorld = playerYPositionDp
                // Score is now updated when new platforms are "passed" or generated by scrolling
            }

            val playerBottom = playerYPositionDp + PLAYER_HEIGHT_DP
            val playerRight = playerXPositionDp + PLAYER_WIDTH_DP

            var landedThisFrame = false
            platforms.forEach { platform ->
                if (playerVelocityY > 0 &&
                    playerBottom >= platform.y && playerYPositionDp < platform.y &&
                    playerRight > platform.x && playerXPositionDp < (platform.x + PLATFORM_WIDTH_DP)
                ) {
                    playerYPositionDp = platform.y - PLAYER_HEIGHT_DP
                    playerVelocityY = INITIAL_JUMP_VELOCITY
                    landedThisFrame = true
                }
            }

            // Camera Scrolling
            val scrollThresholdScreenY = screenHeightDp * SCROLL_THRESHOLD_FACTOR
            if (playerYPositionDp < scrollThresholdScreenY) {
                val scrollAmount = scrollThresholdScreenY - playerYPositionDp
                playerYPositionDp += scrollAmount
                highestYPlayerReachedInWorld += scrollAmount // World position also shifts with scroll

                platforms = platforms.map { it.copy(y = it.y + scrollAmount) }
            }

            // Platform Management
            val currentPlatformsMutable = platforms.toMutableList() // Work with a mutable list

            // Remove platforms that are well off-screen (bottom)
            currentPlatformsMutable.removeAll { platform ->
                platform.y > screenHeightDp + (PLATFORM_HEIGHT_DP * 3) // Remove if 3 platform heights below screen
            }

            // Determine the Y coordinate of the "highest" platform currently in our list (smallest Y value).
            // This is the reference point above which new platforms will be generated.
            var highestPlatformYInList = currentPlatformsMutable.minOfOrNull { it.y }
                ?: (playerYPositionDp + screenHeightDp) // Fallback: if no platforms, generate far above current player (effectively off-screen top)

            // If after scrolling, the player is very high and all existing platforms are below them,
            // reset the basis for new platform generation to be relative to the player's current view.
            if (highestPlatformYInList > playerYPositionDp) {
                highestPlatformYInList = playerYPositionDp - (screenHeightDp * 0.2f) // Start generating slightly above player's current view top
            }


            // Generate new platforms if we are below the max count
            while (currentPlatformsMutable.size < MAX_PLATFORMS_ON_SCREEN) {
                val newPlatformX = Random.nextFloat() * (screenWidthDp - PLATFORM_WIDTH_DP)

                // Place new platform a random vertical distance *above* (smaller Y) the current 'highestPlatformYInList'
                val newPlatformY = highestPlatformYInList - (PLATFORM_HEIGHT_DP * Random.nextInt(2, 6)).toFloat() // Random vertical spacing

                // Simple check to avoid direct vertical overlap with the platform used as basis for this new one
                // More sophisticated checks could look at all recent platforms
                if (kotlin.math.abs(newPlatformY - highestPlatformYInList) > PLATFORM_HEIGHT_DP * 1.5f) { // Ensure some minimum gap
                    currentPlatformsMutable.add(0, // Add to the beginning of the list (conceptually "higher")
                        PlatformState(id = nextPlatformId++, x = newPlatformX, y = newPlatformY)
                    )
                    score += 10
                    Log.d("JorisJump", "Generated platform ID ${nextPlatformId - 1} at X:$newPlatformX Y:$newPlatformY. Size: ${currentPlatformsMutable.size}")
                    highestPlatformYInList = newPlatformY // The new platform is now the highest
                } else {
                    // If too close, try generating even higher next time, or break if it seems stuck
                    // For simplicity, we can just break the while loop for this frame if we can't place one easily.
                    // This prevents an infinite loop if random numbers consistently produce overlaps.
                    Log.d("JorisJump", "Skipped platform generation due to potential overlap or too close.")
                    break // Break from the while loop for this frame
                }
            }
            platforms = currentPlatformsMutable // Update the immutable state


            if (playerYPositionDp > screenHeightDp + PLAYER_HEIGHT_DP) { // Game over if completely off bottom
                gameRunning = false
                Log.d("JorisJump", "Game Over - Fell off. Score: $score")
            }

            delay(16)
        }
    }


    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF87CEEB))
    ) {
        if (!playerInitialized && this.maxWidth > 0.dp && this.maxHeight > 0.dp) {
            screenWidthDp = this.maxWidth.value
            screenHeightDp = this.maxHeight.value
            Log.d("JorisJump", "BoxWithConstraints: Initializing with Screen $screenWidthDp x $screenHeightDp")
            performGameReset() // Use the reset function for initial setup
            playerInitialized = true
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

            Text(
                "Score: $score",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.Black,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            )

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
                        // playerInitialized stays true, gameRunning becomes true via performGameReset
                    }) {
                        Text("Restart Game")
                    }
                }
            }
        }

        Column(modifier = Modifier.align(Alignment.TopStart).padding(8.dp).background(Color.Black.copy(alpha = 0.3f))) {
            Text("PlayerX: ${"%.1f".format(playerXPositionDp)} | TiltX: ${"%.2f".format(rawTiltXForDebug)}", color = Color.White)
            Text("PlayerY: ${"%.1f".format(playerYPositionDp)} | Vy: ${"%.1f".format(playerVelocityY)}", color = Color.White)
            Text("Scrn: ${"%.0f".format(screenWidthDp)}x${"%.0f".format(screenHeightDp)} | HighestY: ${"%.0f".format(highestYPlayerReachedInWorld)}", color = Color.White)
            Text(if (gameRunning) "Running" else "Game Over", color = if (gameRunning) Color.Green else Color.Red, fontSize = 10.sp)
            Text("Platforms: ${platforms.size}", color = Color.White, fontSize = 10.sp)
            if (accelerometer == null) Text("ACCEL N/A!", color = Color.Red)
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
            ) { Text("J", color = Color.Black, modifier = Modifier.align(Alignment.Center)) }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-20 - PLATFORM_HEIGHT_DP - 5).dp)
                    .size(PLATFORM_WIDTH_DP.dp, PLATFORM_HEIGHT_DP.dp)
                    .background(Color.DarkGray)
            )
            Text("Score: 0", style = MaterialTheme.typography.headlineSmall, color = Color.Black,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp))
            Text("Preview (No Logic)", modifier = Modifier.align(Alignment.Center).padding(16.dp), color = Color.Black)
        }
    }
}