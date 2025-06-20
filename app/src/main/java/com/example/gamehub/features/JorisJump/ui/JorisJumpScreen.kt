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
import android.os.Vibrator // For basic vibration
import android.os.VibrationEffect // For more control on newer APIs
import android.os.Build // To check API level for VibrationEffect
import com.example.gamehub.features.JorisJump.model.PlatformState
import com.example.gamehub.features.JorisJump.model.EnemyState
import com.example.gamehub.features.JorisJump.model.*
import com.example.gamehub.features.JorisJump.model.PlayerState
import com.example.gamehub.features.JorisJump.logic.generateInitialPlatformsList
import com.example.gamehub.features.JorisJump.logic.movePlatforms
import com.example.gamehub.features.JorisJump.logic.cleanupPlatforms
import com.example.gamehub.features.JorisJump.logic.checkPlayerPlatformCollision
import com.example.gamehub.features.JorisJump.logic.moveEnemies
import com.example.gamehub.features.JorisJump.logic.cleanupEnemies
import com.example.gamehub.features.JorisJump.logic.checkPlayerEnemyCollision
import com.example.gamehub.features.JorisJump.logic.generatePlatformsAndEnemies
import com.example.gamehub.features.JorisJump.ui.components.PlayerSprite
import com.example.gamehub.features.JorisJump.ui.components.PlatformSprite
import com.example.gamehub.features.JorisJump.ui.components.EnemySprite
import com.example.gamehub.features.JorisJump.ui.components.GameOverDialog

private val pixelFontFamily = FontFamily(
    Font(R.font.arcade_classic, FontWeight.Normal)
)

