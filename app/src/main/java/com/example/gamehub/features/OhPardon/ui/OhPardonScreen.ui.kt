package com.example.gamehub.features.ohpardon.ui

import android.app.Application
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gamehub.features.ohpardon.OhPardonViewModel
import com.example.gamehub.features.ohpardon.classes.OhPardonViewModelFactory
import com.google.firebase.firestore.FirebaseFirestore
import com.example.gamehub.R
import com.example.gamehub.features.ohpardon.classes.SoundManager
import com.example.gamehub.features.ohpardon.classes.VibrationManager
import com.example.gamehub.features.ohpardon.models.Player
import com.example.gamehub.features.ohpardon.models.UiEvent

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


    val pixelFont = FontFamily(Font(R.font.gamebox_font)) // Replace with your actual font

    Scaffold { padding ->
        val isTablet = screenWidthDp > 600

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Background image
            Image(
                painter = painterResource(id = R.drawable.ohpardon_background),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Overlay to make text readable
            Box(modifier = Modifier
                .fillMaxSize()
                .background(Color(0x99000000)) // semi-transparent black
            )

            // Foreground content
            Column(
                modifier = Modifier
                    .fillMaxSize()
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
                            currentPlayer = currentPlayer
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val currentTurnPlayer = gameRoom!!.players.find { it.uid == gameRoom!!.gameState.currentTurnUid }

                    if (currentTurnPlayer != null) {
                        Text(
                            text = "It's ${currentTurnPlayer.name}'s turn!",
                            style = TextStyle(fontFamily = pixelFont, fontSize = 20.sp, color = Color.White),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        currentDiceRoll?.let {
                            Text(
                                text = "${currentTurnPlayer.name} rolled a $it!",
                                style = TextStyle(fontFamily = pixelFont, fontSize = 16.sp, color = Color.White),
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }
                    }

                    val isMyTurn = gameRoom!!.gameState.currentTurnUid == currentPlayer?.uid

                    val buttonModifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .border(1.dp, Color.Black)
                        .background(Color.White)

                    val buttonTextStyle = TextStyle(
                        fontFamily = pixelFont,
                        fontSize = 16.sp,
                        color = Color.Black
                    )

                    if (isMyTurn) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth(0.8f) // optional width constraint
                        ) {
                            // Roll Dice Button with Dice Image
                            if (currentDiceRoll == null) {
                                Button(
                                    onClick = { viewModel.attemptRollDice(userName) },
                                    modifier = buttonModifier,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                    shape = RectangleShape,
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.dice_icon),
                                        contentDescription = "Dice",
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Roll Dice", style = buttonTextStyle)
                                }
                            }

                            // Move Selected Pawn
                            if (currentDiceRoll != null && selectedPawnId != null) {
                                Button(
                                    onClick = {
                                        viewModel.attemptMovePawn(gameRoom!!.gameState.currentTurnUid, selectedPawnId.toString())
                                        selectedPawnId = null
                                    },
                                    modifier = buttonModifier,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                    shape = RectangleShape
                                ) {
                                    Text("Move Selected Pawn", style = buttonTextStyle)
                                }
                            }

                            // Skip Turn
                            Button(
                                onClick = {
                                    viewModel.skipTurn(userName)
                                    selectedPawnId = null
                                },
                                modifier = buttonModifier,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                shape = RectangleShape
                            ) {
                                Text("Skip Turn", style = buttonTextStyle)
                            }
                        }
                    }

                } else {
                    CircularProgressIndicator()
                    Text(
                        "Loading game data...",
                        style = TextStyle(fontFamily = pixelFont, fontSize = 16.sp, color = Color.White)
                    )
                }
            }
        }
    }
}

@Composable
fun GameBoard(
    board: List<List<BoardCell>>,
    onPawnClick: (Int) -> Unit,
    currentPlayer: Player?
) {
    Column {
        board.forEach { row ->
            Row {
                row.forEach { cell ->
                    BoardCellView(cell = cell, currentPlayer = currentPlayer, onPawnClick = onPawnClick)
                }
            }
        }
    }
}

@Composable
fun getPawnImageRes(color: Color?): Int {
    return when (color) {
        Color.Red -> R.drawable.pawn_red
        Color.Blue -> R.drawable.pawn_blue
        Color.Green -> R.drawable.pawn_green
        Color.Yellow -> R.drawable.pawn_yellow
        else -> R.drawable.pawn_default // fallback image
    }
}


@Composable
fun BoardCellView(cell: BoardCell, currentPlayer: Player?, onPawnClick: (Int) -> Unit) {
    val isMyPawn = cell.pawn?.color == currentPlayer?.color
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val cellSize = screenWidth / 12 // or dynamically based on board size

    val backgroundColor = when (cell.type) {
        CellType.EMPTY -> Color.LightGray
        CellType.PATH -> cell.color ?: Color.White
        CellType.HOME -> cell.color ?: Color.Cyan
        CellType.GOAL -> cell.color ?: Color.Yellow
        CellType.ENTRY -> cell.color ?: Color.Black
    }

    Box(
        modifier = Modifier
            .size(cellSize)
            .border(1.dp, Color.Black)
            .background(backgroundColor)
            .clickable(enabled = isMyPawn) {
                cell.pawn?.let { onPawnClick(it.id) }
            },
        contentAlignment = Alignment.Center
    ) {
        cell.pawn?.let {
            Box(
                modifier = Modifier
                    .size(cellSize)
                    .background(Color.LightGray, shape = CircleShape)
                    .border(1.dp, Color.DarkGray, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = getPawnImageRes(it.color)),
                    contentDescription = "Pawn",
                    modifier = Modifier.size(cellSize * 0.85f)
                )
            }

        }

    }
}

