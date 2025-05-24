package com.example.gamehub.features.ohpardon.ui

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gamehub.features.ohpardon.OhPardonViewModel
import com.example.gamehub.features.ohpardon.classes.OhPardonViewModelFactory
import com.google.firebase.firestore.FirebaseFirestore


enum class CellType {
    EMPTY, PATH, HOME, GOAL, ENTRY
}

data class BoardCell(
    val x: Int,
    val y: Int,
    val type: CellType,
    val pawn: PawnForUI? = null,
    val color: Color? = null
)

data class PawnForUI(val color: Color, val id: Int)


@Composable
fun OhPardonScreen(
    navController: NavController,
    code: String,
    userName: String
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val firestore = FirebaseFirestore.getInstance()


    val viewModel: OhPardonViewModel = viewModel(
        factory = OhPardonViewModelFactory(application, code, userName)
    )

    val gameRoom by viewModel.gameRoom.collectAsState()
    val currentDiceRoll = gameRoom?.gameState?.diceRoll
    val lifecycleOwner = LocalLifecycleOwner.current

    var selectedPawnId by remember { mutableStateOf<Int?>(null) }

    val toastMessage by viewModel.toastMessage.collectAsState()
    val showVictoryDialog = remember { mutableStateOf(false) }
    val winnerName = remember { mutableStateOf("") }

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


    fun colorToString(color: Color): String {
        return when (color) {
            Color.Red -> "Red"
            Color.Green -> "Green"
            Color.Blue -> "Blue"
            Color.Yellow -> "Yellow"
            else -> "Unknown"
        }
    }

    val currentPlayer = gameRoom?.players?.find { it.name == userName }
    val isHost = gameRoom?.hostUid == currentPlayer?.uid

    if (showVictoryDialog.value) {
        AlertDialog(
            onDismissRequest = { /* Prevent dismissal on tap outside */ },
            title = { Text(text = "🎉 Game Over") },
            text = { Text(text = "${winnerName.value} has won the game!") },
            confirmButton = {
                TextButton(onClick = {
                    // Host deletes room or navigates away
                    showVictoryDialog.value = false
                    // Call delete if host, or popBackStack if client
                    if (isHost) {
                        firestore.collection("rooms").document(code)
                            .delete()
                            .addOnSuccessListener {
                                navController.popBackStack()
                            }
                    } else {
                        navController.popBackStack()
                    }
                }) {
                    Text(if (isHost) "Close Room" else "Exit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVictoryDialog.value = false }) {
                    Text("Stay")
                }
            }
        )
    }


    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            if (gameRoom != null) {
                val board = viewModel.getBoardForUI(gameRoom!!.players)

                GameBoard(board = board, onPawnClick = { pawnId ->
                    selectedPawnId = pawnId
                })

                Spacer(modifier = Modifier.height(16.dp))

                val isMyTurn = gameRoom!!.gameState.currentTurnUid == currentPlayer?.uid

                if (isMyTurn) {
                    // Dice roll button
                    if (currentDiceRoll == null) {
                        Button(
                            onClick = { viewModel.attemptRollDice(userName) },
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text("Roll Dice")
                        }
                    }

                    // Move button
                    if (currentDiceRoll != null && selectedPawnId != null) {
                        Button(
                            onClick = {
                                viewModel.attemptMovePawn(gameRoom!!.gameState.currentTurnUid, selectedPawnId.toString())
                                selectedPawnId = null
                            },
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text("Move Selected Pawn")
                        }
                    }

                    // Skip turn
                    Button(
                        onClick = {
                            viewModel.skipTurn(userName)
                            selectedPawnId = null
                        },
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text("Skip Turn")
                    }

                    val currentPlayer = gameRoom!!.players.find { it.uid == gameRoom!!.gameState.currentTurnUid }

                    if (currentPlayer != null) {
                        Text(
                            text = "It's ${currentPlayer.name}'s turn!",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        gameRoom!!.gameState.diceRoll?.let {
                            Text(
                                text = "${currentPlayer.name} rolled a $it!",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }
                    }
                }
            } else {
                CircularProgressIndicator()
                Text("Loading game data...")
            }
        }
    }

}

@Composable
fun GameBoard(board: List<List<BoardCell>>, onPawnClick: (Int) -> Unit) {
    Column {
        board.forEach { row ->
            Row {
                row.forEach { cell ->
                    BoardCellView(cell = cell, onPawnClick = onPawnClick)
                }
            }
        }
    }
}

@Composable
fun BoardCellView(cell: BoardCell, onPawnClick: (Int) -> Unit) {
    val backgroundColor = when (cell.type) {
        CellType.EMPTY -> Color.LightGray
        CellType.PATH -> cell.color ?: Color.White
        CellType.HOME -> cell.color ?: Color.Cyan
        CellType.GOAL -> cell.color ?: Color.Yellow
        CellType.ENTRY -> cell.color ?: Color.Black
    }

    Box(
        modifier = Modifier
            .size(32.dp)
            .border(1.dp, Color.Black)
            .background(backgroundColor)
            .clickable(enabled = cell.pawn != null) {
                cell.pawn?.let { onPawnClick(it.id) }
            },
        contentAlignment = Alignment.Center
    ) {
        cell.pawn?.let {
            Canvas(modifier = Modifier.size(20.dp)) {
                drawCircle(color = it.color)
            }
        }
    }
}

