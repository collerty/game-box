package com.example.gamehub.features.ohpardon.ui

import android.app.Application
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.gamehub.R
import com.example.gamehub.features.ohpardon.OhPardonViewModel
import com.example.gamehub.features.ohpardon.classes.OhPardonViewModelFactory
import com.example.gamehub.features.ohpardon.classes.SoundManager
import com.example.gamehub.features.ohpardon.classes.VibrationManager
import com.example.gamehub.features.ohpardon.models.UiEvent
import com.example.gamehub.features.ohpardon.ui.components.*
import com.example.gamehub.navigation.NavRoutes
import com.google.firebase.firestore.FirebaseFirestore

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun OhPardonScreen(
    navController: NavController,
    code: String,
    userName: String
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val firestore = FirebaseFirestore.getInstance()

    val soundManager = remember { SoundManager(context) }
    val vibrationManager = remember { VibrationManager(context) }
    val screenWidthDp = LocalConfiguration.current.screenWidthDp

    val viewModel: OhPardonViewModel = viewModel(
        factory = OhPardonViewModelFactory(application, code, userName)
    )

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                UiEvent.PlayMoveSound -> soundManager.playSound("move_self")
                UiEvent.PlayCaptureSound -> soundManager.playSound("capture")
                UiEvent.PlayIllegalMoveSound -> soundManager.playSound("illegal")
                UiEvent.PlayDiceRollSound -> soundManager.playSound("diceroll")
                UiEvent.Vibrate -> vibrationManager.vibrate()
            }
        }
    }

    val gameRoom by viewModel.gameRoom.collectAsState()
    val currentDiceRoll = gameRoom?.gameState?.diceRoll
    val lifecycleOwner = LocalLifecycleOwner.current

    var selectedPawnId by remember { mutableStateOf<Int?>(null) }

    val toastMessage by viewModel.toastMessage.collectAsState()
    val showVictoryDialog = remember { mutableStateOf(false) }
    val winnerName = remember { mutableStateOf("") }
    val showExitDialog = remember { mutableStateOf(false) }

    // Add BackHandler to intercept back button presses
    BackHandler {
        showExitDialog.value = true
    }

    LaunchedEffect(gameRoom?.status) {
        if (gameRoom?.status == "over" && gameRoom?.gameState?.gameResult?.contains("wins") == true) {
            winnerName.value = gameRoom?.gameState?.gameResult?.removeSuffix(" wins!") ?: ""
            showVictoryDialog.value = true
        }
    }

    // Debugging logs
    LaunchedEffect(gameRoom) {
        Log.d("OhPardonScreen", "GameRoom updated: $gameRoom")
        gameRoom?.let {
            Log.d("OhPardonScreen", "Current turn UID: ${it.gameState.currentTurnUid}")
            Log.d("OhPardonScreen", "Players count: ${it.players.size}")
        }
    }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToastMessage()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.registerShakeListener()
                Lifecycle.Event.ON_PAUSE -> viewModel.unregisterShakeListener()
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            viewModel.unregisterShakeListener()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val currentPlayer = gameRoom?.players?.find { it.name == userName }
    val isHost = gameRoom?.hostUid == currentPlayer?.uid
    val pixelFont = FontFamily(Font(R.font.gamebox_font))

    // Game Dialogs
    GameDialogs(
        showVictoryDialog = showVictoryDialog,
        showExitDialog = showExitDialog,
        winnerName = winnerName.value,
        isHost = isHost,
        onVictoryConfirm = {
            if (isHost) {
                firestore.collection("rooms").document(code)
                    .delete()
                    .addOnSuccessListener { navController.popBackStack() }
            } else {
                navController.popBackStack()
            }
        },
        onVictoryDismiss = { },
        onExitConfirm = {
            if (isHost) {
                firestore.collection("rooms").document(code)
                    .delete()
                    .addOnSuccessListener { navController.popBackStack() }
            } else {
                val roomRef = firestore.collection("rooms").document(code)
                roomRef.get().addOnSuccessListener { document ->
                    val players = document.get("players") as? List<Map<String, Any>>
                    roomRef.update("gameState.ohpardon.diceRoll", null)
                    roomRef.update("gameState.ohpardon.currentPlayer", gameRoom?.hostUid)
                    val playerToRemove = players?.find { it["name"] == userName }
                    if (playerToRemove != null) {
                        roomRef.update("players", com.google.firebase.firestore.FieldValue.arrayRemove(playerToRemove))
                            .addOnSuccessListener { navController.navigate(NavRoutes.GAMES_LIST) }
                    } else {
                        navController.navigate(NavRoutes.GAMES_LIST)
                    }
                }
            }
        },
        onExitDismiss = { }
    )

    Scaffold { padding ->
        val isTablet = screenWidthDp > 600

        GameBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(if (isTablet) 32.dp else 16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                if (gameRoom != null) {
                    val board = viewModel.getBoardForUI(gameRoom!!.players)

                    // Centered GameBoard
                    Box(contentAlignment = Alignment.Center) {
                        GameBoard(
                            board = board,
                            onPawnClick = { selectedPawnId = it },
                            currentPlayer = currentPlayer,
                            selectedPawnId = selectedPawnId
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val currentTurnPlayer = gameRoom!!.players.find { it.uid == gameRoom!!.gameState.currentTurnUid }

                    PlayerTurnInfo(
                        currentTurnPlayer = currentTurnPlayer,
                        currentDiceRoll = currentDiceRoll,
                        pixelFont = pixelFont
                    )

                    val isMyTurn = gameRoom!!.gameState.currentTurnUid == currentPlayer?.uid

                    GameControls(
                        isMyTurn = isMyTurn,
                        currentDiceRoll = currentDiceRoll,
                        selectedPawnId = selectedPawnId,
                        pixelFont = pixelFont,
                        onRollDice = { viewModel.attemptRollDice(userName) },
                        onMovePawn = {
                            viewModel.attemptMovePawn(
                                gameRoom!!.gameState.currentTurnUid,
                                selectedPawnId.toString()
                            )
                            selectedPawnId = null
                        },
                        onSkipTurn = {
                            viewModel.skipTurn(userName)
                            selectedPawnId = null
                        }
                    )

                } else {
                    CircularProgressIndicator()
                    Text(
                        "Loading game data...",
                        style = TextStyle(
                            fontFamily = pixelFont,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    )
                }
            }
        }
    }
}