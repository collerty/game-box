package com.example.gamehub.features.ScreamOSaur.ui

import android.Manifest
import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Animatable // Added for Animatable
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import coil.compose.AsyncImagePainter // Added for state type
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.example.gamehub.R
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

    DisposableEffect(Unit) {
        val activity = context as? Activity
        val originalOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        onDispose {
            activity?.requestedOrientation = originalOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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

    if (showPermissionDialog && !hasAudioPermission) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Microphone Access Needed") },
            text = { Text("This game needs microphone access to hear your roar and make the dinosaur jump!") },
            confirmButton = {
                Button(
                    onClick = {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                ) { Text("Grant Access") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPermissionDialog = false }
                ) { Text("Not Now") }
            }
        )
    }
}

enum class GameState { READY, PLAYING, PAUSED, GAME_OVER }

data class Obstacle(
    var xPosition: Float,
    val height: Float,
    val width: Float,
    var passed: Boolean = false,
    val id: Long = Random.nextLong()
)

private const val MIN_AMPLITUDE_THRESHOLD = 1800
private const val JUMP_AMPLITUDE_THRESHOLD = 12000

@Composable
private fun GameContent() {
    var gameState by remember { mutableStateOf(GameState.READY) }
    var score by remember { mutableStateOf(0) }
    var obstacles by remember { mutableStateOf(listOf<Obstacle>()) }
    var gameSpeed by remember { mutableStateOf(5f) }
    var gameStartTime by remember { mutableStateOf(0L) }
    var timeAtPause by remember { mutableStateOf(0L) }
    var initialDelayHasOccurred by remember { mutableStateOf(false) }

    val jumpAnim = remember { Animatable(0f) }
    var isJumping by remember { mutableStateOf(false) }

    val dinosaurSizeDp = 70.dp // Increased from 50.dp to 70.dp
    val gameHeightDp = 200.dp
    val groundHeightDp = 15.dp
    val dinosaurVisualXOffsetDp = 40.dp
    val jumpMagnitudeDp = 120.dp

    val density = LocalDensity.current // Capture density once
    val configuration = LocalConfiguration.current

    val actualScreenWidthPx: Float = with(density) {
        configuration.screenWidthDp.dp.toPx()
    }

    val gameHeightPx = with(density) { gameHeightDp.toPx() }
    val groundHeightPx = with(density) { groundHeightDp.toPx() }
    val dinosaurSizePx = with(density) { dinosaurSizeDp.toPx() }
    val dinosaurVisualXPositionPx = with(density) { dinosaurVisualXOffsetDp.toPx() }
    val jumpMagnitudePx = with(density) { jumpMagnitudeDp.toPx() }

    // Pre-calculate obstacle dimensions in Px using the captured density
    val obstacleMinHeightPx = with(density) { 25.dp.toPx() }
    val obstacleMaxHeightPx = with(density) { 55.dp.toPx() }
    val obstacleMinWidthPx = with(density) { 15.dp.toPx() }
    val obstacleMaxWidthPx = with(density) { 30.dp.toPx() }

    val dinoTopYOnGroundPx = gameHeightPx - groundHeightPx - dinosaurSizePx

    val runningAnimState = remember { mutableStateOf(0) }
    var isRunningAnimating by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val jumpSoundPlayer = remember {
        MediaPlayer.create(context, R.raw.dino_jump_sound).apply {
            setVolume(0.5f, 0.5f)
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
            try {
                jumpSoundPlayer.start()
            } catch (e: IllegalStateException) {
                android.util.Log.e("GameContent", "Error playing jump sound", e)
            }
        }
    }

    var currentAmplitude by remember { mutableStateOf(0) }

    LaunchedEffect(gameState, isRunningAnimating) {
        if (gameState == GameState.PLAYING && isRunningAnimating) {
            while (isActive && gameState == GameState.PLAYING) {
                runningAnimState.value = (runningAnimState.value + 1) % 4
                delay(100)
            }
        } else if (gameState != GameState.PLAYING) {
            runningAnimState.value = 0
        }
    }

    LaunchedEffect(gameState) {
        if (gameState == GameState.PLAYING) {
            if (!initialDelayHasOccurred) {
                delay(1500)
                gameStartTime = System.currentTimeMillis()
                initialDelayHasOccurred = true
            }
            isRunningAnimating = true

            withContext(Dispatchers.Default) {
                while (isActive && gameState == GameState.PLAYING) {
                    val currentTime = System.currentTimeMillis()
                    val elapsedGameTime = currentTime - gameStartTime

                    obstacles = obstacles.map {
                        it.copy(xPosition = it.xPosition - gameSpeed)
                    }.filter { it.xPosition + it.width > 0 }

                    val spawnNewObstacleTriggerX = actualScreenWidthPx * 0.5f

                    if (obstacles.isEmpty() && elapsedGameTime > 0) {
                        obstacles = listOf(
                            Obstacle(
                                xPosition = actualScreenWidthPx + Random.nextFloat() * (actualScreenWidthPx * 0.2f),
                                height = Random.nextFloat() * (obstacleMaxHeightPx - obstacleMinHeightPx) + obstacleMinHeightPx,
                                width = Random.nextFloat() * (obstacleMaxWidthPx - obstacleMinWidthPx) + obstacleMinWidthPx
                            )
                        )
                    } else if (obstacles.isNotEmpty() && obstacles.last().xPosition < spawnNewObstacleTriggerX && obstacles.size < 5) {
                        obstacles = obstacles + Obstacle(
                            xPosition = actualScreenWidthPx + Random.nextFloat() * (actualScreenWidthPx * 0.2f),
                            height = Random.nextFloat() * (obstacleMaxHeightPx - obstacleMinHeightPx) + obstacleMinHeightPx,
                            width = Random.nextFloat() * (obstacleMaxWidthPx - obstacleMinWidthPx) + obstacleMinWidthPx
                        )
                    }

                    var scoreChangedThisFrame = false
                    obstacles = obstacles.map { obstacle ->
                        if (!obstacle.passed && (obstacle.xPosition + obstacle.width) < dinosaurVisualXPositionPx) {
                            scoreChangedThisFrame = true
                            obstacle.copy(passed = true)
                        } else {
                            obstacle
                        }
                    }
                    if (scoreChangedThisFrame) {
                        score++
                        gameSpeed = (5f + (score / 5f)).coerceAtMost(20f)
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
                                isRunningAnimating = false
                                gameState = GameState.GAME_OVER
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            break
                        }
                    }
                    delay(16)
                }
            }
        } else {
            isRunningAnimating = false
        }
    }

    LaunchedEffect(gameState, context) {
        if (gameState == GameState.PLAYING) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                withContext(Dispatchers.Main) { gameState = GameState.READY }
                return@LaunchedEffect
            }

            var audioRecord: AudioRecord? = null
            try {
                val bufferSize = AudioRecord.getMinBufferSize(8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                if (bufferSize == AudioRecord.ERROR_BAD_VALUE || bufferSize == AudioRecord.ERROR) {
                    android.util.Log.e("GameContent", "AudioRecord: Bad buffer size")
                    withContext(Dispatchers.Main) { gameState = GameState.READY }
                    return@LaunchedEffect
                }

                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC, 8000,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize
                )

                if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                    android.util.Log.e("GameContent", "AudioRecord not initialized")
                    audioRecord.release()
                    withContext(Dispatchers.Main) { gameState = GameState.READY }
                    return@LaunchedEffect
                }

                val buffer = ShortArray(bufferSize)
                audioRecord.startRecording()
                android.util.Log.d("GameContent", "AudioRecord started")

                withContext(Dispatchers.IO) {
                    while (isActive && gameState == GameState.PLAYING) {
                        val read = audioRecord.read(buffer, 0, buffer.size)
                        if (read > 0) {
                            val maxAmplitudeRaw = buffer.take(read).maxOfOrNull { it.toInt().absoluteValue } ?: 0
                            currentAmplitude = if (maxAmplitudeRaw >= MIN_AMPLITUDE_THRESHOLD) maxAmplitudeRaw else 0

                            if (maxAmplitudeRaw > JUMP_AMPLITUDE_THRESHOLD && !isJumping) {
                                withContext(Dispatchers.Main) {
                                    if (!isJumping) {
                                        isJumping = true
                                        playJumpSound()
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
                            }
                        }
                        delay(50)
                    }
                }
            } catch (e: SecurityException) {
                android.util.Log.e("GameContent", "AudioRecord SecurityException: ${e.message}", e)
                withContext(Dispatchers.Main) { gameState = GameState.READY }
            } catch (e: IllegalStateException) {
                android.util.Log.e("GameContent", "AudioRecord IllegalStateException: ${e.message}", e)
                withContext(Dispatchers.Main) { gameState = GameState.READY }
            } finally {
                android.util.Log.d("GameContent", "AudioRecord stopping and releasing")
                audioRecord?.apply {
                    if (recordingState == AudioRecord.RECORDSTATE_RECORDING) stop()
                    release()
                }
                currentAmplitude = 0
            }
        } else {
            currentAmplitude = 0
        }
    }

    // Define grayscale colors
    val gameBackgroundColor = Color.White
    val dinoColor = Color(0xFF808080) // Gray
    val dinoDarkerColor = Color(0xFF606060) // Darker Gray
    val cactusColor = Color(0xFF808080) // Gray
    val cactusDarkerColor = Color(0xFF606060) // Darker Gray
    val groundColor = Color(0xFF808080) // Gray
    val groundDetailColor = Color(0xFF606060) // Darker Gray
    val sunColor = Color(0xFFD3D3D3) // Light Gray
    val sunRayColor = Color(0xFFBEBEBE) // Slightly Darker Gray
    val scoreTextColor = Color.Black // Or a dark gray like Color(0xFF333333)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Score: $score",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = scoreTextColor
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(gameHeightDp)
                .background(gameBackgroundColor) // Use gameBackgroundColor
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Sun
                val sunRadius = with(density) { 25.dp.toPx() }
                val sunCenter = Offset(size.width * 0.15f, size.height * 0.15f)
                drawCircle(color = sunColor, radius = sunRadius, center = sunCenter)
                val numRays = 8
                for (i in 0 until numRays) {
                    val angle = Math.toRadians((i * (360.0 / numRays)) + 15.0)
                    val rayStartOffset = sunRadius * 1.1f
                    val rayEndOffset = sunRadius * 1.6f
                    drawLine(
                        color = sunRayColor.copy(alpha = 0.7f),
                        start = Offset(sunCenter.x + kotlin.math.cos(angle).toFloat() * rayStartOffset, sunCenter.y + kotlin.math.sin(angle).toFloat() * rayStartOffset),
                        end = Offset(sunCenter.x + kotlin.math.cos(angle).toFloat() * rayEndOffset, sunCenter.y + kotlin.math.sin(angle).toFloat() * rayEndOffset),
                        strokeWidth = with(density) { 2.dp.toPx() }
                    )
                }

                // Ground
                val groundTopYCanvas = size.height - groundHeightPx
                drawRect(
                    color = groundColor,
                    topLeft = Offset(0f, groundTopYCanvas),
                    size = Size(size.width, groundHeightPx)
                )
                val numGroundPatches = 30
                for (i in 0..numGroundPatches) {
                    val patchY = groundTopYCanvas + (Random.nextFloat() * (groundHeightPx - with(density) { 4.dp.toPx() }) + with(density) { 2.dp.toPx() })
                    val patchX = Random.nextFloat() * size.width
                    val patchWidth = Random.nextFloat() * with(density) { 20.dp.toPx() } + with(density) { 10.dp.toPx() }
                    val patchHeight = Random.nextFloat() * with(density) { 2.dp.toPx() } + with(density) { 1.dp.toPx() }
                    drawRect(
                        color = groundDetailColor.copy(alpha = Random.nextFloat() * 0.3f + 0.2f),
                        topLeft = Offset(patchX, patchY),
                        size = Size(patchWidth, patchHeight)
                    )
                }

                // Obstacles (Cactus)
                obstacles.forEach { obstacle ->
                    val obX = obstacle.xPosition
                    val obY = gameHeightPx - groundHeightPx - obstacle.height
                    val obW = obstacle.width
                    val obH = obstacle.height
                    val bodyW = obW * 0.4f
                    val bodyH = obH
                    val bodyX = obX + (obW - bodyW) / 2
                    val bodyBaseY = obY
                    drawRoundRect(color = cactusColor, topLeft = Offset(bodyX, bodyBaseY), size = Size(bodyW, bodyH), cornerRadius = androidx.compose.ui.geometry.CornerRadius(bodyW * 0.25f))
                    val armBaseHeight = bodyH * 0.4f
                    val armBaseWidth = bodyW * 0.8f
                    if (obH > with(density) { 30.dp.toPx() }) {
                        val armAttachY = bodyBaseY + bodyH * 0.2f
                        val horizontalSegmentW = armBaseWidth * 0.5f
                        val horizontalSegmentH = armBaseHeight * 0.3f
                        val verticalSegmentW = armBaseWidth * 0.3f
                        val verticalSegmentH = armBaseHeight * 0.8f
                        val rArmHorizX = bodyX + bodyW - horizontalSegmentW * 0.2f
                        drawRoundRect(color = cactusColor, topLeft = Offset(rArmHorizX, armAttachY), size = Size(horizontalSegmentW, horizontalSegmentH), cornerRadius = androidx.compose.ui.geometry.CornerRadius(horizontalSegmentH * 0.5f))
                        val rArmVertX = rArmHorizX + horizontalSegmentW - verticalSegmentW * 0.5f
                        val rArmVertY = armAttachY - verticalSegmentH + horizontalSegmentH
                        drawRoundRect(color = cactusColor, topLeft = Offset(rArmVertX, rArmVertY), size = Size(verticalSegmentW, verticalSegmentH), cornerRadius = androidx.compose.ui.geometry.CornerRadius(verticalSegmentW * 0.5f))
                    }
                    if (obH > with(density) { 40.dp.toPx() } && obW > with(density) { 20.dp.toPx() }) {
                        val armAttachY = bodyBaseY + bodyH * 0.45f
                        val horizontalSegmentW = armBaseWidth * 0.5f
                        val horizontalSegmentH = armBaseHeight * 0.3f
                        val verticalSegmentW = armBaseWidth * 0.3f
                        val verticalSegmentH = armBaseHeight * 0.8f
                        val lArmHorizX = bodyX - horizontalSegmentW + horizontalSegmentW * 0.2f
                        drawRoundRect(color = cactusColor, topLeft = Offset(lArmHorizX, armAttachY), size = Size(horizontalSegmentW, horizontalSegmentH), cornerRadius = androidx.compose.ui.geometry.CornerRadius(horizontalSegmentH * 0.5f))
                        val lArmVertX = lArmHorizX + horizontalSegmentW * 0.5f - verticalSegmentW * 0.5f
                        val lArmVertY = armAttachY - verticalSegmentH + horizontalSegmentH
                        drawRoundRect(color = cactusColor, topLeft = Offset(lArmVertX, lArmVertY), size = Size(verticalSegmentW, verticalSegmentH), cornerRadius = androidx.compose.ui.geometry.CornerRadius(verticalSegmentW * 0.5f))
                    }
                    val numLines = (bodyW / with(density) { 6.dp.toPx() }).toInt().coerceIn(2, 5)
                    if (numLines > 1) {
                        for (i in 1 until numLines) {
                            val lineX = bodyX + (bodyW / numLines) * i
                            drawLine(color = cactusDarkerColor, start = Offset(lineX, bodyBaseY + bodyH * 0.05f), end = Offset(lineX, bodyBaseY + bodyH * 0.95f), strokeWidth = with(density) { 1.dp.toPx() })
                        }
                    }
                }
            }

            // Dinosaur
            // For toDp conversion, use with(density) { ... toDp() }
            val currentDinoTopPx = dinoTopYOnGroundPx - (jumpAnim.value * jumpMagnitudePx)
            val currentDinoTopYDp = with(density) { currentDinoTopPx.toDp() }

            val dinoGifVerticalOffsetDp = 8.dp
            Box(
                modifier = Modifier
                    .size(dinosaurSizeDp)
                    .offset(x = dinosaurVisualXOffsetDp, y = currentDinoTopYDp + dinoGifVerticalOffsetDp)
            ) {
                val imageLoader = coil.ImageLoader.Builder(LocalContext.current)
                    .components {
                        add(ImageDecoderDecoder.Factory())
                    }
                    .build()

                val painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data("file:///android_asset/dino_model.gif")
                        .diskCacheKey(gameState.name)
                        .build(),
                    imageLoader = imageLoader,
                    onState = { state ->
                        if (state is AsyncImagePainter.State.Success) {
                            val drawable = state.result.drawable
                            if (drawable is Animatable) {
                                if (gameState == GameState.PLAYING) {
                                    drawable.start()
                                } else {
                                    drawable.stop()
                                }
                            }
                        }
                    }
                )
                Image(
                    painter = painter,
                    contentDescription = "Dinosaur",
                    modifier = Modifier.fillMaxSize()
                )
            }

            when (gameState) {
                GameState.READY -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("ROAR to make the dinosaur jump!", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = scoreTextColor)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            coroutineScope.launch { jumpAnim.snapTo(0f) }
                            isJumping = false
                            score = 0
                            obstacles = emptyList()
                            gameSpeed = 5f
                            initialDelayHasOccurred = false
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
                        Text("Score: $score", fontSize = 20.sp, color = scoreTextColor)
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(
                            onClick = {
                                coroutineScope.launch { jumpAnim.snapTo(0f) }
                                isJumping = false
                                score = 0
                                obstacles = emptyList()
                                gameSpeed = 5f
                                initialDelayHasOccurred = false
                                gameState = GameState.PLAYING
                                isRunningAnimating = true
                            }
                        ) {
                            Text(
                                text = "Play Again",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = scoreTextColor
                            )
                        }
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

        Spacer(modifier = Modifier.height(8.dp))
        SoundMeter(amplitude = currentAmplitude)
        Spacer(modifier = Modifier.height(16.dp))

        if (gameState == GameState.PLAYING || gameState == GameState.PAUSED) {
            TextButton(
                onClick = {
                    if (gameState == GameState.PLAYING) {
                        timeAtPause = System.currentTimeMillis()
                        gameState = GameState.PAUSED
                        isRunningAnimating = false
                    } else {
                        val pausedDuration = System.currentTimeMillis() - timeAtPause
                        gameStartTime += pausedDuration
                        gameState = GameState.PLAYING
                        isRunningAnimating = true
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = if (gameState == GameState.PLAYING) "Pause" else "Resume",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = scoreTextColor
                )
            }
        }
    }
}

