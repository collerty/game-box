package com.example.gamehub.features.spy.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.gamehub.features.spy.ui.components.PlayerCard
import com.example.gamehub.features.spy.ui.components.SettingsPanel
import com.example.gamehub.features.spy.ui.components.TimerPanel
import com.example.gamehub.features.spy.ui.components.GameOverDialog

@Composable
fun SpyGameScreen(
    navController: NavController? = null,
    viewModel: SpyGameViewModel = viewModel()
) {
    val playerCardInfo by viewModel.playerCardInfo.observeAsState()
    val settingsSummary by viewModel.settingsSummary.observeAsState()
    val timer by viewModel.timer.observeAsState()
    val gameOver by viewModel.gameOver.observeAsState(false)
    val locations by viewModel.locations.observeAsState(emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SettingsPanel(
            summary = settingsSummary,
            onPlayersChange = { viewModel.updateNumberOfPlayers(it) },
            onSpiesChange = { viewModel.updateNumberOfSpies(it) },
            onTimerChange = { viewModel.updateTimerMinutes(it) }
        )
        Spacer(modifier = Modifier.height(16.dp))
        playerCardInfo?.let { info ->
            PlayerCard(
                info = info,
                onRevealRole = { viewModel.revealRole() },
                onAdvancePlayer = { viewModel.advancePlayer() }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        TimerPanel(timer = timer)
        if (gameOver) {
            GameOverDialog(onRestart = { viewModel.resetGame() })
        }
    }
} 