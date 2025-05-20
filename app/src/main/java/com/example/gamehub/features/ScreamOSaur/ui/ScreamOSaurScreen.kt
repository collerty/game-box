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

    if (showPermissionDialog) {
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
    var xPosition: Float,
    val height: Float,
    val width: Float,
    var passed: Boolean = false
)

@Composable
private fun GameContent() {
    // Game state
    var gameState by remember { mutableStateOf(GameState.READY) }
    var score by remember { mutableStateOf(0) }
    var obstacles by remember { mutableStateOf(listOf<Obstacle>()) }
    var gameSpeed by remember { mutableStateOf(5f) } // Pixels per frame

    // Dinosaur properties
    val jumpAnim = remember { Animatable(0f) }
    var isJumping by remember { mutableStateOf(false) }
    val dinosaurSize = 60.dp
    val dinoBaseHeight = 100.dp

    // Game area properties
    val gameHeight = 300.dp
    val groundHeight = 20.dp

    val coroutineScope = rememberCoroutineScope()

    // Get context outside of LaunchedEffect
    val context = LocalContext.current

    // Game loop
    LaunchedEffect(gameState) {
        if (gameState == GameState.PLAYING) {
            // Game loop on Default dispatcher to not block the UI
            withContext(Dispatchers.Default) {
                while (gameState == GameState.PLAYING && isActive) {
                    // Move obstacles
                    obstacles = obstacles.map {
                        it.copy(xPosition = it.xPosition - gameSpeed)
                    }.filter { it.xPosition > -50f } // Remove off-screen obstacles

                    // Generate new obstacles
                    if (obstacles.isEmpty() || obstacles.last().xPosition < 500) {
                        val newObstacle = Obstacle(
                            xPosition = 800f + Random.nextFloat() * 200f,
                            height = 30f + Random.nextFloat() * 30f,
                            width = 20f + Random.nextFloat() * 30f
                        )
                        obstacles = obstacles + newObstacle
                    }

                    // Check for passed obstacles (for scoring)
                    obstacles.forEach { obstacle ->
                        if (!obstacle.passed && obstacle.xPosition < 180f) {
                            obstacle.passed = true
                            score++
                            // Increase speed slightly as score increases
                            gameSpeed = 5f + (score / 10f).coerceAtMost(10f)
                        }
                    }

                    // Collision detection
                    val dinosaurRect = Rect(
                        left = 200f,
                        top = (dinoBaseHeight.value - jumpAnim.value * 120),
                        right = 200f + dinosaurSize.value,
                        bottom = dinoBaseHeight.value + dinosaurSize.value
                    )

                    obstacles.forEach { obstacle ->
                        val obstacleRect = Rect(
                            left = obstacle.xPosition,
                            top = gameHeight.value - groundHeight.value - obstacle.height,
                            right = obstacle.xPosition + obstacle.width,
                            bottom = gameHeight.value - groundHeight.value
                        )

                        if (dinosaurRect.overlaps(obstacleRect)) {
                            gameState = GameState.GAME_OVER
                        }
                    }

                    delay(16) // ~60fps
                }
            }
        }
    }

    // Audio detection for jumping
    LaunchedEffect(gameState) {
        if (gameState == GameState.PLAYING) {
            val bufferSize = AudioRecord.getMinBufferSize(
                8000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            // Using context captured from outside the LaunchedEffect
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED) {
                // Permission was revoked, go back to ready state
                gameState = GameState.READY
                return@LaunchedEffect
            }

            try {
                val audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    8000,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )

                val buffer = ShortArray(bufferSize)
                audioRecord.startRecording()

                try {
                    while (gameState == GameState.PLAYING && isActive) {
                        val read = audioRecord.read(buffer, 0, buffer.size)
                        val max = buffer.take(read).maxOfOrNull { it.toInt().absoluteValue } ?: 0

                        if (max > 2000 && !isJumping) { // Threshold for "roar"
                            isJumping = true
                            coroutineScope.launch {
                                jumpAnim.snapTo(0f)
                                jumpAnim.animateTo(
                                    1f,
                                    animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing)
                                )
                                jumpAnim.animateTo(
                                    0f,
                                    animationSpec = tween(durationMillis = 300, easing = FastOutLinearInEasing)
                                )
                                isJumping = false
                            }
                        }
                        delay(50) // Poll audio less frequently than game loop
                    }
                } finally {
                    audioRecord.stop()
                    audioRecord.release()
                }
            } catch (e: SecurityException) {
                // Handle permission denial
                gameState = GameState.READY
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Score display
        Text(
            text = "Score: $score",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // Game area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(gameHeight)
                .background(Color(0xFFF0F0F0))
        ) {
            // Draw game elements
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Draw ground
                drawRect(
                    color = Color(0xFF8B4513),
                    topLeft = Offset(0f, size.height - groundHeight.toPx()),
                    size = Size(size.width, groundHeight.toPx())
                )

                // Draw obstacles
                obstacles.forEach { obstacle ->
                    drawRect(
                        color = Color(0xFF006400),
                        topLeft = Offset(
                            obstacle.xPosition,
                            size.height - groundHeight.toPx() - obstacle.height
                        ),
                        size = Size(obstacle.width, obstacle.height)
                    )
                }
            }

            // Dinosaur
            Box(
                modifier = Modifier
                    .size(dinosaurSize)
                    .offset(
                        x = 200.dp,
                        y = dinoBaseHeight - (jumpAnim.value * 120).dp
                    )
            ) {
                // Simple dinosaur representation
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color(0xFF4CAF50),
                        radius = size.minDimension / 2
                    )
                }
            }

            // Game state overlays
            when (gameState) {
                GameState.READY -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("ROAR to make the dinosaur jump!", fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { gameState = GameState.PLAYING }) {
                            Text("Start Game")
                        }
                    }
                }
                GameState.GAME_OVER -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Game Over!", fontSize = 24.sp, color = Color.Red)
                        Text("Score: $score", fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                score = 0
                                obstacles = emptyList()
                                gameSpeed = 5f
                                gameState = GameState.PLAYING
                            }
                        ) {
                            Text("Play Again")
                        }
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
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "This game needs microphone access to hear your roar!",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRequestPermission
        ) {
            Text("Grant Microphone Access")
        }
    }
}