@Composable
private fun SoundMeter(
    modifier: Modifier = Modifier,
    amplitude: Int,
    maxMeterAmplitude: Int = JUMP_AMPLITUDE_THRESHOLD
) {
    val displayAmplitude = amplitude.coerceAtMost(maxMeterAmplitude)
    val normalizedAmplitude = (displayAmplitude.toFloat() / maxMeterAmplitude).coerceIn(0f, 1f)

    val barColor = when {
        amplitude == 0 -> Color(0xFFE0E0E0)
        normalizedAmplitude < 0.4f -> Color(0xFFA0A0A0)
        normalizedAmplitude < 0.75f -> Color(0xFF808080)
        else -> Color(0xFF606060)
    }
    val meterHeight = 20.dp

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Sound Level",
            fontSize = 12.sp,
            color = Color.DarkGray
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .height(meterHeight)
                .fillMaxWidth(0.8f)
                .background(
                    color = Color(0xFFF0F0F0),
                    shape = RoundedCornerShape(meterHeight / 2)
                )
                .clip(RoundedCornerShape(meterHeight / 2))
                .border(
                    1.dp,
                    Color.Gray.copy(alpha = 0.5f),
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
            modifier = Modifier.padding(bottom = 16.dp),
            color = Color.Black
        )
        Button(onClick = onRequestPermission) {
            Text("Grant Microphone Access")
        }
    }
}
