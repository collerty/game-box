package com.example.gamehub.features.ScreamOSaur.ui

import android.Manifest
import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gamehub.features.ScreamOSaur.model.GameState
import com.example.gamehub.features.ScreamOSaur.model.ScreamOSaurViewModel
import com.example.gamehub.features.ScreamOSaur.ui.composables.GameCanvas
import com.example.gamehub.features.ScreamOSaur.ui.composables.GameOverlay

@Composable
fun ScreamosaurScreen() {
    val context = LocalContext.current
    val viewModel: ScreamOSaurViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    var showPermissionDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                viewModel.updateAudioPermissionState(true)
            } else {
                showPermissionDialog = true
            }
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
                PermissionRequest {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        }
    }

    if (showPermissionDialog && uiState.hasAudioPermission != true) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Microphone Access Needed") },
            text = { Text("This game needs microphone access to hear your roar and make the dinosaur jump!") },
            confirmButton = {
                Button(
                    onClick = {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        showPermissionDialog = false
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

@Composable
private fun GameContent(viewModel: ScreamOSaurViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val density = LocalDensity.current

    val jumpAnim = remember { androidx.compose.animation.core.Animatable(0f) }

    LaunchedEffect(Unit) {
        snapshotFlow { jumpAnim.value }
            .collect { value ->
                viewModel.setJumpAnimValue(value)
            }
    }

    LaunchedEffect(uiState.isJumping) {
        if (uiState.isJumping) {
            try {
                jumpAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 700, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
                )
                jumpAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 800, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                )
            } finally {
                viewModel.onJumpAnimationFinished()
            }
        }
    }

    // Define dimensions
    val gameHeightDp = 200.dp
    val groundHeightDp = 15.dp
    val dinosaurSizeDp = 70.dp
    val dinosaurVisualXOffsetDp = 40.dp
    val jumpMagnitudeDp = 140.dp

    // Convert dimensions to pixels and set in ViewModel
    LaunchedEffect(density) {
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

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Score: ${uiState.score}",
            style = MaterialTheme.typography.titleMedium,
            color = Color.Red
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(gameHeightDp)
                .background(Color.White)
        ) {
            GameCanvas(
                state = uiState,
                modifier = Modifier.fillMaxSize()
            )
            GameOverlay(
                gameState = uiState.gameState,
                score = uiState.score,
                onStart = { viewModel.startGame() },
                onRestart = { viewModel.startGame() }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        SoundMeter(amplitude = uiState.currentAmplitude)
        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.gameState == GameState.PLAYING || uiState.gameState == GameState.PAUSED) {
            Button(
                onClick = {
                    if (uiState.gameState == GameState.PLAYING) {
                        viewModel.pauseGame()
                    } else {
                        viewModel.resumeGame()
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text(
                    text = if (uiState.gameState == GameState.PLAYING) "Pause" else "Resume",
                    fontSize = 20.sp,
                    color = Color.White
                )
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
        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Text("Grant Microphone Access", color = Color.White)
        }
    }
}