@Composable
fun JorisJumpScreen() {
    val context = LocalContext.current
    val view = LocalView.current

    // --- Haptic Feedback (Vibration) Setup ---
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // For Android 12 (API 31) and above, VibratorManager is preferred
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
            vibratorManager.defaultVibrator
        } else {
            // For older APIs, get Vibrator directly
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    fun triggerVibration(milliseconds: Long, amplitude: Int = VibrationEffect.DEFAULT_AMPLITUDE) {
        if (vibrator.hasVibrator()) { // Check if the device has a vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // For Android Oreo (API 26) and above, use VibrationEffect
                // Amplitude is from 1 to 255. DEFAULT_AMPLITUDE is -1, which uses system default.
                val effectAmplitude = if (amplitude == VibrationEffect.DEFAULT_AMPLITUDE || (amplitude in 1..255)) {
                    amplitude
                } else {
                    VibrationEffect.DEFAULT_AMPLITUDE // Fallback if invalid amplitude provided
                }
                vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, effectAmplitude))
            } else {
                // For older APIs
                @Suppress("DEPRECATION")
                vibrator.vibrate(milliseconds)
            }
        } else {
            Log.w("JorisJump_Vibration", "Device does not have a vibrator.")
        }
    }

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

    var playerState by remember { mutableStateOf(PlayerState()) }
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
        playerState.xScreenDp = (screenWidthDp / 2) - (PLAYER_WIDTH_DP / 2)
        playerState.yWorldDp = screenHeightDp - PLAYER_HEIGHT_DP - PLATFORM_HEIGHT_DP - 30f
        playerState.velocityY = 0f
        playerState.isFallingOffScreen = false
        val (initialPlatformsList, updatedNextId) = generateInitialPlatformsList(screenWidthDp, screenHeightDp, INITIAL_PLATFORM_COUNT, 0)
        platforms = initialPlatformsList
        nextPlatformId = updatedNextId
        enemies = emptyList()
        nextEnemyId = 0
        score = 0
        lastScoredWorldY = playerState.yWorldDp
        gameRunning = true
    }

    DisposableEffect(accelerometer, playerAndScreenInitialized) { // Sensor Input
        if (accelerometer == null) { Log.e("JorisJump_Sensor", "Accelerometer N/A!"); return@DisposableEffect onDispose {} }
        if (!playerAndScreenInitialized) { Log.d("JorisJump_Sensor", "Sensor Waiting for init."); return@DisposableEffect onDispose {} }
        Log.d("JorisJump_Sensor", "Registering sensor listener.")
        val eventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (!gameRunning || playerState.isFallingOffScreen) return
                event?.let {
                    if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        val tiltX = it.values[0]; rawTiltXForDebug = tiltX
                        playerState.xScreenDp -= tiltX * ACCELEROMETER_SENSITIVITY
                        if ((playerState.xScreenDp + PLAYER_WIDTH_DP) < 0) playerState.xScreenDp = screenWidthDp
                        else if (playerState.xScreenDp > screenWidthDp) playerState.xScreenDp = 0f - PLAYER_WIDTH_DP
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

            if (!playerState.isFallingOffScreen) {
                // Platform Movement
                platforms = movePlatforms(platforms, screenWidthDp)

                // Player Physics
                playerState.velocityY += GRAVITY
                playerState.yWorldDp += playerState.velocityY

                // Score Update
                val yForNextPoint = lastScoredWorldY - (1.0f / SCORE_POINTS_PER_DP_WORLD_Y)
                if (playerState.yWorldDp < yForNextPoint) {
                    val pointsEarned = ((lastScoredWorldY - playerState.yWorldDp) * SCORE_POINTS_PER_DP_WORLD_Y).toInt()
                    if (pointsEarned > 0) { score += pointsEarned; lastScoredWorldY -= pointsEarned * (1.0f / SCORE_POINTS_PER_DP_WORLD_Y) }
                }

                // Collision with Platforms
                val playerBottomWorldDp = playerState.yWorldDp + PLAYER_HEIGHT_DP
                val playerRightScreenDp = playerState.xScreenDp + PLAYER_WIDTH_DP
                var didLandAndJumpThisFrame = false
                if (playerState.velocityY > 0) {
                    platforms.forEach { platform ->
                        if (didLandAndJumpThisFrame) return@forEach
                        val xOverlaps = playerRightScreenDp > platform.x && playerState.xScreenDp < (platform.x + PLATFORM_WIDTH_DP)
                        if (xOverlaps) {
                            val previousPlayerBottomWorldDp = playerBottomWorldDp - playerState.velocityY
                            if (previousPlayerBottomWorldDp <= platform.y && playerBottomWorldDp >= platform.y) {
                                playerState.yWorldDp = platform.y - PLAYER_HEIGHT_DP
                                if (platform.hasSpring) {
                                    playerState.velocityY = INITIAL_JUMP_VELOCITY * platform.springJumpFactor
                                    playSound(springSoundPlayer)
                                } else {
                                    playerState.velocityY = INITIAL_JUMP_VELOCITY
                                    playSound(jumpSoundPlayer)
                                }
                                didLandAndJumpThisFrame = true
                            }
                        }
                    }
                }

                // Camera Scrolling
                val playerYOnScreen = playerState.yWorldDp - totalScrollOffsetDp
                val scrollThresholdActualScreenY = screenHeightDp * SCROLL_THRESHOLD_ON_SCREEN_Y_FACTOR
                if (playerYOnScreen < scrollThresholdActualScreenY) {
                    val scrollAmount = scrollThresholdActualScreenY - playerYOnScreen
                    totalScrollOffsetDp -= scrollAmount
                }

                // Enemy Twitching (Applied to all existing enemies)
                enemies = moveEnemies(enemies, gameTimeMillis)
                // Enemy Removal
                enemies = cleanupEnemies(enemies, totalScrollOffsetDp, screenHeightDp)

                // Collision With Enemies
                val playerTopOnScreen = playerState.yWorldDp - totalScrollOffsetDp
                val playerRectForEnemyCollision = RectF(
                    playerState.xScreenDp, playerTopOnScreen,
                    playerState.xScreenDp + PLAYER_WIDTH_DP, playerTopOnScreen + PLAYER_HEIGHT_DP
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
                        playerState.isFallingOffScreen = true // Initiate fall-off sequence
                        hitEnemyThisFrame = true
                        triggerVibration(200L, VibrationEffect.EFFECT_HEAVY_CLICK)
                        Log.d("JorisJump_Enemy", "OUCH! Hit Enemy ${enemy.id}! Game Over.")
                    }
                }

                // Platform Generation (and integrated Enemy Generation)
                val genResult = generatePlatformsAndEnemies(
                    platforms,
                    enemies,
                    nextPlatformId,
                    nextEnemyId,
                    totalScrollOffsetDp,
                    screenHeightDp,
                    screenWidthDp
                )
                platforms = genResult.platforms
                enemies = genResult.enemies
                nextPlatformId = genResult.nextPlatformId
                nextEnemyId = genResult.nextEnemyId

                // Check if player fell off visible bottom (normal fall)
                if (playerState.yWorldDp > totalScrollOffsetDp + screenHeightDp) {
                    if (!playerState.isFallingOffScreen) {
                        playerState.isFallingOffScreen = true
                        Log.d("JorisJump_Fall", "Player fell off visible, starting fall anim")
                    }
                }

            } else { // playerState.isFallingOffScreen is true
                playerState.velocityY += GRAVITY
                playerState.yWorldDp += playerState.velocityY
                if (playerState.yWorldDp > totalScrollOffsetDp + screenHeightDp + PLAYER_HEIGHT_DP * 2.5f) {
                    gameRunning = false
                    triggerVibration(350L, VibrationEffect.EFFECT_HEAVY_CLICK)
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
                PlatformSprite(platform = platform, totalScrollOffsetDp = totalScrollOffsetDp, showHitbox = DEBUG_SHOW_HITBOXES)
            }

            enemies.forEach { enemy ->
                EnemySprite(enemy = enemy, totalScrollOffsetDp = totalScrollOffsetDp, showHitbox = DEBUG_SHOW_HITBOXES)
            }

            PlayerSprite(
                x = playerState.xScreenDp,
                y = playerState.yWorldDp - totalScrollOffsetDp
            )

            Text(
                text = "Score: $score",
                style = TextStyle(
                    fontFamily = pixelFontFamily,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFFDE7),
                    shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), offset = Offset(2f, 3f), blurRadius = 3f)
                ),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp)
            )

            if (!gameRunning) {
                GameOverDialog(score = score, onRestart = { performGameReset() })
            }
        }
    }
}