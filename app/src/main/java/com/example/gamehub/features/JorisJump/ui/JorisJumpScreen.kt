package com.example.gamehub.features.jorisjump.ui

import android.app.Activity // Required for Window operations
import android.content.Context
import android.graphics.RectF
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
private const val DEBUG_SHOW_HITBOXES = true // Set to true to see logical hitboxes

// Constants for Moving Platforms
private const val PLATFORM_BASE_MOVE_SPEED = 1.6f
private const val PLATFORM_MOVE_SPEED_VARIATION = 0.6f

// Constants for Spring Power-up
private const val SPRING_VISUAL_WIDTH_FACTOR = 0.6f
private const val SPRING_VISUAL_HEIGHT_FACTOR = 1.8f

// Constants for Enemies
private const val ENEMY_WIDTH_DP = 40f
private const val ENEMY_HEIGHT_DP = 40f
private const val ENEMY_SPAWN_CHANCE_PER_PLATFORM_ROW = 0.10f // 10% chance per generated platform "row"
private const val MAX_ENEMIES_ON_SCREEN = 3
private const val ENEMY_TWITCH_AMOUNT_DP = 1f
private const val ENEMY_TWITCH_SPEED_FACTOR = 0.05f


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
    val hasSpring: Boolean = false,
    val springJumpFactor: Float = 1.65f
)

