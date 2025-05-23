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
import android.media.MediaPlayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

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
private const val DEBUG_SHOW_HITBOXES = false // Set to true to see logical hitboxes

// Constants for Moving Platforms
private const val PLATFORM_BASE_MOVE_SPEED = 1.6f
private const val PLATFORM_MOVE_SPEED_VARIATION = 0.6f

// Constants for Spring Power-up
private const val SPRING_VISUAL_WIDTH_FACTOR = 0.6f // Spring visual width relative to platform logical width
private const val SPRING_VISUAL_HEIGHT_FACTOR = 1.8f // Spring visual height relative to platform logical height

val arcadeFontFamily = FontFamily(
    Font(R.font.arcade_classic, FontWeight.Normal)
)


data class PlatformState(
    val id: Int,
    var x: Float,
    var y: Float,
    val isMoving: Boolean = false,
    var movementDirection: Int = 1,
    val movementSpeed: Float = 1.0f,
    val movementRange: Float = 50f,
    val originX: Float = x,
    val hasSpring: Boolean = false, // Does this platform have a spring?
    val springJumpFactor: Float = 1.65f // How much higher the spring makes you jump
)

private fun generateInitialPlatformsList(screenWidth: Float, screenHeight: Float, count: Int, startingId: Int): Pair<List<PlatformState>, Int> {
    val initialPlatforms = mutableListOf<PlatformState>()
    var currentNextId = startingId
    var currentY = screenHeight - PLATFORM_HEIGHT_DP - 5f
    val platformMoveRange = screenWidth * 0.20f

    for (i in 0 until count) {
        if (initialPlatforms.size >= count && i > 0) break
        val xPos = if (i == 0) (screenWidth / 2) - (PLATFORM_WIDTH_DP / 2) else Random.nextFloat() * (screenWidth - PLATFORM_WIDTH_DP)
        val yPos = if (i == 0) currentY else currentY - (PLATFORM_HEIGHT_DP * Random.nextInt(4, 9)).toFloat() - Random.nextFloat() * PLATFORM_HEIGHT_DP * 2.5f
        val shouldMove = Random.nextInt(0, 3) == 0
        val shouldHaveSpring = Random.nextInt(0, 8) == 0 // 1 in 10 chance

        initialPlatforms.add(
            PlatformState(
                id = currentNextId++, x = xPos, y = yPos,
                isMoving = if (i == 0) false else shouldMove, // First platform no move/spring
                movementDirection = if (Random.nextBoolean()) 1 else -1,
                movementSpeed = PLATFORM_BASE_MOVE_SPEED + Random.nextFloat() * PLATFORM_MOVE_SPEED_VARIATION,
                movementRange = platformMoveRange, originX = xPos,
                hasSpring = if (i == 0) false else shouldHaveSpring, // First platform no spring
                springJumpFactor = 1.65f + Random.nextFloat() * 0.5f // Example: Boost between 1.65x and 2.15x
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

    // Immersive Mode Effect
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

    // Sound Effect Setup
    var jumpSoundPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var springSoundPlayer by remember { mutableStateOf<MediaPlayer?>(null) } // For spring sound

    DisposableEffect(Unit) {
        try {
            jumpSoundPlayer = MediaPlayer.create(context, R.raw.basic_jump_sound)
            springSoundPlayer = MediaPlayer.create(context, R.raw.spring_jump_sound)
        } catch (e: Exception) {
            Log.e("JorisJump_Sound", "Error creating MediaPlayers", e)
        }
        onDispose {
            jumpSoundPlayer?.release(); jumpSoundPlayer = null
            springSoundPlayer?.release(); springSoundPlayer = null
            Log.d("JorisJump_Sound", "MediaPlayers released")
        }
    }

    fun playSound(player: MediaPlayer?) {
        try {
            player?.let {
                if (it.isPlaying) {
                    it.stop()
                    it.prepare() // MediaPlayer needs prepare() after stop() before start()
                }
                it.start()
            }
        } catch (e: Exception) {
            Log.e("JorisJump_Sound", "Error playing sound", e)
        }
    }

    // Game State Variables
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

    // Game Reset Function
    fun performGameReset() {
        if (screenWidthDp == 0f || screenHeightDp == 0f) { Log.e("JorisJump", "Reset Aborted: Screen Dims N/A."); return }
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

    // Sensor Input Effect
    DisposableEffect(accelerometer, playerAndScreenInitialized) {
        if (accelerometer == null) { Log.e("JorisJump", "Accelerometer N/A!"); return@DisposableEffect onDispose {} }
        if (!playerAndScreenInitialized) return@DisposableEffect onDispose {}
        val sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (!gameRunning || playerIsFallingOffScreen) return
                event?.let {
                    if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        val tiltX = it.values[0]; rawTiltXForDebug = tiltX
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

    // Game Loop
    LaunchedEffect(gameRunning, playerAndScreenInitialized) {
        if (!playerAndScreenInitialized) return@LaunchedEffect
        while (gameRunning) {
            if (!playerIsFallingOffScreen) {
                // Platform Movement
                if (screenWidthDp > 0f) {
                    platforms = platforms.map { p ->
                        if (p.isMoving) {
                            var newX = p.x + (p.movementSpeed * p.movementDirection)
                            var newDirection = p.movementDirection
                            if (p.movementDirection == 1 && newX > p.originX + p.movementRange) { newX = p.originX + p.movementRange; newDirection = -1 }
                            else if (p.movementDirection == -1 && newX < p.originX - p.movementRange) { newX = p.originX - p.movementRange; newDirection = 1 }
                            newX = newX.coerceIn(0f, screenWidthDp - PLATFORM_WIDTH_DP)
                            p.copy(x = newX, movementDirection = newDirection)
                        } else p
                    }
                }

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
                                if (platform.hasSpring) {
                                    playerVelocityY = INITIAL_JUMP_VELOCITY * platform.springJumpFactor
                                    playSound(springSoundPlayer) // Play spring sound (if you have one)
                                    Log.d("JorisJump_Spring", "Landed on SPRING! Factor: ${platform.springJumpFactor}")
                                } else {
                                    playerVelocityY = INITIAL_JUMP_VELOCITY
                                    playSound(jumpSoundPlayer) // Play normal jump sound
                                }
                                didLandAndJumpThisFrame = true
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
                val platformMoveRange = screenWidthDp * 0.15f

                while (currentPlatformsMutable.size < MAX_PLATFORMS_ON_SCREEN &&
                    highestExistingPlatformY > generationCeilingWorldY &&
                    generationAttemptsInTick < MAX_PLATFORMS_ON_SCREEN) {
                    generationAttemptsInTick++
                    var newXGen = Random.nextFloat() * (screenWidthDp - PLATFORM_WIDTH_DP)
                    val minVerticalGapFactor = 5.5f; val maxVerticalGapFactor = 9.5f
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
                    val shouldHaveSpring = Random.nextInt(0, 10) == 0
                    currentPlatformsMutable.add(0, PlatformState(
                        id = nextPlatformId++, x = newXGen, y = newYGen,
                        isMoving = shouldMove,
                        movementDirection = if (Random.nextBoolean()) 1 else -1,
                        movementSpeed = PLATFORM_BASE_MOVE_SPEED + Random.nextFloat() * PLATFORM_MOVE_SPEED_VARIATION,
                        movementRange = if(screenWidthDp > 0) platformMoveRange else 50f, originX = newXGen,
                        hasSpring = shouldHaveSpring,
                        springJumpFactor = 1.65f + Random.nextFloat() * 0.5f // Example: Boost between 1.65x and 2.15x
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
    ) {
        Image(painter = painterResource(id = R.drawable.background_doodle), contentDescription = "Game Background",
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)

        if (!playerAndScreenInitialized && this.maxWidth > 0.dp && this.maxHeight > 0.dp) {
            screenWidthDp = this.maxWidth.value; screenHeightDp = this.maxHeight.value
            performGameReset(); playerAndScreenInitialized = true
        }

        if (playerAndScreenInitialized) {
            platforms.forEach { platform ->
                val visualCloudScaleFactor = 4f
                val currentPlatformLogicalWidthDp = PLATFORM_WIDTH_DP
                val currentPlatformLogicalHeightDp = PLATFORM_HEIGHT_DP

                val visualCloudWidthDp = currentPlatformLogicalWidthDp * visualCloudScaleFactor
                val visualCloudHeightDp = currentPlatformLogicalHeightDp * visualCloudScaleFactor
                val visualCloudOffsetX = (currentPlatformLogicalWidthDp - visualCloudWidthDp) / 2f
                val visualCloudOffsetY = (currentPlatformLogicalHeightDp - visualCloudHeightDp) / 2f

                // Draw Cloud Platform Image
                Image(
                    painter = painterResource(id = R.drawable.cloud_platform),
                    contentDescription = "Cloud Platform",
                    modifier = Modifier
                        .absoluteOffset(
                            x = (platform.x + visualCloudOffsetX).dp,
                            y = ((platform.y - totalScrollOffsetDp) + visualCloudOffsetY).dp
                        )
                        .size(visualCloudWidthDp.dp, visualCloudHeightDp.dp)
                )

                if (platform.hasSpring) {
                    // --- Spring Visuals & Positioning ---
                    // Define spring's visual size based on the LOGICAL platform dimensions
                    val springVisualWidth = currentPlatformLogicalWidthDp * SPRING_VISUAL_WIDTH_FACTOR
                    val springVisualHeight = currentPlatformLogicalHeightDp * SPRING_VISUAL_HEIGHT_FACTOR

                    // Calculate position for the spring's TOP-LEFT corner
                    // to center it HORIZONTALLY on the LOGICAL platform.
                    val springX_onPlatform = platform.x + (currentPlatformLogicalWidthDp / 2f) - (springVisualWidth / 2f)
                    // Position spring's BOTTOM to be slightly above or on the LOGICAL platform's TOP.
                    val springY_onPlatform = platform.y - springVisualHeight + (currentPlatformLogicalHeightDp * 0.2f) // Adjust 0.2f to make it sit higher/lower

                    Image(
                        painter = painterResource(id = R.drawable.spring_mushroom),
                        contentDescription = "Spring Mushroom",
                        modifier = Modifier
                            .absoluteOffset(
                                x = springX_onPlatform.dp,
                                y = (springY_onPlatform - totalScrollOffsetDp).dp
                            )
                            .size(springVisualWidth.dp, springVisualHeight.dp)
                    )

                    // --- Draw Spring Hitbox (for visual debugging of the spring image) ---
                    if (DEBUG_SHOW_HITBOXES) {
                        Box(
                            modifier = Modifier
                                .absoluteOffset(
                                    x = springX_onPlatform.dp,
                                    y = (springY_onPlatform - totalScrollOffsetDp).dp
                                )
                                .size(springVisualWidth.dp, springVisualHeight.dp)
                                .background(Color.Cyan.copy(alpha = 0.4f))
                        )
                    }
                }

                if (DEBUG_SHOW_HITBOXES) {
                    Box(modifier = Modifier
                        .absoluteOffset(x = platform.x.dp, y = (platform.y - totalScrollOffsetDp).dp)
                        .size(PLATFORM_WIDTH_DP.dp, PLATFORM_HEIGHT_DP.dp)
                        .background(Color.Red.copy(alpha = 0.3f)))
                }
            }

            Image(painter = painterResource(id = R.drawable.joris_doodler), contentDescription = "Joris the Doodler",
                modifier = Modifier
                    .absoluteOffset(x = playerXPositionScreenDp.dp, y = (playerYPositionWorldDp - totalScrollOffsetDp).dp)
                    .size(PLAYER_WIDTH_DP.dp, PLAYER_HEIGHT_DP.dp))
            if (DEBUG_SHOW_HITBOXES) {
                Box(modifier = Modifier
                    .absoluteOffset(x = playerXPositionScreenDp.dp, y = (playerYPositionWorldDp - totalScrollOffsetDp).dp)
                    .size(PLAYER_WIDTH_DP.dp, PLAYER_HEIGHT_DP.dp)
                    .background(Color.Blue.copy(alpha = 0.3f)))
            }

            Text(
                text = "Score: $score",
                style = TextStyle(
                    fontFamily = arcadeFontFamily, // <<< ADD THIS LINE
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold, // You can try FontWeight.Normal if your font handles weight differently
                    color = Color(0xFFFFFDE7),
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.5f),
                        offset = Offset(x = 2f, y = 3f),
                        blurRadius = 3f
                    )
                ),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp)
            )

            if (!gameRunning) {
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                            "GAME OVER",
                    style = TextStyle(
                        fontFamily = arcadeFontFamily, // <<< ADD THIS LINE
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold, // Or FontWeight.Bold / FontWeight.Normal
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
                            fontFamily = arcadeFontFamily, // <<< ADD THIS LINE
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold, // Or FontWeight.Normal
                            color = Color.White,
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.5f),
                                offset = Offset(2f, 3f),
                                blurRadius = 3f
                            )
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { performGameReset() }) { Text(
                        "Restart Game",
                        style = TextStyle( // Apply TextStyle here too
                            fontFamily = arcadeFontFamily, // <<< ADD THIS LINE
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold // Or FontWeight.Normal
                            // color will be inherited from button's contentColor
                        )
                    ) }
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
            val previewCloudVisualFactor = 4f
            Box(modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-70 + PLAYER_HEIGHT_DP + 5 - (PLATFORM_HEIGHT_DP * (previewCloudVisualFactor -1)/2) ).dp) // Approx cloud pos
                .size(PLATFORM_WIDTH_DP.dp * previewCloudVisualFactor, PLATFORM_HEIGHT_DP.dp * previewCloudVisualFactor)
            ) {
                Image(painter = painterResource(id = R.drawable.cloud_platform), contentDescription = "Preview Cloud")
            }
            // Approx spring on cloud
            Box(modifier = Modifier.align(Alignment.BottomCenter)
                .offset(
                    y = (-70 + PLAYER_HEIGHT_DP + 5 - (PLATFORM_HEIGHT_DP * (previewCloudVisualFactor -1)/2) - (PLATFORM_HEIGHT_DP * SPRING_VISUAL_HEIGHT_FACTOR * previewCloudVisualFactor * 0.7f) ).dp,
                    x = ( (PLATFORM_WIDTH_DP * previewCloudVisualFactor / 2f) - (PLATFORM_WIDTH_DP* SPRING_VISUAL_WIDTH_FACTOR * previewCloudVisualFactor / 2f) ).dp // Centered on cloud
                )
                .size( (PLATFORM_WIDTH_DP* SPRING_VISUAL_WIDTH_FACTOR * previewCloudVisualFactor).dp, (PLATFORM_HEIGHT_DP * SPRING_VISUAL_HEIGHT_FACTOR * previewCloudVisualFactor).dp )
            ) {
                Image(painter = painterResource(id = R.drawable.spring_mushroom), contentDescription = "Preview Spring")
            }

            Text("Score: 0", style = MaterialTheme.typography.headlineSmall, color = Color.Black,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp))
            Text("Preview (No Dynamic Logic)", modifier = Modifier.align(Alignment.Center).padding(16.dp), color = Color.Black)
        }
    }
}