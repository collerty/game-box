package com.example.gamehub.features.jorisjump.ui

import android.app.Activity // Required for Window operations
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
import androidx.compose.ui.platform.LocalView // Get the current View
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat // For edge-to-edge
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay
import kotlin.random.Random
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.gamehub.R

// Constants for the game
private const val PLAYER_WIDTH_DP = 50f
private const val PLAYER_HEIGHT_DP = 75f
private const val ACCELEROMETER_SENSITIVITY = 4.0f
private const val GRAVITY = 0.4f
private const val INITIAL_JUMP_VELOCITY = -11.5f
private const val PLATFORM_HEIGHT_DP = 15f
private const val PLATFORM_WIDTH_DP = 30f
private const val SCROLL_THRESHOLD_ON_SCREEN_Y_FACTOR = 0.65f
private const val MAX_PLATFORMS_ON_SCREEN = 10
private const val INITIAL_PLATFORM_COUNT = 5
private const val SCORE_POINTS_PER_DP_WORLD_Y = 0.04f
private const val DEBUG_SHOW_HITBOXES = false


data class PlatformState(
    val id: Int,
    var x: Float, // World X Dp from left
    var y: Float  // World Y Dp from top (smaller Y is higher)
)

private fun generateInitialPlatformsList(screenWidth: Float, screenHeight: Float, count: Int, startingId: Int): Pair<List<PlatformState>, Int> {
    val initialPlatforms = mutableListOf<PlatformState>()
    var currentNextId = startingId
    var currentY = screenHeight - PLATFORM_HEIGHT_DP - 5f
    initialPlatforms.add(
        PlatformState(id = currentNextId++, x = (screenWidth / 2) - (PLATFORM_WIDTH_DP / 2), y = currentY)
    )
    for (i in 1 until count) {
        if (initialPlatforms.size >= count) break
        val nextX = Random.nextFloat() * (screenWidth - PLATFORM_WIDTH_DP)
        currentY -= (PLATFORM_HEIGHT_DP * Random.nextInt(4, 9)).toFloat() + Random.nextFloat() * PLATFORM_HEIGHT_DP * 2.5f
        initialPlatforms.add(PlatformState(id = currentNextId++, x = nextX, y = currentY))
    }
    return Pair(initialPlatforms.toList(), currentNextId)
}

