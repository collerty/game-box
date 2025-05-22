package com.example.gamehub.features.screamosaur.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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

private const val MIN_AMPLITUDE_THRESHOLD = 800 // Sounds below this won't show on meter
private const val JUMP_AMPLITUDE_THRESHOLD = 2500 // Sounds above this trigger a jump

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
                // Handle missing permission if it was revoked mid-game (unlikely but good practice)
                withContext(Dispatchers.Main) { gameState = GameState.READY } // Or PAUSED if coming from there
                return@LaunchedEffect
            }

            var audioRecord: AudioRecord? = null
            try {
                // Validate bufferSize
                if (bufferSize == AudioRecord.ERROR_BAD_VALUE || bufferSize == AudioRecord.ERROR) {
                    // Handle error, perhaps by setting a default or logging
                    withContext(Dispatchers.Main) { gameState = GameState.READY }
                    return@LaunchedEffect
                }

                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC, 8000,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize
                )

                // Check if AudioRecord initialized properly
                if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                    audioRecord.release() // Release if not initialized
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

                            // Apply threshold for sound meter display
                            currentAmplitude = if (maxAmplitudeRaw >= MIN_AMPLITUDE_THRESHOLD) {
                                maxAmplitudeRaw
                            } else {
                                0
                            }

                            if (maxAmplitudeRaw > JUMP_AMPLITUDE_THRESHOLD && !isJumping) {
                                isJumping = true
                                coroutineScope.launch {
                                    jumpAnim.snapTo(0f) // Ensure jump starts from ground
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
                        delay(50) // Audio processing interval
                    }
                } finally {
                    // Ensure AudioRecord is stopped and released
                    if (audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        audioRecord.stop()
                    }
                    audioRecord.release()
                }
            } catch (e: SecurityException) {
                // Handle SecurityException (e.g., permission denied during recording)
                audioRecord?.release() // Attempt to release if not null
                withContext(Dispatchers.Main) { gameState = GameState.READY }
            } catch (e: IllegalStateException) {
                // Handle IllegalStateException (e.g., AudioRecord not properly initialized or used)
                audioRecord?.release()
                withContext(Dispatchers.Main) { gameState = GameState.READY }
            }
        } else {
            currentAmplitude = 0 // Reset amplitude if not playing
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
                .background(Color(0xFFE0F7FA)) // Light blue background for the game
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Draw ground
                drawRect(
                    color = Color(0xFF795548), // Brown color for ground
                    topLeft = Offset(0f, size.height - groundHeightPx),
                    size = Size(size.width, groundHeightPx)
                )
                // Draw obstacles
                obstacles.forEach { obstacle ->
                    drawRect(
                        color = Color(0xFF4CAF50), // Green obstacles
                        topLeft = Offset(
                            obstacle.xPosition,
                            // Y position is from top, so subtract ground and obstacle height
                            gameHeightPx - groundHeightPx - obstacle.height
                        ),
                        size = Size(obstacle.width, obstacle.height)
                    )
                }
            }

            // Dinosaur
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
                        center = Offset(size.width * 0.78f, size.height * 0.27f), // Slightly offset for pupil
                        radius = size.width * 0.05f
                    )
                    // Legs
                    val legWidth = size.width * 0.15f
                    val legHeight = size.height * 0.35f
                    val legYPos = size.height * 0.75f // Position legs towards the bottom

                    val legOffsetFactor = when {
                        isJumping -> 0.15f // Legs splayed during jump
                        else -> when (runningAnimState.value) { // Running animation
                            0 -> 0.0f    // Both legs centered
                            1 -> 0.05f   // One leg forward
                            2 -> 0.1f    // Other leg forward
                            else -> 0.05f// Cycle back
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
                        topLeft = Offset(size.width * 0.0f, size.height * 0.4f), // Tail from the back
                        size = Size(size.width * 0.3f, size.height * 0.2f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.05f)
                    )
                }
            }

            // Game State Overlays
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
                            // Reset game state for a new game
                            coroutineScope.launch { jumpAnim.snapTo(0f) } // Reset jump animation
                            isJumping = false
                            score = 0
                            obstacles = emptyList()
                            gameSpeed = 5f // Reset speed
                            initialDelayHasOccurred = false // Reset initial delay flag
                            gameStartTime = System.currentTimeMillis() // Set for initial delay calculation
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
                                // Reset game state for a new game
                                coroutineScope.launch { jumpAnim.snapTo(0f) }
                                isJumping = false
                                score = 0
                                obstacles = emptyList()
                                gameSpeed = 5f
                                initialDelayHasOccurred = false
                                gameStartTime = System.currentTimeMillis() // Set for initial delay
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

        Spacer(modifier = Modifier.height(16.dp)) // Space between game area and sound meter
        SoundMeter(amplitude = currentAmplitude)
        Spacer(modifier = Modifier.height(24.dp)) // Space before Pause/Resume button

        // Pause/Resume Button
        if (gameState == GameState.PLAYING || gameState == GameState.PAUSED) {
            Button(
                onClick = {
                    if (gameState == GameState.PLAYING) {
                        timeAtPause = System.currentTimeMillis()
                        gameState = GameState.PAUSED
                    } else { // gameState == GameState.PAUSED
                        val pausedDuration = System.currentTimeMillis() - timeAtPause
                        gameStartTime += pausedDuration // Adjust gameStartTime to account for the pause
                        gameState = GameState.PLAYING
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(if (gameState == GameState.PLAYING) "Pause Game" else "Resume Game")
            }
        }
        Spacer(modifier = Modifier.height(16.dp)) // Optional space below button
    }
}

@Composable
private fun SoundMeter(
    modifier: Modifier = Modifier,
    amplitude: Int,
    maxMeterAmplitude: Int = 7500 // Max expected amplitude for meter scaling
) {
    // Ensure amplitude for meter doesn't exceed maxMeterAmplitude if it was already thresholded
    val displayAmplitude = amplitude.coerceAtMost(maxMeterAmplitude)
    val normalizedAmplitude = (displayAmplitude.toFloat() / maxMeterAmplitude).coerceIn(0f, 1f)

    val barColor = when {
        // Check original amplitude for true zero, normalized for meter display
        amplitude == 0 -> MaterialTheme.colorScheme.surfaceVariant // Use a neutral/track color if truly zero
        normalizedAmplitude < 0.4f -> Color(0xFF4CAF50) // Green
        normalizedAmplitude < 0.75f -> Color(0xFFFFEB3B) // Yellow
        else -> Color(0xFFF44336) // Red
    }
    val meterHeight = 20.dp

    Column(
        modifier = modifier.fillMaxWidth(), // Occupy available width for centering
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Sound Level",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box( // Outer track for the meter
            modifier = Modifier
                .height(meterHeight)
                .fillMaxWidth(0.8f) // Meter takes 80% of available width
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant, // Background of the track
                    shape = RoundedCornerShape(meterHeight / 2)
                )
                .clip(RoundedCornerShape(meterHeight / 2)) // Clip inner content to rounded shape
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), // Subtle border
                    RoundedCornerShape(meterHeight / 2)
                )
        ) {
            Box( // Inner bar representing sound level
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(normalizedAmplitude) // Width based on sound level
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