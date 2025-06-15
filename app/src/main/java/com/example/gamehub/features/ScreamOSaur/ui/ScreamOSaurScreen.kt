package com.example.gamehub.features.screamosaur.ui

import android.Manifest
import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gamehub.R
import com.example.gamehub.features.screamosaur.model.GameState
import com.example.gamehub.features.screamosaur.model.Obstacle
import com.example.gamehub.features.screamosaur.model.ScreamOSaurViewModel
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

    val dinosaurSizeDp = 50.dp
    val gameHeightDp = 200.dp
    val groundHeightDp = 15.dp
    val dinosaurVisualXOffsetDp = 40.dp
    val jumpMagnitudeDp = 120.dp

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
        Text(
            text = "Score: ${uiState.score}",
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
                val sunRadius = 25.dp.toPx()
                val sunCenter = Offset(size.width * 0.15f, size.height * 0.15f)
                drawCircle(color = Color(0xFFFFD54F), radius = sunRadius, center = sunCenter)
                val numRays = 8
                for (i in 0 until numRays) {
                    val angle = Math.toRadians((i * (360.0 / numRays)) + 15.0)
                    val rayStartOffset = sunRadius * 1.1f
                    val rayEndOffset = sunRadius * 1.6f
                    drawLine(
                        color = Color(0xFFFFB74D).copy(alpha = 0.7f),
                        start = Offset(sunCenter.x + kotlin.math.cos(angle).toFloat() * rayStartOffset, sunCenter.y + kotlin.math.sin(angle).toFloat() * rayStartOffset),
                        end = Offset(sunCenter.x + kotlin.math.cos(angle).toFloat() * rayEndOffset, sunCenter.y + kotlin.math.sin(angle).toFloat() * rayEndOffset),
                        strokeWidth = 2.dp.toPx()
                    )
                }

                val groundBaseColor = Color(0xFFBCAAA4)
                val groundDarkerColor = Color(0xFF8D6E63)
                val groundTopY = uiState.gameHeightPx - uiState.groundHeightPx
                drawRect(
                    color = groundBaseColor,
                    topLeft = Offset(0f, groundTopY),
                    size = Size(size.width, uiState.groundHeightPx)
                )
                val numGroundPatches = 30
                for (i in 0..numGroundPatches) {
                    val patchY = groundTopY + (Random.nextFloat() * (uiState.groundHeightPx - 4.dp.toPx())) + 2.dp.toPx()
                    val patchX = Random.nextFloat() * size.width
                    val patchWidth = Random.nextFloat() * 20.dp.toPx() + 10.dp.toPx()
                    val patchHeight = Random.nextFloat() * 2.dp.toPx() + 1.dp.toPx()
                    drawRect(
                        color = groundDarkerColor.copy(alpha = Random.nextFloat() * 0.3f + 0.2f),
                        topLeft = Offset(patchX, patchY),
                        size = Size(patchWidth, patchHeight)
                    )
                }

                uiState.obstacles.forEach { obstacle ->
                    val cactusColor = Color(0xFF4CAF50)
                    val cactusDarkerColor = Color(0xFF388E3C)

                    val obX = obstacle.xPosition
                    val obY = uiState.gameHeightPx - uiState.groundHeightPx - obstacle.height
                    val obW = obstacle.width
                    val obH = obstacle.height

                    val bodyW = obW * 0.4f
                    val bodyH = obH
                    val bodyX = obX + (obW - bodyW) / 2
                    val bodyBaseY = obY

                    drawRoundRect(
                        color = cactusColor,
                        topLeft = Offset(bodyX, bodyBaseY),
                        size = Size(bodyW, bodyH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(bodyW * 0.25f)
                    )

                    if (obH > 30.dp.toPx()) {
                        val armBaseHeight = bodyH * 0.4f
                        val armBaseWidth = bodyW * 0.8f
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

                    if (obH > 40.dp.toPx() && obW > 20.dp.toPx()) {
                        val armBaseHeight = bodyH * 0.4f
                        val armBaseWidth = bodyW * 0.8f
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

                    val numLines = (bodyW / 6.dp.toPx()).toInt().coerceIn(2, 5)
                    if (numLines > 1) {
                        for (i in 1 until numLines) {
                            val lineX = bodyX + (bodyW / numLines) * i
                            drawLine(color = cactusDarkerColor, start = Offset(lineX, bodyBaseY + bodyH * 0.05f), end = Offset(lineX, bodyBaseY + bodyH * 0.95f), strokeWidth = 1.dp.toPx())
                        }
                    }
                }
            }

            val currentDinoTopYDp = with(density) {
                (uiState.dinoTopYOnGroundPx - (uiState.jumpAnimValue * uiState.jumpMagnitudePx)).toDp()
            }
            Box(
                modifier = Modifier
                    .size(dinosaurSizeDp)
                    .offset(x = dinosaurVisualXOffsetDp, y = currentDinoTopYDp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRoundRect(color = Color(0xFFF44336), topLeft = Offset(size.width * 0.15f, size.height * 0.2f), size = Size(size.width * 0.7f, size.height * 0.6f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.1f))
                    drawRoundRect(color = Color(0xFFF44336), topLeft = Offset(size.width * 0.55f, size.height * 0.05f), size = Size(size.width * 0.4f, size.height * 0.4f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.1f))
                    drawCircle(color = Color.White, center = Offset(size.width * 0.75f, size.height * 0.25f), radius = size.width * 0.1f)
                    drawCircle(color = Color.Black, center = Offset(size.width * 0.78f, size.height * 0.27f), radius = size.width * 0.05f)
                    val legWidth = size.width * 0.15f
                    val legHeight = size.height * 0.35f
                    val legYPos = size.height * 0.75f
                    val legOffsetFactor = when {
                        uiState.isJumping -> 0.15f
                        else -> when (uiState.runningAnimState) {
                            0 -> 0.0f
                            1 -> 0.05f
                            2 -> 0.1f
                            else -> 0.05f
                        }
                    }
                    drawRoundRect(color = Color(0xFFD32F2F), topLeft = Offset(size.width * (0.3f - legOffsetFactor), legYPos), size = Size(legWidth, legHeight), cornerRadius = androidx.compose.ui.geometry.CornerRadius(legWidth * 0.3f))
                    drawRoundRect(color = Color(0xFFD32F2F), topLeft = Offset(size.width * (0.55f + legOffsetFactor), legYPos), size = Size(legWidth, legHeight), cornerRadius = androidx.compose.ui.geometry.CornerRadius(legWidth * 0.3f))
                    drawRoundRect(color = Color(0xFFF44336), topLeft = Offset(size.width * 0.0f, size.height * 0.4f), size = Size(size.width * 0.3f, size.height * 0.2f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.05f))
                }
            }

            when (uiState.gameState) {
                GameState.READY -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("ROAR to make the dinosaur jump!", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.startGame() }) { Text("Start Game") }
                    }
                }
                GameState.GAME_OVER -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Game Over!", fontSize = 24.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                        Text("Score: ${uiState.score}", fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.startGame() }) { Text("Play Again") }
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
        amplitude == 0 -> MaterialTheme.colorScheme.surfaceVariant
        normalizedAmplitude < 0.4f -> Color(0xFF4CAF50)
        normalizedAmplitude < 0.75f -> Color(0xFFFFEB3B)
        else -> Color(0xFFF44336)
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

