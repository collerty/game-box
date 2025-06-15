package com.example.gamehub.features.ScreamOSaur.ui

import android.Manifest
import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Animatable // Added for GIF control
import android.os.Build.VERSION.SDK_INT
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.ImageLoader
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.example.gamehub.R
import com.example.gamehub.features.ScreamOSaur.model.GameState
import com.example.gamehub.features.ScreamOSaur.model.Obstacle
import com.example.gamehub.features.ScreamOSaur.model.ScreamOSaurViewModel
import kotlin.random.Random

@Composable
fun ScreamosaurScreen(viewModel: ScreamOSaurViewModel = viewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showPermissionDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            viewModel.updateAudioPermissionState(isGranted)
            showPermissionDialog = !isGranted
        }
    )

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.updateAudioPermissionState(hasPermission)
        if (!hasPermission) {
            showPermissionDialog = true
        }
    }

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
            if (uiState.hasAudioPermission == true) {
                GameContent(viewModel)
            } else {
                PermissionRequest(
                    onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                )
            }
        }
    }

    if (showPermissionDialog && uiState.hasAudioPermission == false) {
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

@Composable
private fun GameContent(viewModel: ScreamOSaurViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current

    val gameBackgroundColor = Color.White
    val gameObjectColor = Color.Black
    val scoreTextColor = Color(0xFF535353)

    val dinosaurSizeDp = 54.dp // Increased size
    val gameHeightDp = 200.dp
    val groundHeightDp = 2.dp
    val dinosaurVisualXOffsetDp = 40.dp
    val jumpMagnitudeDp = 100.dp

    val density = LocalDensity.current

    LaunchedEffect(density, gameHeightDp, groundHeightDp, dinosaurSizeDp, dinosaurVisualXOffsetDp, jumpMagnitudeDp) {
        with(density) {
            viewModel.setGameDimensions(
                gameHeightPx = gameHeightDp.toPx(),
                groundHeightPx = groundHeightDp.toPx(),
                dinosaurSizePx = dinosaurSizeDp.toPx(),
                dinosaurVisualXPositionPx = dinosaurVisualXOffsetDp.toPx(),
                jumpMagnitudePx = jumpMagnitudeDp.toPx()
            )
        }
    }

    LaunchedEffect(uiState.gameState) {
        if (uiState.gameState == GameState.GAME_OVER) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, end = 16.dp)) {
            Text(
                text = "HI ${uiState.score.toString().padStart(5, '0')}",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, color = scoreTextColor, fontWeight = FontWeight.Bold),
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(gameHeightDp)
                .background(gameBackgroundColor)
                .border(BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gameWidthPx = size.width
                val gameHeightPx = uiState.gameHeightPx
                val groundHeightPx = uiState.groundHeightPx
                val groundTopY = gameHeightPx - groundHeightPx

                drawLine(
                    color = gameObjectColor,
                    start = Offset(0f, groundTopY),
                    end = Offset(gameWidthPx, groundTopY),
                    strokeWidth = groundHeightPx
                )

                uiState.obstacles.forEach { obstacle ->
                    val obX = obstacle.xPosition
                    val obYBase = gameHeightPx - groundHeightPx - obstacle.height
                    val obW = obstacle.width
                    val obH = obstacle.height

                    drawRect(
                        color = gameObjectColor,
                        topLeft = Offset(obX + obW * 0.3f, obYBase),
                        size = Size(obW * 0.4f, obH)
                    )
                    if (obH > 20.dp.toPx() && obW > 15.dp.toPx()) {
                        val armWidth = obW * 0.3f
                        val armHeight = obH * 0.4f
                        drawRect(
                            color = gameObjectColor,
                            topLeft = Offset(obX, obYBase + obH * 0.2f),
                            size = Size(armWidth, armHeight)
                        )
                        drawRect(
                            color = gameObjectColor,
                            topLeft = Offset(obX, obYBase + obH * 0.2f),
                            size = Size(obW * 0.7f, armHeight * 0.4f)
                        )
                        drawRect(
                            color = gameObjectColor,
                            topLeft = Offset(obX + obW * 0.7f, obYBase + obH * 0.1f),
                            size = Size(armWidth, armHeight * 0.8f)
                        )
                        drawRect(
                            color = gameObjectColor,
                            topLeft = Offset(obX + obW * 0.3f, obYBase + obH * 0.1f),
                            size = Size(obW * 0.7f, armHeight * 0.3f)
                        )
                    }
                }
            }

            val currentDinoTopYDp = with(density) {
                (uiState.dinoTopYOnGroundPx - (uiState.jumpAnimValue * uiState.jumpMagnitudePx)).toDp()
            }

            // Dinosaur GIF Display & Control
            val imageLoader = ImageLoader.Builder(LocalContext.current)
                .components {
                    if (SDK_INT >= 28) {
                        add(ImageDecoderDecoder.Factory())
                    } else {
                        add(GifDecoder.Factory())
                    }
                }
                .build()

            val painter = rememberAsyncImagePainter(
                model = "file:///android_asset/dino_model.gif",
                imageLoader = imageLoader
            )

            Image(
                painter = painter,
                contentDescription = "Dinosaur",
                modifier = Modifier
                    .size(dinosaurSizeDp) // Uses the updated size
                    .offset(x = dinosaurVisualXOffsetDp, y = currentDinoTopYDp)
            )

            // Control GIF animation based on game state
            val painterState = painter.state
            if (painterState is AsyncImagePainter.State.Success) {
                val animatable = painterState.result.drawable as? Animatable
                if (animatable != null) {
                    // Use DisposableEffect to manage start/stop and ensure stop on dispose/key change
                    DisposableEffect(animatable, uiState.gameState) {
                        if (uiState.gameState == GameState.PLAYING) {
                            animatable.start()
                        } else {
                            // Stop for READY, PAUSED, GAME_OVER
                            animatable.stop()
                        }

                        onDispose {
                            // This ensures the animation is stopped when the effect is disposed
                            // (e.g., gameState changes, animatable instance changes, or composable is removed)
                            animatable.stop()
                        }
                    }
                }
            }

            when (uiState.gameState) {
                GameState.READY -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("ROAR TO JUMP", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = gameObjectColor)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.startGame() },
                            colors = ButtonDefaults.buttonColors(containerColor = gameObjectColor, contentColor = gameBackgroundColor)
                        ) { Text("START") }
                    }
                }
                GameState.GAME_OVER -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("G A M E   O V E R", fontSize = 24.sp, color = gameObjectColor, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.startGame() },
                            colors = ButtonDefaults.buttonColors(containerColor = gameObjectColor, contentColor = gameBackgroundColor)
                        ) { Text("RESTART") }
                    }
                }
                GameState.PAUSED -> {
                    Box(
                        modifier = Modifier.fillMaxSize().background(gameBackgroundColor.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("P A U S E D", fontSize = 22.sp, color = gameObjectColor, fontWeight = FontWeight.Bold)
                    }
                }
                GameState.PLAYING -> { /* No overlay */ }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        SoundMeter(amplitude = uiState.currentAmplitude)
        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.gameState == GameState.PLAYING || uiState.gameState == GameState.PAUSED) {
            Button(
                onClick = {
                    if (uiState.gameState == GameState.PLAYING) {
                        viewModel.pauseGame()
                    } else {
                        viewModel.resumeGame()
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(if (uiState.gameState == GameState.PLAYING) "Pause Game" else "Resume Game")
            }
        }
    }
}

