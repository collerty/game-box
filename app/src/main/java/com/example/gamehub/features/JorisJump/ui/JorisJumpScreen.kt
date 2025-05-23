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
import android.media.MediaPlayer // Import MediaPlayer

// Constants for the game
private const val PLAYER_WIDTH_DP = 50f
private const val PLAYER_HEIGHT_DP = 75f
private const val ACCELEROMETER_SENSITIVITY = 4.0f
private const val GRAVITY = 0.4f
private const val INITIAL_JUMP_VELOCITY = -11.5f
private const val PLATFORM_HEIGHT_DP = 15f // This is the LOGICAL height for collision
private const val PLATFORM_WIDTH_DP = 30f  // This is the LOGICAL width for collision
private const val SCROLL_THRESHOLD_ON_SCREEN_Y_FACTOR = 0.65f
private const val MAX_PLATFORMS_ON_SCREEN = 10
private const val INITIAL_PLATFORM_COUNT = 5
private const val SCORE_POINTS_PER_DP_WORLD_Y = 0.04f
private const val DEBUG_SHOW_HITBOXES = true // Set to true to see logical hitboxes

// Constants for Moving Platforms
private const val PLATFORM_BASE_MOVE_SPEED = 1.6f
private const val PLATFORM_MOVE_SPEED_VARIATION = 0.6f
// PLATFORM_MOVE_RANGE_FACTOR will be defined inside composable once screenWidthDp is known


data class PlatformState(
    val id: Int,
    var x: Float, // World X Dp from left (center of logical hitbox)
    var y: Float,  // World Y Dp from top (top of logical hitbox)
    val isMoving: Boolean = false,          // Is this platform a moving one?
    var movementDirection: Int = 1,         // 1 for right, -1 for left
    val movementSpeed: Float = 1.0f,        // Dp per frame
    val movementRange: Float = 50f,         // How far from originX it can move to one side
    val originX: Float = x                  // Initial X to oscillate around
)

