package com.example.gamehub.features.screamosaur.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer // Added import
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType // Added import
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback // Added import
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.gamehub.R // Added import for resources
import kotlinx.coroutines.*
import kotlin.math.absoluteValue
import kotlin.random.Random

@Composable
fun ScreamosaurScreen() {
    val context = LocalContext.current
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var showPermissionDialog by remember { mutableStateOf(!hasAudioPermission) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasAudioPermission = isGranted
            showPermissionDialog = false
        }
    )

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🦖 Scream-O-Saur",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(8.dp))

            if (hasAudioPermission) {
                GameContent()
            } else {
                PermissionRequest(
                    onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                )
            }
        }
    }

    if (showPermissionDialog && !hasAudioPermission) { // Only show if permission not granted
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Microphone Access Needed") },
            text = { Text("This game needs microphone access to hear your roar and make the dinosaur jump!") },
            confirmButton = {
                Button(
                    onClick = {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                ) {
                    Text("Grant Access")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPermissionDialog = false }
                ) {
                    Text("Not Now")
                }
            }
        )
    }
}

// Game state
enum class GameState { READY, PLAYING, PAUSED, GAME_OVER }

// Obstacle data class
data class Obstacle(
    var xPosition: Float, // Pixel value
    val height: Float,    // Pixel value
    val width: Float,     // Pixel value
    var passed: Boolean = false
)

private const val MIN_AMPLITUDE_THRESHOLD = 1800 // Sounds below this won't show on meter
private const val JUMP_AMPLITUDE_THRESHOLD = 4500 // Sounds above this trigger a jump

