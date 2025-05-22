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
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
enum class GameState { READY, PLAYING, GAME_OVER }

// Obstacle data class
data class Obstacle(
    var xPosition: Float, // Pixel value
    val height: Float,    // Pixel value
    val width: Float,     // Pixel value
    var passed: Boolean = false
)

@Composable
private fun GameContent() {
    var gameState by remember { mutableStateOf(GameState.READY) }
    var score by remember { mutableStateOf(0) }
    var obstacles by remember { mutableStateOf(listOf<Obstacle>()) }
    var gameSpeed by remember { mutableStateOf(5f) } // Pixels per frame
    var gameStartTime by remember { mutableStateOf(0L) }

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
            gameStartTime = System.currentTimeMillis()
            delay(1500) // Initial safe period before obstacles appear

            withContext(Dispatchers.Default) {
                while (gameState == GameState.PLAYING && isActive) {
                    val currentTime = System.currentTimeMillis()
                    val elapsedGameTime = currentTime - gameStartTime

                    obstacles = obstacles.map {
                        it.copy(xPosition = it.xPosition - gameSpeed)
                    }.filter { it.xPosition + it.width > 0 }

                    val canvasWidthPx = gameHeightPx * (16f/9f)
                    val spawnNewObstacleTriggerX = canvasWidthPx * 0.5f
                    val newObstacleBaseX = canvasWidthPx + Random.nextFloat() * (canvasWidthPx * 0.2f)
                    val firstObstacleX = canvasWidthPx + Random.nextFloat() * (canvasWidthPx * 0.2f)

                    val obstacleMinHeightPx = with(density) { 25.dp.toPx()}
                    val obstacleMaxHeightPx = with(density) { 55.dp.toPx()}
                    val obstacleMinWidthPx = with(density) { 15.dp.toPx()}
                    val obstacleMaxWidthPx = with(density) { 30.dp.toPx()}


                    if (obstacles.isEmpty()) {
                        val newObstacle = Obstacle(
                            xPosition = firstObstacleX,
                            height = Random.nextFloat() * (obstacleMaxHeightPx - obstacleMinHeightPx) + obstacleMinHeightPx,
                            width = Random.nextFloat() * (obstacleMaxWidthPx - obstacleMinWidthPx) + obstacleMinWidthPx
                        )
                        obstacles = listOf(newObstacle)
                    } else if (obstacles.last().xPosition < spawnNewObstacleTriggerX && obstacles.size < 5) {
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
                            gameSpeed = (5f + (score / 8f)).coerceAtMost(18f)
                        }
                    }

                    if (elapsedGameTime > 2000) {
                        val currentDinoTopYPx = dinoTopYOnGroundPx - (jumpAnim.value * jumpMagnitudePx)
                        val dinosaurVisualRect = Rect(
                            left = dinosaurVisualXPositionPx,
                            top = currentDinoTopYPx,
                            right = dinosaurVisualXPositionPx + dinosaurSizePx,
                            bottom = currentDinoTopYPx + dinosaurSizePx
                        )

                        val horizontalHitboxReductionPx = dinosaurSizePx * 0.20f
                        val verticalHitboxReductionPx = dinosaurSizePx * 0.20f

                        val dinosaurHitbox = Rect(
                            left = dinosaurVisualRect.left + horizontalHitboxReductionPx,
                            top = dinosaurVisualRect.top + verticalHitboxReductionPx,
                            right = dinosaurVisualRect.right - horizontalHitboxReductionPx,
                            bottom = dinosaurVisualRect.bottom - (verticalHitboxReductionPx * 0.3f)
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
                            break
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
                            val maxAmplitude = buffer.take(read).maxOfOrNull { it.toInt().absoluteValue } ?: 0
                            if (maxAmplitude > 2500 && !isJumping) {
                                isJumping = true
                                coroutineScope.launch {
                                    jumpAnim.snapTo(0f)
                                    jumpAnim.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing) // Increased duration
                                    )
                                    jumpAnim.animateTo(
                                        targetValue = 0f,
                                        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing) // Increased duration
                                    )
                                    isJumping = false
                                }
                            }
                        }
                        delay(50) // Poll audio
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
                .background(Color(0xFFE0F7FA))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Draw ground
                drawRect(
                    color = Color(0xFF795548),
                    topLeft = Offset(0f, size.height - groundHeightPx),
                    size = Size(size.width, groundHeightPx)
                )

                // Draw obstacles
                obstacles.forEach { obstacle ->
                    drawRect(
                        color = Color(0xFF4CAF50),
                        topLeft = Offset(
                            obstacle.xPosition,
                            gameHeightPx - groundHeightPx - obstacle.height
                        ),
                        size = Size(obstacle.width, obstacle.height)
                    )
                }
            }

            val currentDinoTopYDp = with(density) { (dinoTopYOnGroundPx - (jumpAnim.value * jumpMagnitudePx)).toDp() }
            Box(
                modifier = Modifier
                    .size(dinosaurSizeDp)
                    .offset(
                        x = dinosaurVisualXOffsetDp,
                        y = currentDinoTopYDp
                    )
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Body
                    drawRoundRect(
                        color = Color(0xFFF44336),
                        topLeft = Offset(size.width * 0.15f, size.height * 0.2f),
                        size = Size(size.width * 0.7f, size.height * 0.6f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.1f)
                    )
                    // Head
                    drawRoundRect(
                        color = Color(0xFFF44336),
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
                        color = Color(0xFFD32F2F),
                        topLeft = Offset(size.width * (0.3f - legOffsetFactor), legYPos),
                        size = Size(legWidth, legHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(legWidth * 0.3f)
                    )
                    // Front Leg
                    drawRoundRect(
                        color = Color(0xFFD32F2F),
                        topLeft = Offset(size.width * (0.55f + legOffsetFactor), legYPos),
                        size = Size(legWidth, legHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(legWidth * 0.3f)
                    )
                    // Tail
                    drawRoundRect(
                        color = Color(0xFFF44336),
                        topLeft = Offset(size.width * 0.0f, size.height * 0.4f),
                        size = Size(size.width * 0.3f, size.height * 0.2f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.05f)
                    )
                }
            }

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
                                gameState = GameState.PLAYING
                            }
                        ) { Text("Play Again") }
                    }
                }
                else -> { /* Playing state - no overlay */ }
            }
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