private fun generateInitialPlatformsList(screenWidth: Float, screenHeight: Float, count: Int, startingId: Int): Pair<List<PlatformState>, Int> {
    val initialPlatforms = mutableListOf<PlatformState>()
    var currentNextId = startingId
    var currentY = screenHeight - PLATFORM_HEIGHT_DP - 5f // World Y of first platform's top
    val platformMoveRange = screenWidth * 0.20f // Example: move 15% of screen width to each side

    for (i in 0 until count) {
        if (initialPlatforms.size >= count && i > 0) break

        val xPos = if (i == 0) (screenWidth / 2) - (PLATFORM_WIDTH_DP / 2)
        else Random.nextFloat() * (screenWidth - PLATFORM_WIDTH_DP)
        val yPos = if (i == 0) currentY
        else currentY - (PLATFORM_HEIGHT_DP * Random.nextInt(4, 9)).toFloat() - Random.nextFloat() * PLATFORM_HEIGHT_DP * 2.5f

        val shouldMove = Random.nextInt(0, 3) == 0 // Roughly 1/3 chance

        initialPlatforms.add(
            PlatformState(
                id = currentNextId++,
                x = xPos,
                y = yPos,
                isMoving = if (i == 0) false else shouldMove, // First platform is not moving
                movementDirection = if (Random.nextBoolean()) 1 else -1,
                movementSpeed = PLATFORM_BASE_MOVE_SPEED + Random.nextFloat() * PLATFORM_MOVE_SPEED_VARIATION,
                movementRange = platformMoveRange,
                originX = xPos
            )
        )
        if (i > 0) currentY = yPos
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

    // --- Sound Effect Setup ---
    var jumpSoundPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // Load the sound effect when the composable enters the composition
    // and release it when it leaves.
    DisposableEffect(Unit) {
        // Create and prepare the MediaPlayer instance
        // Using try-catch in case the resource is missing or there's an issue creating MediaPlayer
        try {
            jumpSoundPlayer = MediaPlayer.create(context, R.raw.basic_jump_sound) // Use your sound file name
        } catch (e: Exception) {
            Log.e("JorisJump_Sound", "Error creating MediaPlayer for jump sound", e)
            jumpSoundPlayer = null // Ensure it's null if creation failed
        }

        onDispose {
            jumpSoundPlayer?.release() // Release the MediaPlayer resources
            jumpSoundPlayer = null
            Log.d("JorisJump_Sound", "Jump sound MediaPlayer released")
        }
    }

    // Helper function to play the jump sound
    fun playJumpSound() {
        try {
            if (jumpSoundPlayer?.isPlaying == true) {
                // If it's already playing, stop and restart to allow rapid jumps
                // Or, you could create multiple MediaPlayer instances or use SoundPool for overlapping sounds
                jumpSoundPlayer?.stop()
                jumpSoundPlayer?.prepare() // Need to prepare again after stop
            }
            jumpSoundPlayer?.start()
        } catch (e: Exception) {
            Log.e("JorisJump_Sound", "Error playing jump sound", e)
            // Optionally, try to re-create it if it failed before
            if (jumpSoundPlayer == null) {
                try {
                    jumpSoundPlayer = MediaPlayer.create(context, R.raw.basic_jump_sound)
                    jumpSoundPlayer?.start()
                } catch (recreateEx: Exception) {
                    Log.e("JorisJump_Sound", "Error re-creating and playing jump sound", recreateEx)
                }
            }
        }
    }

    var playerXPositionScreenDp by remember { mutableStateOf(0f) }
    var playerYPositionWorldDp by remember { mutableStateOf(0f) }
    var playerVelocityY by remember { mutableStateOf(0f) }
    var playerIsFallingOffScreen by remember { mutableStateOf(false) }

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
        playerIsFallingOffScreen = false

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
                if (!gameRunning || playerIsFallingOffScreen) return
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
        if (!playerAndScreenInitialized) return@LaunchedEffect
        while (gameRunning) {
            if (!playerIsFallingOffScreen) {
                // --- PLATFORM MOVEMENT UPDATE ---
                if (screenWidthDp > 0f) { // Ensure screenWidthDp is initialized for bounds checks
                    platforms = platforms.map { p ->
                        if (p.isMoving) {
                            var newX = p.x + (p.movementSpeed * p.movementDirection)
                            var newDirection = p.movementDirection

                            if (p.movementDirection == 1 && newX > p.originX + p.movementRange) {
                                newX = p.originX + p.movementRange
                                newDirection = -1
                            } else if (p.movementDirection == -1 && newX < p.originX - p.movementRange) {
                                newX = p.originX - p.movementRange
                                newDirection = 1
                            }
                            newX = newX.coerceIn(0f, screenWidthDp - PLATFORM_WIDTH_DP) // Ensure stays on screen
                            p.copy(x = newX, movementDirection = newDirection)
                        } else {
                            p
                        }
                    }
                }

                // --- REGULAR GAMEPLAY LOGIC ---
                playerVelocityY += GRAVITY
                playerYPositionWorldDp += playerVelocityY

                val yForNextPoint = lastScoredWorldY - (1.0f / SCORE_POINTS_PER_DP_WORLD_Y)
                if (playerYPositionWorldDp < yForNextPoint) {
                    val pointsEarned = ((lastScoredWorldY - playerYPositionWorldDp) * SCORE_POINTS_PER_DP_WORLD_Y).toInt()
                    if (pointsEarned > 0) { score += pointsEarned; lastScoredWorldY -= pointsEarned * (1.0f / SCORE_POINTS_PER_DP_WORLD_Y) }
                }

                val playerBottomWorldDp = playerYPositionWorldDp + PLAYER_HEIGHT_DP
                val playerRightScreenDp = playerXPositionScreenDp + PLAYER_WIDTH_DP
                var didLandAndJumpThisFrame = false
                if (playerVelocityY > 0) {
                    platforms.forEach { platform ->
                        if (didLandAndJumpThisFrame) return@forEach
                        val xOverlaps = playerRightScreenDp > platform.x && playerXPositionScreenDp < (platform.x + PLATFORM_WIDTH_DP)
                        if (xOverlaps) {
                            val previousPlayerBottomWorldDp = playerBottomWorldDp - playerVelocityY
                            if (previousPlayerBottomWorldDp <= platform.y && playerBottomWorldDp >= platform.y) {
                                playerYPositionWorldDp = platform.y - PLAYER_HEIGHT_DP
                                playerVelocityY = INITIAL_JUMP_VELOCITY
                                didLandAndJumpThisFrame = true
                                playJumpSound()
                                Log.d("JorisJump_Collision", "Landed on platform ${platform.id}. PlayerY set to: $playerYPositionWorldDp")
                            }
                        }
                    }
                }

                val playerYOnScreen = playerYPositionWorldDp - totalScrollOffsetDp
                val scrollThresholdActualScreenY = screenHeightDp * SCROLL_THRESHOLD_ON_SCREEN_Y_FACTOR
                if (playerYOnScreen < scrollThresholdActualScreenY) {
                    val scrollAmount = scrollThresholdActualScreenY - playerYOnScreen
                    totalScrollOffsetDp -= scrollAmount
                }

                val currentPlatformsMutable = platforms.toMutableList()
                currentPlatformsMutable.removeAll { it.y > totalScrollOffsetDp + screenHeightDp + PLATFORM_HEIGHT_DP * 2 }
                val generationCeilingWorldY = totalScrollOffsetDp - (screenHeightDp * 1.0f)
                var highestExistingPlatformY = currentPlatformsMutable.minOfOrNull { it.y } ?: (totalScrollOffsetDp + screenHeightDp / 2)
                var generationAttemptsInTick = 0
                val platformMoveRange = screenWidthDp * 0.15f // Define here for dynamic generation

                while (currentPlatformsMutable.size < MAX_PLATFORMS_ON_SCREEN &&
                    highestExistingPlatformY > generationCeilingWorldY &&
                    generationAttemptsInTick < MAX_PLATFORMS_ON_SCREEN) {
                    generationAttemptsInTick++
                    var newXGen = Random.nextFloat() * (screenWidthDp - PLATFORM_WIDTH_DP)
                    val minVerticalGapFactor = 5.5f
                    val maxVerticalGapFactor = 9.5f
                    val verticalGap = (PLATFORM_HEIGHT_DP * (minVerticalGapFactor + Random.nextFloat() * (maxVerticalGapFactor - minVerticalGapFactor)))
                    val newYGen = highestExistingPlatformY - verticalGap
                    val platformImmediatelyBelow = currentPlatformsMutable.firstOrNull()
                    if (platformImmediatelyBelow != null && kotlin.math.abs(newXGen - platformImmediatelyBelow.x) < PLATFORM_WIDTH_DP * 0.75f) {
                        if (Random.nextFloat() < 0.6) {
                            val shiftDirection = if (platformImmediatelyBelow.x < screenWidthDp / 2) 1 else -1
                            newXGen = (platformImmediatelyBelow.x + shiftDirection * (PLATFORM_WIDTH_DP * 2 + Random.nextFloat() * screenWidthDp * 0.2f))
                                .coerceIn(0f, screenWidthDp - PLATFORM_WIDTH_DP)
                        }
                    }
                    val shouldMove = Random.nextInt(0, 3) == 0
                    currentPlatformsMutable.add(0, PlatformState(
                        id = nextPlatformId++, x = newXGen, y = newYGen,
                        isMoving = shouldMove,
                        movementDirection = if (Random.nextBoolean()) 1 else -1,
                        movementSpeed = PLATFORM_BASE_MOVE_SPEED + Random.nextFloat() * PLATFORM_MOVE_SPEED_VARIATION,
                        movementRange = if(screenWidthDp > 0) platformMoveRange else 50f,
                        originX = newXGen
                    ))
                    highestExistingPlatformY = newYGen
                }
                platforms = currentPlatformsMutable.toList()

                if (playerYPositionWorldDp > totalScrollOffsetDp + screenHeightDp) {
                    if (!playerIsFallingOffScreen) { playerIsFallingOffScreen = true }
                }
            } else {
                playerVelocityY += GRAVITY
                playerYPositionWorldDp += playerVelocityY
                if (playerYPositionWorldDp > totalScrollOffsetDp + screenHeightDp + PLAYER_HEIGHT_DP * 2.5f) {
                    gameRunning = false
                }
            }
            delay(16)
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
        // .background(Color(0xFF87CEEB)) // Replaced by Image
    ) {
        Image(
            painter = painterResource(id = R.drawable.background_doodle),
            contentDescription = "Joris Jump Game Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        if (!playerAndScreenInitialized && this.maxWidth > 0.dp && this.maxHeight > 0.dp) {
            screenWidthDp = this.maxWidth.value
            screenHeightDp = this.maxHeight.value
            performGameReset()
            playerAndScreenInitialized = true
        }

        if (playerAndScreenInitialized) {
            platforms.forEach { platform ->
                val visualScaleFactor = 4f
                val logicalPlatformWidthDp = PLATFORM_WIDTH_DP
                val logicalPlatformHeightDp = PLATFORM_HEIGHT_DP
                val visualPlatformWidthDp = logicalPlatformWidthDp * visualScaleFactor
                val visualPlatformHeightDp = logicalPlatformHeightDp * visualScaleFactor
                val visualOffsetX = (logicalPlatformWidthDp - visualPlatformWidthDp) / 2f
                val visualOffsetY = (logicalPlatformHeightDp - visualPlatformHeightDp) / 2f

                Image(
                    painter = painterResource(id = R.drawable.cloud_platform),
                    contentDescription = "Cloud Platform",
                    modifier = Modifier
                        .absoluteOffset(
                            x = (platform.x + visualOffsetX).dp,
                            y = ((platform.y - totalScrollOffsetDp) + visualOffsetY).dp
                        )
                        .size(visualPlatformWidthDp.dp, visualPlatformHeightDp.dp)
                )
                if (DEBUG_SHOW_HITBOXES) {
                    Box(modifier = Modifier
                        .absoluteOffset(x = platform.x.dp, y = (platform.y - totalScrollOffsetDp).dp)
                        .size(PLATFORM_WIDTH_DP.dp, PLATFORM_HEIGHT_DP.dp) // Use logical size for hitbox
                        .background(Color.Red.copy(alpha = 0.3f)))
                }
            }

            Image(
                painter = painterResource(id = R.drawable.joris_doodler),
                contentDescription = "Joris the Doodler",
                modifier = Modifier
                    .absoluteOffset(x = playerXPositionScreenDp.dp, y = (playerYPositionWorldDp - totalScrollOffsetDp).dp)
                    .size(PLAYER_WIDTH_DP.dp, PLAYER_HEIGHT_DP.dp)
            )
            if (DEBUG_SHOW_HITBOXES) {
                Box(modifier = Modifier
                    .absoluteOffset(x = playerXPositionScreenDp.dp, y = (playerYPositionWorldDp - totalScrollOffsetDp).dp)
                    .size(PLAYER_WIDTH_DP.dp, PLAYER_HEIGHT_DP.dp)
                    .background(Color.Blue.copy(alpha = 0.3f)))
            }

            Text("Score: $score", style = MaterialTheme.typography.headlineSmall, color = Color.Black,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp))

            if (!gameRunning) {
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
            Text(if (gameRunning) (if (playerIsFallingOffScreen) "FALLING OFF" else "Running") else "GAME OVER",
                color = if (gameRunning) (if (playerIsFallingOffScreen) Color.Yellow else Color.Green) else Color.Red, fontSize = 10.sp)
            Text("Platforms: ${platforms.size} | NextID: $nextPlatformId", color = Color.White, fontSize = 10.sp)
            if (accelerometer == null) Text("ACCEL N/A!", color = Color.Red, fontSize = 10.sp)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun JorisJumpScreenPreview() {
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(painter = painterResource(id = R.drawable.background_doodle), contentDescription = "Preview Background",
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-70).dp)
                .size(PLAYER_WIDTH_DP.dp, PLAYER_HEIGHT_DP.dp)) {
                Image(painter = painterResource(id = R.drawable.joris_doodler), contentDescription = "Preview Doodler")
            }
            Box(modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-70 + PLAYER_HEIGHT_DP + 5).dp)
                .size(PLATFORM_WIDTH_DP.dp * 4, PLATFORM_HEIGHT_DP.dp * 4) // Preview visual size for cloud
            ) {
                Image(painter = painterResource(id = R.drawable.cloud_platform), contentDescription = "Preview Cloud")
            }
            Text("Score: 0", style = MaterialTheme.typography.headlineSmall, color = Color.Black,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp))
            Text("Preview (No Dynamic Logic)", modifier = Modifier.align(Alignment.Center).padding(16.dp), color = Color.Black)
        }
    }
}