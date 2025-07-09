package com.example.gamehub.features.spy.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.gamehub.R
import com.example.gamehub.features.spy.model.LocationManager
import com.example.gamehub.features.spy.ui.components.PlayerCard
import com.example.gamehub.features.spy.ui.components.SettingsPanel
import com.example.gamehub.features.spy.ui.components.TimerPanel
import com.example.gamehub.features.spy.ui.components.GameOverDialog
import com.example.gamehub.ui.SpriteMenuButton
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

@Composable
fun SpyGameScreen(
    navController: NavController? = null
) {
    val context = LocalContext.current
    val locationManager = androidx.compose.runtime.remember(context) { LocationManager(context) }
    val factory = androidx.compose.runtime.remember(locationManager) { SpyGameViewModelFactory(locationManager) }
    val viewModel: SpyGameViewModel = viewModel(factory = factory)

    val gamePhase by viewModel.gamePhase.observeAsState(GamePhase.SETTINGS)
    val playerCardInfo by viewModel.playerCardInfo.observeAsState()
    val settingsSummary by viewModel.settingsSummary.observeAsState()
    val timer by viewModel.timer.observeAsState()
    val gameOver by viewModel.gameOver.observeAsState(false)
    val locations by viewModel.locations.observeAsState(emptyList())

    Box(modifier = Modifier.fillMaxSize()) {
        // Background image
        Image(
            painter = painterResource(id = R.drawable.spy_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // Semi-transparent overlay for better text visibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        )
        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (gamePhase) {
                GamePhase.SETTINGS -> {
                    SettingsPanel(
                        summary = settingsSummary,
                        onPlayersChange = { viewModel.updateNumberOfPlayers(it) },
                        onSpiesChange = { viewModel.updateNumberOfSpies(it) },
                        onTimerChange = { viewModel.updateTimerMinutes(it) }
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    SpriteMenuButton(text = "Start Game", onClick = { viewModel.startGame() })
                }
                GamePhase.REVEAL -> {
                    playerCardInfo?.let { info ->
                        PlayerCard(
                            info = info,
                            onRevealRole = { viewModel.revealRole() },
                            onAdvancePlayer = { viewModel.advancePlayer() }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        androidx.compose.material3.Text(
                            text = "Player ${viewModel.currentPlayerIndex + 1} of ${viewModel.numberOfPlayers}",
                            color = Color.White
                        )
                    }
                }
                GamePhase.TIMER -> {
                    val context = LocalContext.current
                    TimerPanel(timer = timer, onVibrationEvent = {
                        val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val vm = context.getSystemService(VibratorManager::class.java)!!
                            vm.defaultVibrator
                        } else {
                            @Suppress("DEPRECATION")
                            context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val effect = VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE)
                            vibrator.vibrate(effect)
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(300)
                        }
                    })
                }
                GamePhase.GAME_OVER -> {
                    GameOverDialog(onRestart = { viewModel.resetGame() })
                }
            }
        }
    }
} 