@Composable
private fun GameContent() {
    var gameState by remember { mutableStateOf(GameState.READY) }
    var score by remember { mutableStateOf(0) }
    var obstacles by remember { mutableStateOf(listOf<Obstacle>()) }
    var gameSpeed by remember { mutableStateOf(5f) } // Pixels per frame
    var gameStartTime by remember { mutableStateOf(0L) }
    var timeAtPause by remember { mutableStateOf(0L) }
    var initialDelayHasOccurred by remember { mutableStateOf(false) }


    val jumpAnim = remember { Animatable(0f) }
    var isJumping by remember { mutableStateOf(false) }

    // Dp values for layout modifiers
    val dinosaurSizeDp = 60.dp
    val gameHeightDp = 300.dp
    val groundHeightDp = 20.dp
    val dinosaurVisualXOffsetDp = 60.dp
    val jumpMagnitudeDp = 150.dp

    val density = LocalDensity.current

    // Pixel values for game logic and drawing
    val gameHeightPx = with(density) { gameHeightDp.toPx() }
    val groundHeightPx = with(density) { groundHeightDp.toPx() }
    val dinosaurSizePx = with(density) { dinosaurSizeDp.toPx() }
    val dinosaurVisualXPositionPx = with(density) { dinosaurVisualXOffsetDp.toPx() }
    val jumpMagnitudePx = with(density) { jumpMagnitudeDp.toPx() }

    val dinoTopYOnGroundPx = gameHeightPx - groundHeightPx - dinosaurSizePx

    val runningAnimState = remember { mutableStateOf(0) }
    val isRunningAnimating = remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current // Added for haptic feedback

    // MediaPlayer for jump sound
    val jumpSoundPlayer = remember {
        MediaPlayer.create(context, R.raw.dino_jump_sound).apply {
            setVolume(0.5f, 0.5f) // Adjust volume as needed
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            jumpSoundPlayer.release()
        }
    }

    fun playJumpSound() {
        if (jumpSoundPlayer.isPlaying) {
            jumpSoundPlayer.seekTo(0)
        } else {
            jumpSoundPlayer.start()
        }
    }

    var currentAmplitude by remember { mutableStateOf(0) }

    LaunchedEffect(gameState) {
        if (gameState == GameState.PLAYING) {
            isRunningAnimating.value = true
            while (isRunningAnimating.value && gameState == GameState.PLAYING && isActive) {
                runningAnimState.value = (runningAnimState.value + 1) % 4
                delay(100)
            }
        } else {
            isRunningAnimating.value = false
        }
    }

    LaunchedEffect(gameState) {
        if (gameState == GameState.PLAYING) {
            if (!initialDelayHasOccurred) {
                delay(1500) // Initial safe period before obstacles appear
                gameStartTime = System.currentTimeMillis() // Set/reset gameStartTime after the initial delay
                initialDelayHasOccurred = true
            }

            withContext(Dispatchers.Default) {
                while (gameState == GameState.PLAYING && isActive) {
                    val currentTime = System.currentTimeMillis()
                    val elapsedGameTime = currentTime - gameStartTime

                    obstacles = obstacles.map {
                        it.copy(xPosition = it.xPosition - gameSpeed)
                    }.filter { it.xPosition + it.width > 0 }

                    val canvasWidthPx = gameHeightPx * (16f/9f) // Assuming 16:9 aspect for game area if width is not fixed
                    val spawnNewObstacleTriggerX = canvasWidthPx * 0.5f
                    val newObstacleBaseX = canvasWidthPx + Random.nextFloat() * (canvasWidthPx * 0.2f)
                    val firstObstacleX = canvasWidthPx + Random.nextFloat() * (canvasWidthPx * 0.2f)


                    val obstacleMinHeightPx = with(density) { 25.dp.toPx()}
                    val obstacleMaxHeightPx = with(density) { 55.dp.toPx()}
                    val obstacleMinWidthPx = with(density) { 15.dp.toPx()}
                    val obstacleMaxWidthPx = with(density) { 30.dp.toPx()}

                    if (obstacles.isEmpty() && elapsedGameTime > 0) { // Ensure some time passed after initial delay
                        val newObstacle = Obstacle(
                            xPosition = firstObstacleX,
                            height = Random.nextFloat() * (obstacleMaxHeightPx - obstacleMinHeightPx) + obstacleMinHeightPx,
                            width = Random.nextFloat() * (obstacleMaxWidthPx - obstacleMinWidthPx) + obstacleMinWidthPx
                        )
                        obstacles = listOf(newObstacle)
                    } else if (obstacles.isNotEmpty() && obstacles.last().xPosition < spawnNewObstacleTriggerX && obstacles.size < 5) {
                        val newObstacle = Obstacle(
                            xPosition = newObstacleBaseX,
                            height = Random.nextFloat() * (obstacleMaxHeightPx - obstacleMinHeightPx) + obstacleMinHeightPx,
                            width = Random.nextFloat() * (obstacleMaxWidthPx - obstacleMinWidthPx) + obstacleMinWidthPx
                        )
                        obstacles = obstacles + newObstacle
                    }

                    obstacles.forEach { obstacle ->
                        val obstacleRightEdgePx = obstacle.xPosition + obstacle.width
                        if (!obstacle.passed && obstacleRightEdgePx < dinosaurVisualXPositionPx) {
                            obstacle.passed = true
                            score++
                            // Increase game speed more significantly with score
                            gameSpeed = (5f + (score / 5f)).coerceAtMost(20f) // Faster increase, max speed 20f
                        }
                    }

                    // Collision check delay is now relative to elapsedGameTime after initial 1.5s delay
                    if (elapsedGameTime > 2000) { // Start collision checks a bit after obstacles can appear
                        val currentDinoTopYPx = dinoTopYOnGroundPx - (jumpAnim.value * jumpMagnitudePx)
                        val dinosaurVisualRect = Rect(
                            left = dinosaurVisualXPositionPx,
                            top = currentDinoTopYPx,
                            right = dinosaurVisualXPositionPx + dinosaurSizePx,
                            bottom = currentDinoTopYPx + dinosaurSizePx
                        )

                        // Fine-tuned hitbox
                        val horizontalHitboxReductionPx = dinosaurSizePx * 0.20f // Reduce 20% from each side
                        val verticalHitboxReductionPx = dinosaurSizePx * 0.20f   // Reduce 20% from top/bottom

                        val dinosaurHitbox = Rect(
                            left = dinosaurVisualRect.left + horizontalHitboxReductionPx,
                            top = dinosaurVisualRect.top + verticalHitboxReductionPx,
                            right = dinosaurVisualRect.right - horizontalHitboxReductionPx,
                            bottom = dinosaurVisualRect.bottom - (verticalHitboxReductionPx * 0.3f) // Less reduction from bottom for feet
                        )


                        val collision = obstacles.any { obstacle ->
                            val obstacleRect = Rect(
                                left = obstacle.xPosition,
                                top = gameHeightPx - groundHeightPx - obstacle.height,
                                right = obstacle.xPosition + obstacle.width,
                                bottom = gameHeightPx - groundHeightPx
                            )
                            dinosaurHitbox.overlaps(obstacleRect)
                        }

                        if (collision) {
                            withContext(Dispatchers.Main) {
                                isRunningAnimating.value = false
                                gameState = GameState.GAME_OVER
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress) // Strong vibration on game over
                            }
                            break // Exit game loop
                        }
                    }
                    delay(16) // ~60fps
                }
            }
        }
    }


    LaunchedEffect(gameState) {
        if (gameState == GameState.PLAYING) {
            val bufferSize = AudioRecord.getMinBufferSize(
                8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
            if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                withContext(Dispatchers.Main) { gameState = GameState.READY }
                return@LaunchedEffect
            }

            var audioRecord: AudioRecord? = null
            try {
                if (bufferSize == AudioRecord.ERROR_BAD_VALUE || bufferSize == AudioRecord.ERROR) {
                    withContext(Dispatchers.Main) { gameState = GameState.READY }
                    return@LaunchedEffect
                }

                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC, 8000,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize
                )

                if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                    audioRecord.release()
                    withContext(Dispatchers.Main) { gameState = GameState.READY }
                    return@LaunchedEffect
                }

                val buffer = ShortArray(bufferSize)
                audioRecord.startRecording()

                try {
                    while (gameState == GameState.PLAYING && isActive) {
                        val read = audioRecord.read(buffer, 0, buffer.size)
                        if (read > 0) {
                            val maxAmplitudeRaw = buffer.take(read).maxOfOrNull { it.toInt().absoluteValue } ?: 0
                            currentAmplitude = if (maxAmplitudeRaw >= MIN_AMPLITUDE_THRESHOLD) {
                                maxAmplitudeRaw
                            } else {
                                0
                            }

                            if (maxAmplitudeRaw > JUMP_AMPLITUDE_THRESHOLD && !isJumping) {
                                isJumping = true
                                playJumpSound() // Play jump sound
                                coroutineScope.launch {
                                    jumpAnim.snapTo(0f)
                                    jumpAnim.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing)
                                    )
                                    jumpAnim.animateTo(
                                        targetValue = 0f,
                                        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
                                    )
                                    isJumping = false
                                }
                            }
                        }
                        delay(50)
                    }
                } finally {
                    if (audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        audioRecord.stop()
                    }
                    audioRecord.release()
                }
            } catch (e: SecurityException) {
                audioRecord?.release()
                withContext(Dispatchers.Main) { gameState = GameState.READY }
            } catch (e: IllegalStateException) {
                audioRecord?.release()
                withContext(Dispatchers.Main) { gameState = GameState.READY }
            }
        } else {
            currentAmplitude = 0
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Score: $score",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(gameHeightDp)
                .background(Color(0xFFE0F7FA)) // Light blue background for the game (sky)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Draw Sun
                val sunRadius = 25.dp.toPx()
                val sunCenter = Offset(size.width * 0.15f, size.height * 0.15f) // Top-left
                drawCircle(
                    color = Color(0xFFFFD54F), // Lighter Yellow
                    radius = sunRadius,
                    center = sunCenter
                )
                // Sun rays
                val numRays = 8
                for (i in 0 until numRays) {
                    val angle = Math.toRadians((i * (360.0 / numRays)) + 15.0) // Offset angle
                    val rayStartOffset = sunRadius * 1.1f
                    val rayEndOffset = sunRadius * 1.6f
                    drawLine(
                        color = Color(0xFFFFB74D).copy(alpha = 0.7f), // Lighter Orange, semi-transparent
                        start = Offset(
                            sunCenter.x + kotlin.math.cos(angle).toFloat() * rayStartOffset,
                            sunCenter.y + kotlin.math.sin(angle).toFloat() * rayStartOffset
                        ),
                        end = Offset(
                            sunCenter.x + kotlin.math.cos(angle).toFloat() * rayEndOffset,
                            sunCenter.y + kotlin.math.sin(angle).toFloat() * rayEndOffset
                        ),
                        strokeWidth = 2.dp.toPx()
                    )
                }

                // Draw ground
                val groundBaseColor = Color(0xFFBCAAA4) // A sandy brown
                val groundDarkerColor = Color(0xFF8D6E63) // Darker shade for texture
                val groundTopY = size.height - groundHeightPx
                drawRect(
                    color = groundBaseColor,
                    topLeft = Offset(0f, groundTopY),
                    size = Size(size.width, groundHeightPx)
                )
                // Add some texture to the ground (horizontal dashes)
                val numGroundPatches = 30
                for (i in 0..numGroundPatches) {
                    val patchY = groundTopY + (Random.nextFloat() * (groundHeightPx - 4.dp.toPx())) + 2.dp.toPx()
                    val patchX = Random.nextFloat() * size.width
                    val patchWidth = Random.nextFloat() * 20.dp.toPx() + 10.dp.toPx()
                    val patchHeight = Random.nextFloat() * 2.dp.toPx() + 1.dp.toPx()
                    drawRect(
                        color = groundDarkerColor.copy(alpha = Random.nextFloat() * 0.3f + 0.2f), // Vary alpha
                        topLeft = Offset(patchX, patchY),
                        size = Size(patchWidth, patchHeight)
                    )
                }

                // Draw obstacles (Cacti)
                obstacles.forEach { obstacle ->
                    val cactusColor = Color(0xFF4CAF50) // Brighter green for cactus body
                    val cactusDarkerColor = Color(0xFF388E3C) // Darker green for details

                    // Obstacle properties (hitbox)
                    val obX = obstacle.xPosition
                    val obY = gameHeightPx - groundHeightPx - obstacle.height
                    val obW = obstacle.width
                    val obH = obstacle.height

                    // Main Body of the cactus (centered within obstacle's width)
                    val bodyW = obW * 0.4f // Cactus body is 40% of the hitbox width
                    val bodyH = obH        // Cactus body is full height of the hitbox
                    val bodyX = obX + (obW - bodyW) / 2 // Center the body horizontally
                    val bodyBaseY = obY

                    drawRoundRect(
                        color = cactusColor,
                        topLeft = Offset(bodyX, bodyBaseY),
                        size = Size(bodyW, bodyH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(bodyW * 0.25f)
                    )

                    // Cactus Arms
                    val armBaseHeight = bodyH * 0.4f // General height for arm segments
                    val armBaseWidth = bodyW * 0.8f  // General width for arm segments

                    // Right Arm (if cactus is tall enough)
                    if (obH > 30.dp.toPx()) {
                        val armAttachY = bodyBaseY + bodyH * 0.2f // Attach point on body
                        val horizontalSegmentW = armBaseWidth * 0.5f
                        val horizontalSegmentH = armBaseHeight * 0.3f
                        val verticalSegmentW = armBaseWidth * 0.3f
                        val verticalSegmentH = armBaseHeight * 0.8f

                        // Horizontal part of right arm
                        val rArmHorizX = bodyX + bodyW - horizontalSegmentW * 0.2f // Start slightly inside body
                        drawRoundRect(
                            color = cactusColor,
                            topLeft = Offset(rArmHorizX, armAttachY),
                            size = Size(horizontalSegmentW, horizontalSegmentH),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(horizontalSegmentH * 0.5f)
                        )
                        // Vertical part of right arm (pointing up)
                        val rArmVertX = rArmHorizX + horizontalSegmentW - verticalSegmentW * 0.5f // Centered on end of horizontal
                        val rArmVertY = armAttachY - verticalSegmentH + horizontalSegmentH // Positioned above horizontal
                        drawRoundRect(
                            color = cactusColor,
                            topLeft = Offset(rArmVertX, rArmVertY),
                            size = Size(verticalSegmentW, verticalSegmentH),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(verticalSegmentW * 0.5f)
                        )
                    }

                    // Left Arm (if cactus is taller and wider)
                    if (obH > 40.dp.toPx() && obW > 20.dp.toPx()) {
                        val armAttachY = bodyBaseY + bodyH * 0.45f // Different attach point
                        val horizontalSegmentW = armBaseWidth * 0.5f
                        val horizontalSegmentH = armBaseHeight * 0.3f
                        val verticalSegmentW = armBaseWidth * 0.3f
                        val verticalSegmentH = armBaseHeight * 0.8f

                        // Horizontal part of left arm
                        val lArmHorizX = bodyX - horizontalSegmentW + horizontalSegmentW * 0.2f // Start from left, slightly inside
                        drawRoundRect(
                            color = cactusColor,
                            topLeft = Offset(lArmHorizX, armAttachY),
                            size = Size(horizontalSegmentW, horizontalSegmentH),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(horizontalSegmentH * 0.5f)
                        )
                        // Vertical part of left arm (pointing up)
                        val lArmVertX = lArmHorizX + horizontalSegmentW * 0.5f - verticalSegmentW * 0.5f // Centered on start of horizontal
                        val lArmVertY = armAttachY - verticalSegmentH + horizontalSegmentH // Positioned above horizontal
                        drawRoundRect(
                            color = cactusColor,
                            topLeft = Offset(lArmVertX, lArmVertY),
                            size = Size(verticalSegmentW, verticalSegmentH),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(verticalSegmentW * 0.5f)
                        )
                    }

                    // Texture on main body (vertical lines)
                    val numLines = (bodyW / 6.dp.toPx()).toInt().coerceIn(2, 5)
                    if (numLines > 1) {
                        for (i in 1 until numLines) {
                            val lineX = bodyX + (bodyW / numLines) * i
                            drawLine(
                                color = cactusDarkerColor,
                                start = Offset(lineX, bodyBaseY + bodyH * 0.05f),
                                end = Offset(lineX, bodyBaseY + bodyH * 0.95f),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    }
                }
            }

            // Dinosaur (drawing logic unchanged)
            val currentDinoTopYDp = with(density) { (dinoTopYOnGroundPx - (jumpAnim.value * jumpMagnitudePx)).toDp() }
            Box(
                modifier = Modifier
                    .size(dinosaurSizeDp)
                    .offset(x = dinosaurVisualXOffsetDp, y = currentDinoTopYDp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Body
                    drawRoundRect(
                        color = Color(0xFFF44336), // Red
                        topLeft = Offset(size.width * 0.15f, size.height * 0.2f),
                        size = Size(size.width * 0.7f, size.height * 0.6f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.1f)
                    )
                    // Head
                    drawRoundRect(
                        color = Color(0xFFF44336), // Red
                        topLeft = Offset(size.width * 0.55f, size.height * 0.05f),
                        size = Size(size.width * 0.4f, size.height * 0.4f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.1f)
                    )
                    // Eye
                    drawCircle(
                        color = Color.White,
                        center = Offset(size.width * 0.75f, size.height * 0.25f),
                        radius = size.width * 0.1f
                    )
                    drawCircle(
                        color = Color.Black,
                        center = Offset(size.width * 0.78f, size.height * 0.27f),
                        radius = size.width * 0.05f
                    )
                    // Legs
                    val legWidth = size.width * 0.15f
                    val legHeight = size.height * 0.35f
                    val legYPos = size.height * 0.75f

                    val legOffsetFactor = when {
                        isJumping -> 0.15f
                        else -> when (runningAnimState.value) {
                            0 -> 0.0f
                            1 -> 0.05f
                            2 -> 0.1f
                            else -> 0.05f
                        }
                    }
                    // Back Leg
                    drawRoundRect(
                        color = Color(0xFFD32F2F), // Darker Red
                        topLeft = Offset(size.width * (0.3f - legOffsetFactor), legYPos),
                        size = Size(legWidth, legHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(legWidth * 0.3f)
                    )
                    // Front Leg
                    drawRoundRect(
                        color = Color(0xFFD32F2F), // Darker Red
                        topLeft = Offset(size.width * (0.55f + legOffsetFactor), legYPos),
                        size = Size(legWidth, legHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(legWidth * 0.3f)
                    )
                    // Tail
                    drawRoundRect(
                        color = Color(0xFFF44336), // Red
                        topLeft = Offset(size.width * 0.0f, size.height * 0.4f),
                        size = Size(size.width * 0.3f, size.height * 0.2f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.05f)
                    )
                }
            }

            // Game State Overlays (logic unchanged)
            when (gameState) {
                GameState.READY -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("ROAR to make the dinosaur jump!", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            coroutineScope.launch { jumpAnim.snapTo(0f) }
                            isJumping = false
                            score = 0
                            obstacles = emptyList()
                            gameSpeed = 5f
                            initialDelayHasOccurred = false
                            gameStartTime = System.currentTimeMillis()
                            gameState = GameState.PLAYING
                        }) { Text("Start Game") }
                    }
                }
                GameState.GAME_OVER -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Game Over!", fontSize = 24.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                        Text("Score: $score", fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch { jumpAnim.snapTo(0f) }
                                isJumping = false
                                score = 0
                                obstacles = emptyList()
                                gameSpeed = 5f
                                initialDelayHasOccurred = false
                                gameStartTime = System.currentTimeMillis()
                                gameState = GameState.PLAYING
                            }
                        ) { Text("Play Again") }
                    }
                }
                GameState.PAUSED -> {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("PAUSED", fontSize = 30.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                GameState.PLAYING -> { /* Playing state - no overlay */ }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        SoundMeter(amplitude = currentAmplitude)
        Spacer(modifier = Modifier.height(24.dp))

        if (gameState == GameState.PLAYING || gameState == GameState.PAUSED) {
            Button(
                onClick = {
                    if (gameState == GameState.PLAYING) {
                        timeAtPause = System.currentTimeMillis()
                        gameState = GameState.PAUSED
                    } else {
                        val pausedDuration = System.currentTimeMillis() - timeAtPause
                        gameStartTime += pausedDuration
                        gameState = GameState.PLAYING
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(if (gameState == GameState.PLAYING) "Pause Game" else "Resume Game")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SoundMeter(
    modifier: Modifier = Modifier,
    amplitude: Int,
    maxMeterAmplitude: Int = 7500
) {
    val displayAmplitude = amplitude.coerceAtMost(maxMeterAmplitude)
    val normalizedAmplitude = (displayAmplitude.toFloat() / maxMeterAmplitude).coerceIn(0f, 1f)

    val barColor = when {
        amplitude == 0 -> MaterialTheme.colorScheme.surfaceVariant
        normalizedAmplitude < 0.4f -> Color(0xFF4CAF50) // Green
        normalizedAmplitude < 0.75f -> Color(0xFFFFEB3B) // Yellow
        else -> Color(0xFFF44336) // Red
    }
    val meterHeight = 20.dp

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Sound Level",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .height(meterHeight)
                .fillMaxWidth(0.8f)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(meterHeight / 2)
                )
                .clip(RoundedCornerShape(meterHeight / 2))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    RoundedCornerShape(meterHeight / 2)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(normalizedAmplitude)
                    .background(barColor)
            )
        }
    }
}


@Composable
private fun PermissionRequest(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "This game needs microphone access to hear your roar!",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Button(onClick = onRequestPermission) {
            Text("Grant Microphone Access")
        }
    }
}