@Composable
private fun SoundMeter(
    modifier: Modifier = Modifier,
    amplitude: Int,
    maxMeterAmplitude: Int = ScreamOSaurViewModel.JUMP_AMPLITUDE_THRESHOLD
) {
    val displayAmplitude = amplitude.coerceAtMost(maxMeterAmplitude)
    val normalizedAmplitude = (displayAmplitude.toFloat() / maxMeterAmplitude).coerceIn(0f, 1f)

    val barColor = when {
        amplitude == 0 -> Color.LightGray
        normalizedAmplitude < 0.4f -> Color.Gray
        normalizedAmplitude < 0.75f -> Color.DarkGray
        else -> Color.Black
    }
    val meterHeight = 16.dp

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "SOUND",
            fontSize = 10.sp,
            color = Color.DarkGray
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .height(meterHeight)
                .fillMaxWidth(0.8f)
                .background(
                    color = Color.LightGray,
                    shape = RoundedCornerShape(meterHeight / 2)
                )
                .clip(RoundedCornerShape(meterHeight / 2))
                .border(
                    1.dp,
                    Color.Gray.copy(alpha = 0.3f),
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
            "GAME NEEDS MIC TO HEAR ROAR!",
            style = MaterialTheme.typography.titleMedium.copy(color = Color.Black),
            modifier = Modifier.padding(bottom = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White)
        ) {
            Text("GRANT MIC ACCESS")
        }
    }
}