@Composable
fun JorisJumpScreen() {
    val context = LocalContext.current
    val view = LocalView.current

    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        val windowInsetsController = window?.let { WindowInsetsControllerCompat(it, view) }
        if (window != null && windowInsetsController != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
            windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onDispose {
            if (window != null && windowInsetsController != null) {
                WindowCompat.setDecorFitsSystemWindows(window, true)
                windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
            }
            Log.d("JorisJump", "Exiting Immersive Mode")
        }
    }

    var playerXPositionScreenDp by remember { mutableStateOf(0f) }
    var playerYPositionWorldDp by remember { mutableStateOf(0f) }
    var playerVelocityY by remember { mutableStateOf(0f) }
    var playerIsFallingOffScreen by remember { mutableStateOf(false) } // New state

    var platforms by remember { mutableStateOf<List<PlatformState>>(emptyList()) }
    var gameRunning by remember { mutableStateOf(true) }
    var score by remember { mutableStateOf(0) }
    var nextPlatformId by remember { mutableStateOf(0) }
    var lastScoredWorldY by remember { mutableStateOf(Float.MAX_VALUE) }

    var totalScrollOffsetDp by remember { mutableStateOf(0f) }
    var screenWidthDp by remember { mutableStateOf(0f) }
    var screenHeightDp by remember { mutableStateOf(0f) }

    var playerAndScreenInitialized by remember { mutableStateOf(false) }
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val accelerometer = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    var rawTiltXForDebug by remember { mutableStateOf(0f) }

    fun performGameReset() {
        if (screenWidthDp == 0f || screenHeightDp == 0f) {
            Log.e("JorisJump", "Reset Aborted: Screen dimensions not yet available.")
            return
        }
        Log.d("JorisJump", "Performing game reset.")
        totalScrollOffsetDp = 0f
        playerXPositionScreenDp = (screenWidthDp / 2) - (PLAYER_WIDTH_DP / 2)
        playerYPositionWorldDp = screenHeightDp - PLAYER_HEIGHT_DP - PLATFORM_HEIGHT_DP - 30f
        playerVelocityY = 0f
        playerIsFallingOffScreen = false // Reset this flag

        val (initialPlatformsList, updatedNextId) = generateInitialPlatformsList(screenWidthDp, screenHeightDp, INITIAL_PLATFORM_COUNT, 0)
        platforms = initialPlatformsList
        nextPlatformId = updatedNextId

        score = 0
        lastScoredWorldY = playerYPositionWorldDp
        gameRunning = true
    }

    DisposableEffect(accelerometer, playerAndScreenInitialized) {
        if (accelerometer == null) { Log.e("JorisJump", "Accelerometer N/A!"); return@DisposableEffect onDispose {} }
        if (!playerAndScreenInitialized) return@DisposableEffect onDispose {}
        val sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (!gameRunning || playerIsFallingOffScreen) return // Stop input if game over or falling off
                event?.let {
                    if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        val tiltX = it.values[0]
                        rawTiltXForDebug = tiltX
                        playerXPositionScreenDp -= tiltX * ACCELEROMETER_SENSITIVITY
                        if ((playerXPositionScreenDp + PLAYER_WIDTH_DP) < 0) playerXPositionScreenDp = screenWidthDp
                        else if (playerXPositionScreenDp > screenWidthDp) playerXPositionScreenDp = 0f - PLAYER_WIDTH_DP
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensorManager.unregisterListener(sensorEventListener) }
    }

    LaunchedEffect(gameRunning, playerAndScreenInitialized) {
        if (!playerAndScreenInitialized) return@LaunchedEffect // Must be initialized
        // Game loop continues as long as gameRunning is true, even if playerIsFallingOffScreen.
        // gameRunning is set to false only after player has fallen far enough.
        while (gameRunning) {
            if (!playerIsFallingOffScreen) {
                // --- REGULAR GAMEPLAY LOGIC ---
                playerVelocityY += GRAVITY
                playerYPositionWorldDp += playerVelocityY

                // Score Update
                val yForNextPoint = lastScoredWorldY - (1.0f / SCORE_POINTS_PER_DP_WORLD_Y)
                if (playerYPositionWorldDp < yForNextPoint) {
                    val pointsEarned = ((lastScoredWorldY - playerYPositionWorldDp) * SCORE_POINTS_PER_DP_WORLD_Y).toInt()
                    if (pointsEarned > 0) {
                        score += pointsEarned
                        lastScoredWorldY -= pointsEarned * (1.0f / SCORE_POINTS_PER_DP_WORLD_Y)
                    }
                }

                // Collision Detection
                val playerBottomWorldDp = playerYPositionWorldDp + PLAYER_HEIGHT_DP
                val playerRightScreenDp = playerXPositionScreenDp + PLAYER_WIDTH_DP
                platforms.forEach { platform ->
                    if (playerVelocityY > 0 &&
                        playerBottomWorldDp >= platform.y && playerYPositionWorldDp < platform.y &&
                        playerRightScreenDp > platform.x && playerXPositionScreenDp < (platform.x + PLATFORM_WIDTH_DP)) {
                        playerYPositionWorldDp = platform.y - PLAYER_HEIGHT_DP
                        playerVelocityY = INITIAL_JUMP_VELOCITY
                    }
                }

                // Camera Scrolling
                val playerYOnScreen = playerYPositionWorldDp - totalScrollOffsetDp
                val scrollThresholdActualScreenY = screenHeightDp * SCROLL_THRESHOLD_ON_SCREEN_Y_FACTOR
                if (playerYOnScreen < scrollThresholdActualScreenY) {
                    val scrollAmount = scrollThresholdActualScreenY - playerYOnScreen
                    totalScrollOffsetDp -= scrollAmount
                }

                // Platform Management
                val currentPlatformsMutable = platforms.toMutableList()
                currentPlatformsMutable.removeAll { it.y > totalScrollOffsetDp + screenHeightDp + PLATFORM_HEIGHT_DP * 2 }
                val generationCeilingWorldY = totalScrollOffsetDp - (screenHeightDp * 1.0f)
                var highestExistingPlatformY = currentPlatformsMutable.minOfOrNull { it.y } ?: (totalScrollOffsetDp + screenHeightDp / 2)
                var generationAttemptsInTick = 0
                while (currentPlatformsMutable.size < MAX_PLATFORMS_ON_SCREEN &&
                    highestExistingPlatformY > generationCeilingWorldY &&
                    generationAttemptsInTick < MAX_PLATFORMS_ON_SCREEN) {
                    generationAttemptsInTick++
                    var newX = Random.nextFloat() * (screenWidthDp - PLATFORM_WIDTH_DP)
                    val minVerticalGapFactor = 5.5f
                    val maxVerticalGapFactor = 9.5f
                    val verticalGap = (PLATFORM_HEIGHT_DP * (minVerticalGapFactor + Random.nextFloat() * (maxVerticalGapFactor - minVerticalGapFactor)))
                    val newY = highestExistingPlatformY - verticalGap
                    val platformImmediatelyBelow = currentPlatformsMutable.firstOrNull()
                    if (platformImmediatelyBelow != null && kotlin.math.abs(newX - platformImmediatelyBelow.x) < PLATFORM_WIDTH_DP * 0.75f) {
                        if (Random.nextFloat() < 0.6) {
                            val shiftDirection = if (platformImmediatelyBelow.x < screenWidthDp / 2) 1 else -1
                            newX = (platformImmediatelyBelow.x + shiftDirection * (PLATFORM_WIDTH_DP * 2 + Random.nextFloat() * screenWidthDp * 0.2f))
                                .coerceIn(0f, screenWidthDp - PLATFORM_WIDTH_DP)
                        }
                    }
                    currentPlatformsMutable.add(0, PlatformState(id = nextPlatformId++, x = newX, y = newY))
                    highestExistingPlatformY = newY
                }
                platforms = currentPlatformsMutable.toList()

                // Check if player fell off visible bottom
                if (playerYPositionWorldDp > totalScrollOffsetDp + screenHeightDp) {
                    if (!playerIsFallingOffScreen) {
                        playerIsFallingOffScreen = true
                        Log.d("JorisJump", "Player fell off visible screen, continuing fall.")
                    }
                }

            } else { // playerIsFallingOffScreen is true
                playerVelocityY += GRAVITY // Continue falling
                playerYPositionWorldDp += playerVelocityY

                // Check if fallen far enough past the bottom
                if (playerYPositionWorldDp > totalScrollOffsetDp + screenHeightDp + PLAYER_HEIGHT_DP * 2.5f) { // Fall 2.5 player heights below screen
                    gameRunning = false // Actual game over
                    Log.d("JorisJump", "Game Over - Player fully off screen. Final Score: $score")
                }
            }
            delay(16)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF87CEEB))
    ) {
        Image(
            painter = painterResource(id = R.drawable.background_doodle), // Your new image
            contentDescription = "Joris Jump Game Background",
            modifier = Modifier.fillMaxSize(), // Make the image fill the entire screen
            contentScale = ContentScale.Crop // Crucial for handling aspect ratio differences
            // Crop will fill the bounds and crop parts of the image
            // that don't fit the screen's aspect ratio,
            // while maintaining the image's own aspect ratio.
            // Other options: ContentScale.FillBounds (stretches),
            // ContentScale.Fit (letterboxes/pillarboxes)
        )
        if (!playerAndScreenInitialized && this.maxWidth > 0.dp && this.maxHeight > 0.dp) {
            screenWidthDp = this.maxWidth.value
            screenHeightDp = this.maxHeight.value
            performGameReset()
            playerAndScreenInitialized = true
        }

        if (playerAndScreenInitialized) {
            platforms.forEach { platform ->
                val visualScaleFactor = 4f // Make the image appear 3x bigger

                // The logical position and size for collision and placement
                val logicalPlatformWidthDp = PLATFORM_WIDTH_DP
                val logicalPlatformHeightDp = PLATFORM_HEIGHT_DP

                // Calculate the visual size
                val visualPlatformWidthDp = logicalPlatformWidthDp * visualScaleFactor
                val visualPlatformHeightDp = logicalPlatformHeightDp * visualScaleFactor

                // Calculate the offset needed to keep the *center* of the visual image
                // aligned with the *center* of the logical hitbox.
                // If we just scale it, it will scale from its top-left.
                // Offset needed = (Logical Size - Visual Size) / 2
                val visualOffsetX = (logicalPlatformWidthDp - visualPlatformWidthDp) / 2f
                val visualOffsetY = (logicalPlatformHeightDp - visualPlatformHeightDp) / 2f

                // Draw the platform image (cloud) - VISUALLY LARGER
                Image(
                    painter = painterResource(id = R.drawable.cloud_platform), // Your cloud image
                    contentDescription = "Cloud Platform",
                    modifier = Modifier
                        .absoluteOffset(
                            x = (platform.x + visualOffsetX).dp, // Adjust X for centering the visual
                            y = ((platform.y - totalScrollOffsetDp) + visualOffsetY).dp // Adjust Y for centering the visual
                        )
                        .size(visualPlatformWidthDp.dp, visualPlatformHeightDp.dp) // Draw at the larger visual size
                )

                if (DEBUG_SHOW_HITBOXES) {
                    Box(
                        modifier = Modifier
                            .absoluteOffset(
                                x = platform.x.dp, // Same X as the platform image
                                y = (platform.y - totalScrollOffsetDp).dp // Same Y as the platform image
                            )
                            .size(PLATFORM_WIDTH_DP.dp, PLATFORM_HEIGHT_DP.dp) // Same size
                            .background(Color.Red.copy(alpha = 0.3f))
                    )
                }
            }

            // Player is drawn as long as gameRunning is true OR playerIsFallingOffScreen is true.
            // Once gameRunning is false, the game over UI appears.
            // The player box itself will be drawn off-screen due to its large Y value.
            Image(
                painter = painterResource(id = R.drawable.joris_doodler), // This references your image
                contentDescription = "Joris the Doodler", // For accessibility
                modifier = Modifier
                    .absoluteOffset(
                        x = playerXPositionScreenDp.dp,
                        y = (playerYPositionWorldDp - totalScrollOffsetDp).dp
                    )
                    .size(PLAYER_WIDTH_DP.dp, PLAYER_HEIGHT_DP.dp)
            )

            Text("Score: $score", style = MaterialTheme.typography.headlineSmall, color = Color.Black,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp))

            if (!gameRunning) { // Only show Game Over UI when game loop has stopped
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("GAME OVER", style = MaterialTheme.typography.headlineMedium, color = Color.Red)
                    Text("Final Score: $score", style = MaterialTheme.typography.headlineSmall, color = Color.Black)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { performGameReset() }) { Text("Restart Game") }
                }
            }
        }

        Column(modifier = Modifier.align(Alignment.TopStart).padding(8.dp).background(Color.Black.copy(alpha = 0.3f))) {
            Text("PlayerX(scr): ${"%.1f".format(playerXPositionScreenDp)} | TiltX: ${"%.2f".format(rawTiltXForDebug)}", color = Color.White, fontSize = 10.sp)
            Text("PlayerY(world): ${"%.1f".format(playerYPositionWorldDp)} | PlayerY(scr): ${"%.1f".format(playerYPositionWorldDp - totalScrollOffsetDp)} | Vy: ${"%.1f".format(playerVelocityY)}", color = Color.White, fontSize = 10.sp)
            Text("ScrollOffset: ${"%.1f".format(totalScrollOffsetDp)} | LastScoredY: ${"%.1f".format(lastScoredWorldY)}", color = Color.White, fontSize = 10.sp)
            Text("Scrn: ${"%.0f".format(screenWidthDp)}x${"%.0f".format(screenHeightDp)}", color = Color.White, fontSize = 10.sp)
            Text(
                if (gameRunning) (if (playerIsFallingOffScreen) "FALLING OFF" else "Running") else "GAME OVER",
                color = if (gameRunning) (if (playerIsFallingOffScreen) Color.Yellow else Color.Green) else Color.Red,
                fontSize = 10.sp
            )
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
            Box(modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-70).dp)
                .size(PLAYER_WIDTH_DP.dp, PLAYER_HEIGHT_DP.dp).background(Color.Green)
            ) { Text("J", color = Color.Black, modifier = Modifier.align(Alignment.Center)) }
            Box(modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-70 + PLAYER_HEIGHT_DP + 5).dp)
                .size(PLATFORM_WIDTH_DP.dp, PLATFORM_HEIGHT_DP.dp).background(Color.DarkGray))
            Box(modifier = Modifier.align(Alignment.Center).offset(y = (-150).dp, x = 30.dp)
                .size(PLATFORM_WIDTH_DP.dp, PLATFORM_HEIGHT_DP.dp).background(Color.DarkGray))
            Text("Score: 0", style = MaterialTheme.typography.headlineSmall, color = Color.Black,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp))
            Text("Preview (No Dynamic Logic)", modifier = Modifier.align(Alignment.Center).padding(16.dp), color = Color.Black)
        }
    }
}