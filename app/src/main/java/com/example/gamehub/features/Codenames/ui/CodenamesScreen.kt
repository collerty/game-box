package com.example.gamehub.features.codenames.ui

import android.util.Log
import android.widget.Toast
import android.view.WindowManager
import android.view.View
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.gamehub.R
import com.example.gamehub.features.codenames.model.CardColor
import com.example.gamehub.navigation.NavRoutes
import com.example.gamehub.ui.SpriteMenuButton
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.toObject
import com.example.gamehub.ui.theme.ArcadeClassic
import com.google.firebase.auth.FirebaseAuth
import com.example.gamehub.audio.SoundManager
import com.example.gamehub.repository.interfaces.ICodenamesRepository
import com.example.gamehub.features.codenames.model.CodenamesGameState
import com.example.gamehub.features.codenames.model.Clue
import com.example.gamehub.features.codenames.ui.components.TeamPanel
import com.example.gamehub.features.codenames.ui.components.CodenamesBoard
import com.example.gamehub.features.codenames.ui.components.ClueInput
import com.example.gamehub.features.codenames.ui.components.GameOverDialog
import com.example.gamehub.features.codenames.ui.components.BackgroundImage
import com.example.gamehub.features.codenames.ui.components.PhaseStatusText
import com.example.gamehub.features.codenames.ui.components.HideSystemBarsEffect
import com.example.gamehub.features.codenames.ui.components.DebugLogger
import com.example.gamehub.features.codenames.ui.components.VibrationEffectHandler
import com.example.gamehub.features.codenames.ui.components.CodenamesBottomControls
import android.app.Activity
import android.content.pm.ActivityInfo

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
                        // Replicate the previous card click logic here
                        val cards = gameState?.cards?.toMutableList() ?: return@CodenamesBoard
                        val card = cards[cardIndex]
                        if (!card.isRevealed) {
                            cards[cardIndex] = card.copy(isRevealed = true)
                            // Update the game state as before
                            repository.updateGameState(
                                roomId,
                                CodenamesGameState(
                                    currentTurn = currentTurn,
                                    currentTeam = currentTeam,
                                    isMasterPhase = isMasterPhase,
                                    redWordsRemaining = redWordsRemaining,
                                    blueWordsRemaining = blueWordsRemaining,
                                    winner = winner,
                                    clues = redClues + blueClues,
                                    cards = cards,
                                    currentGuardingWordCount = 0,
                                    guessesRemaining = 0
                                ),
                                onSuccess = {},
                                onError = { e -> Log.e("Codenames", "Error updating Firestore after card click: $e") }
                            )
                        }
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
                                // Basic validation for clue format (e.g., "WORD X")
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
                            repository.getGameState(roomId, onSuccess = { state ->
                                val currentClues: List<Clue> = state?.clues ?: emptyList()
                                val newClue = Clue(clueText, currentTeam)
                                repository.updateGameState(
                                    roomId,
                                    CodenamesGameState(
                                        currentTurn = currentTurn,
                                        currentTeam = currentTeam,
                                        isMasterPhase = false,
                                        redWordsRemaining = redWordsRemaining,
                                        blueWordsRemaining = blueWordsRemaining,
                                        winner = winner,
                                        clues = currentClues + newClue,
                                        cards = gameState?.cards ?: emptyList(),
                                        currentGuardingWordCount = count,
                                        guessesRemaining = count + 1
                                    ),
                                    onSuccess = {
                                        if (currentTeam == "RED") viewModel.redMasterClue = "" else viewModel.blueMasterClue = ""
                                                        Toast.makeText(context, "Clue submitted!", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { e ->
                                                        Log.e("Codenames", "Error updating Firestore after clue submission: $e")
                                                        Toast.makeText(context, "Failed to submit clue: ${e.message}", Toast.LENGTH_LONG).show()
                                                    }
                                )
                            }, onError = { e ->
                                Log.e("Codenames", "Error getting game state to submit clue: $e")
                                Toast.makeText(context, "Fai    led to get game state: ${e.message}", Toast.LENGTH_LONG).show()
                            })
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