data class EnemyState(
    val id: Int,
    var x: Float, // World X Dp from left
    var y: Float, // World Y Dp from top
    var visualOffsetX: Float = 0f, // For twitching animation
    var visualOffsetY: Float = 0f  // For twitching animation
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
        val shouldHaveSpring = Random.nextInt(0, 8) == 0

        initialPlatforms.add(
            PlatformState(
                id = currentNextId++, x = xPos, y = yPos,
                isMoving = if (i == 0) false else shouldMove,
                movementDirection = if (Random.nextBoolean()) 1 else -1,
                movementSpeed = PLATFORM_BASE_MOVE_SPEED + Random.nextFloat() * PLATFORM_MOVE_SPEED_VARIATION,
                movementRange = platformMoveRange, originX = xPos,
                hasSpring = if (i == 0) false else shouldHaveSpring,
                springJumpFactor = if (shouldHaveSpring) 1.65f + Random.nextFloat() * 0.5f else 1.0f
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

    var enemies by remember { mutableStateOf<List<EnemyState>>(emptyList()) }
    var nextEnemyId by remember { mutableStateOf(0) }
    // timeSinceLastEnemySpawnAttempt is removed as enemies spawn with platforms now

    DisposableEffect(Unit) { // Immersive Mode
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
            Log.d("JorisJump_Lifecycle", "Exiting Immersive Mode")
        }
    }

    var jumpSoundPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var springSoundPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    DisposableEffect(Unit) { // Sounds
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
                if (it.isPlaying) { it.stop(); it.prepare() }
                it.start()
            }
        } catch (e: Exception) { Log.e("JorisJump_Sound", "Error playing sound", e) }
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
        if (screenWidthDp == 0f || screenHeightDp == 0f) { Log.e("JorisJump_Reset", "Reset Aborted: Screen Dims N/A ($screenWidthDp x $screenHeightDp)"); return }
        Log.d("JorisJump_Reset", "Performing game reset.")
        totalScrollOffsetDp = 0f
        playerXPositionScreenDp = (screenWidthDp / 2) - (PLAYER_WIDTH_DP / 2)
        playerYPositionWorldDp = screenHeightDp - PLAYER_HEIGHT_DP - PLATFORM_HEIGHT_DP - 30f
        playerVelocityY = 0f
        playerIsFallingOffScreen = false
        val (initialPlatformsList, updatedNextId) = generateInitialPlatformsList(screenWidthDp, screenHeightDp, INITIAL_PLATFORM_COUNT, 0)
        platforms = initialPlatformsList
        nextPlatformId = updatedNextId
        enemies = emptyList()
        nextEnemyId = 0
        score = 0
        lastScoredWorldY = playerYPositionWorldDp
        gameRunning = true
    }

    DisposableEffect(accelerometer, playerAndScreenInitialized) { // Sensor Input
        if (accelerometer == null) { Log.e("JorisJump_Sensor", "Accelerometer N/A!"); return@DisposableEffect onDispose {} }
        if (!playerAndScreenInitialized) { Log.d("JorisJump_Sensor", "Sensor Waiting for init."); return@DisposableEffect onDispose {} }
        Log.d("JorisJump_Sensor", "Registering sensor listener.")
        val eventListener = object : SensorEventListener {
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
        sensorManager.registerListener(eventListener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        onDispose { Log.d("JorisJump_Sensor", "Unregistering sensor listener."); sensorManager.unregisterListener(eventListener) }
    }

    LaunchedEffect(gameRunning, playerAndScreenInitialized) { // Game Loop
        if (!playerAndScreenInitialized) { Log.d("JorisJump_GameLoop", "Loop waiting for init."); return@LaunchedEffect }
        Log.d("JorisJump_GameLoop", "Game loop started. GameRunning: $gameRunning")

        var gameTimeMillis = 0L
        while (gameRunning) {
            gameTimeMillis += 16

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

                // Player Physics
                playerVelocityY += GRAVITY
                playerYPositionWorldDp += playerVelocityY

                // Score Update
                val yForNextPoint = lastScoredWorldY - (1.0f / SCORE_POINTS_PER_DP_WORLD_Y)
                if (playerYPositionWorldDp < yForNextPoint) {
                    val pointsEarned = ((lastScoredWorldY - playerYPositionWorldDp) * SCORE_POINTS_PER_DP_WORLD_Y).toInt()
                    if (pointsEarned > 0) { score += pointsEarned; lastScoredWorldY -= pointsEarned * (1.0f / SCORE_POINTS_PER_DP_WORLD_Y) }
                }

                // Collision with Platforms
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
                                    playSound(springSoundPlayer)
                                } else {
                                    playerVelocityY = INITIAL_JUMP_VELOCITY
                                    playSound(jumpSoundPlayer)
                                }
                                didLandAndJumpThisFrame = true
                            }
                        }
                    }
                }

                // Camera Scrolling
                val playerYOnScreen = playerYPositionWorldDp - totalScrollOffsetDp
                val scrollThresholdActualScreenY = screenHeightDp * SCROLL_THRESHOLD_ON_SCREEN_Y_FACTOR
                if (playerYOnScreen < scrollThresholdActualScreenY) {
                    val scrollAmount = scrollThresholdActualScreenY - playerYOnScreen
                    totalScrollOffsetDp -= scrollAmount
                }

                // Enemy Twitching (Applied to all existing enemies)
                enemies = enemies.map { enemy ->
                    val twitchAngle = (gameTimeMillis * ENEMY_TWITCH_SPEED_FACTOR + enemy.id)
                    enemy.copy(
                        visualOffsetX = kotlin.math.sin(twitchAngle.toDouble()).toFloat() * ENEMY_TWITCH_AMOUNT_DP,
                        visualOffsetY = kotlin.math.cos(twitchAngle.toDouble() * 0.7).toFloat() * ENEMY_TWITCH_AMOUNT_DP
                    )
                }
                // Enemy Removal
                enemies = enemies.filter { enemy ->
                    enemy.y < totalScrollOffsetDp + screenHeightDp + ENEMY_HEIGHT_DP * 3
                }

                // Collision With Enemies
                val playerTopOnScreen = playerYPositionWorldDp - totalScrollOffsetDp
                val playerRectForEnemyCollision = RectF(
                    playerXPositionScreenDp, playerTopOnScreen,
                    playerXPositionScreenDp + PLAYER_WIDTH_DP, playerTopOnScreen + PLAYER_HEIGHT_DP
                )
                var hitEnemyThisFrame = false
                enemies.forEach { enemy ->
                    if (hitEnemyThisFrame) return@forEach // Process only one enemy hit per frame
                    val enemyTopOnScreen = enemy.y - totalScrollOffsetDp
                    val enemyRectForCollision = RectF(
                        enemy.x, enemyTopOnScreen,
                        enemy.x + ENEMY_WIDTH_DP, enemyTopOnScreen + ENEMY_HEIGHT_DP
                    )
                    if (playerRectForEnemyCollision.intersect(enemyRectForCollision)) {
                        gameRunning = false
                        playerIsFallingOffScreen = true // Initiate fall-off sequence
                        hitEnemyThisFrame = true
                        Log.d("JorisJump_Enemy", "OUCH! Hit Enemy ${enemy.id}! Game Over.")
                    }
                }


                // Platform Generation (and integrated Enemy Generation)
                val currentPlatformsMutable = platforms.toMutableList()
                currentPlatformsMutable.removeAll { it.y > totalScrollOffsetDp + screenHeightDp + PLATFORM_HEIGHT_DP * 2 }
                val generationCeilingWorldY = totalScrollOffsetDp - screenHeightDp // Target one screen height above current view
                var highestExistingPlatformY = currentPlatformsMutable.minOfOrNull { it.y } ?: (totalScrollOffsetDp + screenHeightDp / 2f)
                var generationAttemptsInTick = 0
                val platformMoveRange = screenWidthDp * 0.15f

                while (currentPlatformsMutable.size < MAX_PLATFORMS_ON_SCREEN &&
                    highestExistingPlatformY > generationCeilingWorldY &&
                    generationAttemptsInTick < MAX_PLATFORMS_ON_SCREEN) {
                    generationAttemptsInTick++
                    var newPlatformX = Random.nextFloat() * (screenWidthDp - PLATFORM_WIDTH_DP)
                    val minVerticalGapFactor = 5.5f; val maxVerticalGapFactor = 9.5f
                    val verticalGap = (PLATFORM_HEIGHT_DP * (minVerticalGapFactor + Random.nextFloat() * (maxVerticalGapFactor - minVerticalGapFactor)))
                    val newPlatformY = highestExistingPlatformY - verticalGap

                    val platformImmediatelyBelow = currentPlatformsMutable.firstOrNull()
                    if (platformImmediatelyBelow != null && kotlin.math.abs(newPlatformX - platformImmediatelyBelow.x) < PLATFORM_WIDTH_DP * 0.75f) {
                        if (Random.nextFloat() < 0.6) {
                            val shiftDirection = if (platformImmediatelyBelow.x < screenWidthDp / 2) 1 else -1
                            newPlatformX = (platformImmediatelyBelow.x + shiftDirection * (PLATFORM_WIDTH_DP * 2 + Random.nextFloat() * screenWidthDp * 0.2f))
                                .coerceIn(0f, screenWidthDp - PLATFORM_WIDTH_DP)
                        }
                    }
                    val shouldMovePlatform = Random.nextInt(0, 3) == 0
                    val shouldHaveSpringOnPlatform = Random.nextInt(0, 8) == 0
                    currentPlatformsMutable.add(0, PlatformState(
                        id = nextPlatformId++, x = newPlatformX, y = newPlatformY,
                        isMoving = shouldMovePlatform,
                        movementDirection = if (Random.nextBoolean()) 1 else -1,
                        movementSpeed = PLATFORM_BASE_MOVE_SPEED + Random.nextFloat() * PLATFORM_MOVE_SPEED_VARIATION,
                        movementRange = if(screenWidthDp > 0) platformMoveRange else 50f, originX = newPlatformX,
                        hasSpring = shouldHaveSpringOnPlatform,
                        springJumpFactor = if (shouldHaveSpringOnPlatform) 1.65f + Random.nextFloat() * 0.5f else 1.0f
                    ))
                    highestExistingPlatformY = newPlatformY

                    // --- NEW: TRY TO SPAWN AN ENEMY WITH/NEAR THIS PLATFORM ---
                    if (enemies.size < MAX_ENEMIES_ON_SCREEN &&
                        Random.nextFloat() < ENEMY_SPAWN_CHANCE_PER_PLATFORM_ROW &&
                        screenWidthDp > 0f) {

                        var attemptSpawn = true
                        var enemyX = 0f
                        var enemyY = 0f
                        val safetyMarginDp = PLATFORM_WIDTH_DP * 1.5f // Don't spawn enemy X within this margin of a platform's X

                        // Try a few times to find a good position, otherwise skip this spawn
                        for (spawnAttempt in 0..2) { // Try up to 3 times
                            enemyX = Random.nextFloat() * (screenWidthDp - ENEMY_WIDTH_DP)
                            val enemyYOffsetFromPlatform = (Random.nextFloat() - 0.5f) * PLATFORM_HEIGHT_DP * 8 // +/- 4 platform heights
                            enemyY = newPlatformY + enemyYOffsetFromPlatform // newPlatformY is the Y of the platform just generated

                            // Check against the platform that was JUST generated (newPlatformX, newPlatformY)
                            val platformRect = RectF(
                                newPlatformX,
                                newPlatformY,
                                newPlatformX + PLATFORM_WIDTH_DP,
                                newPlatformY + PLATFORM_HEIGHT_DP
                            )
                            val enemyRectCandidate = RectF(
                                enemyX,
                                enemyY,
                                enemyX + ENEMY_WIDTH_DP,
                                enemyY + ENEMY_HEIGHT_DP
                            )

                            // Check horizontal proximity to the platform it's "associated" with
                            val tooCloseHorizontallyToItsPlatform =
                                (enemyX + ENEMY_WIDTH_DP > newPlatformX - safetyMarginDp && // Enemy right is past platform left minus margin
                                        enemyX < newPlatformX + PLATFORM_WIDTH_DP + safetyMarginDp)   // Enemy left is before platform right plus margin

                            // Check vertical proximity (if Ys are very similar, horizontal check is more important)
                            val tooCloseVerticallyToItsPlatform =
                                kotlin.math.abs(enemyY - newPlatformY) < PLAYER_HEIGHT_DP // Avoid spawning right on same Y level

                            if (tooCloseHorizontallyToItsPlatform && tooCloseVerticallyToItsPlatform) {
                                Log.d("JorisJump_Enemy_Avoid", "Attempt ${spawnAttempt + 1}: Enemy candidate too close to its anchor platform. Retrying X.")
                                attemptSpawn = false // Mark this attempt as failed, try again if loop continues
                            } else {
                                // Optional: Check against ALL recently generated/nearby platforms (more complex)
                                // For now, just checking against its "anchor" platform.
                                attemptSpawn = true
                                break // Found a suitable position
                            }
                        }


                        if (attemptSpawn) {
                            enemies = enemies + EnemyState(id = nextEnemyId++, x = enemyX, y = enemyY)
                            Log.d("JorisJump_Enemy_Spawned", "SPAWNED Enemy ${nextEnemyId - 1} at Xw:${"%.1f".format(enemyX)}, Yw:${"%.1f".format(enemyY)}")
                        } else {
                            Log.d("JorisJump_Enemy_Avoid", "Skipped enemy spawn after multiple attempts to find clear spot near platform.")
                        }
                    }
                }
                platforms = currentPlatformsMutable.toList()

                // Check if player fell off visible bottom (normal fall)
                if (playerYPositionWorldDp > totalScrollOffsetDp + screenHeightDp) {
                    if (!playerIsFallingOffScreen) {
                        playerIsFallingOffScreen = true
                        Log.d("JorisJump_Fall", "Player fell off visible, starting fall anim")
                    }
                }

            } else { // playerIsFallingOffScreen is true
                playerVelocityY += GRAVITY
                playerYPositionWorldDp += playerVelocityY
                if (playerYPositionWorldDp > totalScrollOffsetDp + screenHeightDp + PLAYER_HEIGHT_DP * 2.5f) {
                    gameRunning = false
                    Log.d("JorisJump_Fall", "Player fully off screen. GAME OVER.")
                }
            }
            delay(16)
        }
        Log.d("JorisJump_GameLoop", "Game loop ended. GameRunning: $gameRunning")
    }

    // --- UI DRAWING ---
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Image(painter = painterResource(id = R.drawable.background_doodle), contentDescription = "Game Background",
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)

        if (!playerAndScreenInitialized && this.maxWidth > 0.dp && this.maxHeight > 0.dp) {
            screenWidthDp = this.maxWidth.value; screenHeightDp = this.maxHeight.value
            performGameReset(); playerAndScreenInitialized = true
            Log.d("JorisJump_Init", "Screen Initialized: $screenWidthDp x $screenHeightDp")
        }

        if (playerAndScreenInitialized) {
            // Draw Platforms
            platforms.forEach { platform ->
                val visualCloudScaleFactor = 4f
                val currentPlatformLogicalWidthDp = PLATFORM_WIDTH_DP
                val currentPlatformLogicalHeightDp = PLATFORM_HEIGHT_DP
                val visualCloudWidthDp = currentPlatformLogicalWidthDp * visualCloudScaleFactor
                val visualCloudHeightDp = currentPlatformLogicalHeightDp * visualCloudScaleFactor
                val visualCloudOffsetX = (currentPlatformLogicalWidthDp - visualCloudWidthDp) / 2f
                val visualCloudOffsetY = (currentPlatformLogicalHeightDp - visualCloudHeightDp) / 2f

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
                    val springVisualWidth = currentPlatformLogicalWidthDp * SPRING_VISUAL_WIDTH_FACTOR
                    val springVisualHeight = currentPlatformLogicalHeightDp * SPRING_VISUAL_HEIGHT_FACTOR
                    val springX_onPlatform = platform.x + (currentPlatformLogicalWidthDp / 2f) - (springVisualWidth / 2f)
                    val springY_onPlatform = platform.y - springVisualHeight + (currentPlatformLogicalHeightDp * 0.2f)
                    Image(
                        painter = painterResource(id = R.drawable.spring_mushroom),
                        contentDescription = "Spring Mushroom",
                        modifier = Modifier
                            .absoluteOffset(x = springX_onPlatform.dp, y = (springY_onPlatform - totalScrollOffsetDp).dp)
                            .size(springVisualWidth.dp, springVisualHeight.dp)
                    )
                    if (DEBUG_SHOW_HITBOXES) { Box(modifier = Modifier.absoluteOffset(x = springX_onPlatform.dp, y = (springY_onPlatform - totalScrollOffsetDp).dp).size(springVisualWidth.dp, springVisualHeight.dp).background(Color.Cyan.copy(alpha = 0.4f))) }
                }
                if (DEBUG_SHOW_HITBOXES) { Box(modifier = Modifier.absoluteOffset(x = platform.x.dp, y = (platform.y - totalScrollOffsetDp).dp).size(PLATFORM_WIDTH_DP.dp, PLATFORM_HEIGHT_DP.dp).background(Color.Red.copy(alpha = 0.3f))) }
            }

            // Draw Enemies
            enemies.forEach { enemy ->
                Image(
                    painter = painterResource(id = R.drawable.saibaman_enemy),
                    contentDescription = "Saibaman Enemy",
                    modifier = Modifier
                        .absoluteOffset(
                            x = (enemy.x + enemy.visualOffsetX).dp,
                            y = ((enemy.y - totalScrollOffsetDp) + enemy.visualOffsetY).dp
                        )
                        .size(ENEMY_WIDTH_DP.dp, ENEMY_HEIGHT_DP.dp)
                )
                if (DEBUG_SHOW_HITBOXES) {
                    Box(
                        modifier = Modifier
                            .absoluteOffset(x = enemy.x.dp, y = (enemy.y - totalScrollOffsetDp).dp)
                            .size(ENEMY_WIDTH_DP.dp, ENEMY_HEIGHT_DP.dp)
                            .background(Color.Yellow.copy(alpha = 0.3f))
                    )
                }
            }

            // Draw Player
            Image(
                painter = painterResource(id = R.drawable.joris_doodler),
                contentDescription = "Joris the Doodler",
                modifier = Modifier
                    .absoluteOffset(x = playerXPositionScreenDp.dp, y = (playerYPositionWorldDp - totalScrollOffsetDp).dp)
                    .size(PLAYER_WIDTH_DP.dp, PLAYER_HEIGHT_DP.dp)
            )
            if (DEBUG_SHOW_HITBOXES) {
                Box(
                    modifier = Modifier
                        .absoluteOffset(x = playerXPositionScreenDp.dp, y = (playerYPositionWorldDp - totalScrollOffsetDp).dp)
                        .size(PLAYER_WIDTH_DP.dp, PLAYER_HEIGHT_DP.dp)
                        .background(Color.Blue.copy(alpha = 0.3f))
                )
            }

            // Score Text
            Text(
                text = "Score: $score",
                style = TextStyle(fontFamily = arcadeFontFamily, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFFDE7), shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), offset = Offset(2f, 3f), blurRadius = 3f)),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp)
            )

            // Game Over UI
            if (!gameRunning) {
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("GAME OVER", style = TextStyle(fontFamily = arcadeFontFamily, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFF44336), shadow = Shadow(color = Color.Black.copy(alpha = 0.7f), offset = Offset(3f, 4f), blurRadius = 5f)))
                    Text("Final Score: $score", style = TextStyle(fontFamily = arcadeFontFamily, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White, shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), offset = Offset(2f, 3f), blurRadius = 3f)))
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { performGameReset() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50), contentColor = Color.White),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Restart Game", style = TextStyle(fontFamily = arcadeFontFamily, fontSize = 20.sp, fontWeight = FontWeight.Bold)) }
                }
            }
        }

        // Debug Info Column
        Column(modifier = Modifier.align(Alignment.TopStart).padding(8.dp).background(Color.Black.copy(alpha = 0.3f))) {
            Text("PlayerX(scr): ${"%.1f".format(playerXPositionScreenDp)}|TltX: ${"%.2f".format(rawTiltXForDebug)}",color=Color.White,fontSize=10.sp)
            Text("PlayerY(w): ${"%.1f".format(playerYPositionWorldDp)}|P Y(s): ${"%.1f".format(playerYPositionWorldDp-totalScrollOffsetDp)}|Vy: ${"%.1f".format(playerVelocityY)}",color=Color.White,fontSize=10.sp)
            Text("ScrOff: ${"%.1f".format(totalScrollOffsetDp)}|LstScrY: ${"%.1f".format(lastScoredWorldY)}",color=Color.White,fontSize=10.sp)
            Text("Scrn: ${"%.0f".format(screenWidthDp)}x${"%.0f".format(screenHeightDp)}",color=Color.White,fontSize=10.sp)
            Text(if(gameRunning)(if(playerIsFallingOffScreen)"FALLING OFF" else "Running")else "GAME OVER",color=if(gameRunning)(if(playerIsFallingOffScreen)Color.Yellow else Color.Green)else Color.Red,fontSize=10.sp)
            Text("Pltfms: ${platforms.size}|NxtPID: $nextPlatformId|NxtEID: $nextEnemyId",color=Color.White,fontSize=10.sp)
            Text("Enemies: ${enemies.size}",color=Color.White,fontSize=10.sp)
            if(accelerometer==null)Text("ACCEL N/A!",color=Color.Red,fontSize=10.sp)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun JorisJumpScreenPreview() {
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) { // Main container for all preview elements

            // Background Image
            Image(
                painter = painterResource(id = R.drawable.background_doodle),
                contentDescription = "Preview Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Preview Doodler
            // Encapsulate Doodler in its own Box for alignment and offset
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter) // Align this Box
                    .offset(y = (-70).dp)          // Then offset it
                    .size(PLAYER_WIDTH_DP.dp, PLAYER_HEIGHT_DP.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.joris_doodler),
                    contentDescription = "Preview Doodler",
                    modifier = Modifier.fillMaxSize() // Image fills its parent Box
                )
            }

            // Preview Cloud Platform
            val previewCloudVisualFactor = 4f
            val pvlW = PLATFORM_WIDTH_DP
            val pvlH = PLATFORM_HEIGHT_DP
            Box( // Encapsulate Cloud in its own Box
                modifier = Modifier
                    .align(Alignment.BottomCenter) // Align this Box
                    // Offset for the cloud's visual center to be roughly where a logical platform might be
                    .offset(y = (-70 + PLAYER_HEIGHT_DP + 5 - (pvlH * (previewCloudVisualFactor - 1) / 2)).dp)
                    .size(pvlW.dp * previewCloudVisualFactor, pvlH.dp * previewCloudVisualFactor)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.cloud_platform),
                    contentDescription = "Preview Cloud",
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Preview Spring Mushroom
            // Positioned relative to where the cloud is, approximately
            val springPreviewYOffset = (-70 + PLAYER_HEIGHT_DP + 5 - (pvlH * (previewCloudVisualFactor - 1) / 2) - (pvlH * SPRING_VISUAL_HEIGHT_FACTOR * previewCloudVisualFactor * 0.7f)).dp
            val springPreviewXOffset = ((pvlW * previewCloudVisualFactor / 2f) - (pvlW * SPRING_VISUAL_WIDTH_FACTOR * previewCloudVisualFactor / 2f)).dp
            Box( // Encapsulate Spring in its own Box
                modifier = Modifier
                    .align(Alignment.BottomCenter) // Align this Box
                    .offset(x = springPreviewXOffset, y = springPreviewYOffset) // Then offset it
                    .size(
                        (PLATFORM_WIDTH_DP * SPRING_VISUAL_WIDTH_FACTOR * previewCloudVisualFactor).dp,
                        (PLATFORM_HEIGHT_DP * SPRING_VISUAL_HEIGHT_FACTOR * previewCloudVisualFactor).dp
                    )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.spring_mushroom),
                    contentDescription = "Preview Spring",
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Preview Enemy - THIS IS NOW A DIRECT CHILD OF THE MAIN BOX, or in its own positioning Box
            Image(
                painter = painterResource(id = R.drawable.saibaman_enemy),
                contentDescription = "Preview Enemy",
                modifier = Modifier
                    .align(Alignment.Center) // Example alignment
                    .offset(x = 50.dp, y = -50.dp) // Example offset
                    .size(ENEMY_WIDTH_DP.dp, ENEMY_HEIGHT_DP.dp)
            )

            // Score Text
            Text(
                "Score: 0",
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = arcadeFontFamily),
                color = Color.Black,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            )

            // Preview Helper Text
            Text(
                "Preview (No Dynamic Logic)",
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                color = Color.Black
            )
        }
    }
}