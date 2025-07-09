package com.example.gamehub.features.codenames.ui

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.gamehub.R
import com.example.gamehub.navigation.NavRoutes
import com.example.gamehub.repository.interfaces.ICodenamesRepository
import com.example.gamehub.features.codenames.model.Clue
import com.example.gamehub.features.codenames.ui.components.TeamPanel
import com.example.gamehub.features.codenames.ui.components.CodenamesBoard
import com.example.gamehub.features.codenames.ui.components.GameOverDialog
import com.example.gamehub.features.codenames.ui.components.BackgroundImage
import com.example.gamehub.features.codenames.ui.components.PhaseStatusText
import com.example.gamehub.features.codenames.ui.components.HideSystemBarsEffect
import com.example.gamehub.features.codenames.ui.components.DebugLogger
import com.example.gamehub.features.codenames.ui.components.CodenamesBottomControls
import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.ui.unit.dp

@Composable
fun LockScreenOrientation(orientation: Int) {
    val context = LocalContext.current
    val activity = context as? Activity
    activity?.requestedOrientation = orientation
}

@Composable
fun CodenamesScreen(
    navController: NavController,
    roomId: String,
    isMaster: Boolean,
    masterTeam: String? = null,
    repository: ICodenamesRepository,
    viewModel: CodenamesViewModel = viewModel(factory = CodenamesViewModelFactory(repository))
) {
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
    val context = LocalContext.current
    HideSystemBarsEffect()
    DebugLogger(
        viewModel.currentTurn.collectAsState().value,
        viewModel.isMasterPhase.collectAsState().value,
        viewModel.currentTeam.collectAsState().value,
        viewModel.winner.collectAsState().value
    )
    // VibrationEffectHandler(...) // Use when you want to trigger vibration

    // Start listeners and timer
    LaunchedEffect(roomId) {
        viewModel.startListening(roomId)
    }
    LaunchedEffect(viewModel.isMasterPhase.collectAsState().value, viewModel.currentTeam.collectAsState().value) {
        viewModel.startTimer(
            isMasterPhase = viewModel.isMasterPhase.value,
            currentTeam = viewModel.currentTeam.value,
            roomId = roomId
        )
    }

    val gameState by viewModel.gameState.collectAsState()
    val currentTurn by viewModel.currentTurn.collectAsState()
    val redWordsRemaining by viewModel.redWordsRemaining.collectAsState()
    val blueWordsRemaining by viewModel.blueWordsRemaining.collectAsState()
    val winner by viewModel.winner.collectAsState()
    val isMasterPhase by viewModel.isMasterPhase.collectAsState()
    val currentTeam by viewModel.currentTeam.collectAsState()
    val redClues by viewModel.redClues.collectAsState()
    val blueClues by viewModel.blueClues.collectAsState()
    val timerSeconds by viewModel.timerSeconds.collectAsState()

    // Debug logging for visibility conditions
    LaunchedEffect(isMaster, currentTeam, isMasterPhase, currentTurn) {
        Log.d("CodenamesDebug", """
            Visibility Debug:
            isMaster: $isMaster
            masterTeam: $masterTeam
            isMasterPhase: $isMasterPhase
            currentTeam: $currentTeam
            currentTurn: $currentTurn
            Red team conditions: ${isMaster && masterTeam?.uppercase() == "RED" && isMasterPhase && currentTeam == "RED"}
            Blue team conditions: ${isMaster && masterTeam?.uppercase() == "BLUE" && isMasterPhase && currentTeam == "BLUE"}
            Raw values:
            masterTeam from intent: $masterTeam
            currentMasterTeam: $masterTeam
            currentTeam: $currentTeam
        """.trimIndent())
    }

    // Replace the Game Over Screen logic with GameOverDialog:
    GameOverDialog(
        winner = winner,
        onReturnToLobby = {
            navController.navigate(NavRoutes.LOBBY_MENU.replace("{gameId}", "codenames")) {
                popUpTo(NavRoutes.LOBBY_MENU.replace("{gameId}", "codenames")) { inclusive = true }
            }
        }
    )
    if (winner != null) return

    // Main game layout with background
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Replace the background image logic with BackgroundImage:
        BackgroundImage(
            resId = R.drawable.stars_bg,
            contentDescription = "Stars Background",
            modifier = Modifier.fillMaxSize()
        )

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Top section (scores and grid)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Allows this row to take up available space, pushing controls to bottom
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Panel (Red Team/Log)
                TeamPanel(
                    teamName = "RED",
                    wordsRemaining = redWordsRemaining,
                    clues = redClues.map { it.word },
                    timerSeconds = if (currentTurn == "RED") timerSeconds else null,
                    isCurrentTurn = currentTurn == "RED",
                    modifier = Modifier.weight(0.7f).fillMaxHeight().padding(end = 4.dp)
                )

                // Center Grid (Cards)
                CodenamesBoard(
                    cards = gameState?.cards ?: emptyList(),
                    isMaster = isMaster,
                    isMasterPhase = isMasterPhase,
                    currentTeam = currentTeam,
                    currentTurn = currentTurn,
                    winner = winner,
                    onCardClick = { cardIndex ->
                        viewModel.makeGuess(roomId, cardIndex)
                    },
                    modifier = Modifier.weight(4f).fillMaxHeight().padding(horizontal = 4.dp)
                )

                // Right Panel (Blue Team/Clue History)
                TeamPanel(
                    teamName = "BLUE",
                    wordsRemaining = blueWordsRemaining,
                    clues = blueClues.map { it.word },
                    timerSeconds = if (currentTurn == "BLUE") timerSeconds else null,
                    isCurrentTurn = currentTurn == "BLUE",
                    modifier = Modifier.weight(0.7f).fillMaxHeight().padding(start = 4.dp)
                )
            }

            // Bottom section for global controls/status (Master Input, General Status)
            CodenamesBottomControls(
                isMaster = isMaster,
                isMasterPhase = isMasterPhase,
                currentTeam = currentTeam,
                masterTeam = masterTeam,
                clueText = if (currentTeam == "RED") viewModel.redMasterClue else viewModel.blueMasterClue,
                onClueTextChange = {
                    if (currentTeam == "RED") viewModel.redMasterClue = it else viewModel.blueMasterClue = it
                },
                onSubmitClue = {
                    val clueText = if (currentTeam == "RED") viewModel.redMasterClue else viewModel.blueMasterClue
                    val parts = clueText.split(" ")
                    if (parts.size == 2 && parts[1].toIntOrNull() != null) {
                        val word = parts[0]
                        val count = parts[1].toInt()
                        if (count < 0 || count > 9) {
                            Toast.makeText(context, "Clue number must be between 0 and 9", Toast.LENGTH_SHORT).show()
                            Log.e("Codenames", "Invalid clue number: $count")
                            return@CodenamesBottomControls
                        }
                        if (word.isNotEmpty()) {
                            val newClue = Clue(word, currentTeam)
                            viewModel.submitClue(roomId, newClue, count)
                            if (currentTeam == "RED") viewModel.redMasterClue = "" else viewModel.blueMasterClue = ""
                            Toast.makeText(context, "Clue submitted!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Clue word cannot be empty", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Invalid clue format. Use 'WORD NUMBER'", Toast.LENGTH_SHORT).show()
                        Log.e("Codenames", "Invalid clue format. Please use 'WORD NUMBER'. Input: $clueText")
                    }
                },
                phaseStatus = {
                    PhaseStatusText(
                        currentTurn = currentTurn,
                        isMasterPhase = isMasterPhase,
                        modifier = Modifier
                    )
                },
                modifier = Modifier
            )
        }
    }
}