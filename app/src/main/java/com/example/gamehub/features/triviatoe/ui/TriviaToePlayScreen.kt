package com.example.gamehub.features.triviatoe.ui

import TriviatoeQuestionScreen
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.gamehub.features.triviatoe.model.*
import com.example.gamehub.features.triviatoe.FirestoreTriviatoeSession
import com.example.gamehub.R
import kotlinx.coroutines.launch
import androidx.navigation.NavOptionsBuilder
import android.media.MediaPlayer
import androidx.annotation.RawRes


@Suppress("UnusedBoxWithConstraintsScope")
@Composable
fun TriviatoePlayScreen(
    session: FirestoreTriviatoeSession,
    playerId: String,
    navController: NavController,
    originalRoomCode: String // <-- Add this
) {
    val scope = rememberCoroutineScope()
    val gameState by session.stateFlow.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var lastCheckedRound by remember { mutableStateOf(-1) }
    var lastAnnouncedRound by remember { mutableStateOf(-1) }

    val votesCount = gameState.rematchVotes?.values?.count { it } ?: 0
    val playersCount = gameState.players.size
    val isHost = playerId == gameState.players.firstOrNull()?.uid // Only host does reset
    val context = LocalContext.current

// Start music service when this screen is enter
    LaunchedEffect(votesCount, playersCount, isHost) {
        if (votesCount == playersCount && playersCount > 1 && isHost) {
            // Only call reset if everyone voted, and only host triggers
            session.tryResetIfAllAgreed()
        }
    }

    LaunchedEffect(gameState.state, gameState.currentRound) {
        if (gameState.state == TriviatoeRoundState.MOVE_1 && gameState.currentRound != lastAnnouncedRound) {
            lastAnnouncedRound = gameState.currentRound
            val firstPlayerName = gameState.players.find { it.uid == gameState.firstToMove }?.name ?: "?"
            snackbarHostState.showSnackbar("$firstPlayerName goes first!")
        }
    }

    val player = gameState.players.find { it.uid == playerId }
    val playerSymbol = player?.symbol // "X" or "O"
    val playerSymbolRes = when (playerSymbol) {
        "X" -> R.drawable.x_icon
        "O" -> R.drawable.o_icon
        else -> R.drawable.x_icon
    }
    val otherPlayer = gameState.players.firstOrNull { it.uid != playerId }
    val otherSymbol = otherPlayer?.symbol
    val otherSymbolRes = when (otherSymbol) {
        "X" -> R.drawable.x_icon
        "O" -> R.drawable.o_icon
        else -> R.drawable.o_icon
    }

    // Track if we've played the win/loss sound this game
    var hasPlayedResultSound by remember { mutableStateOf(false) }

    LaunchedEffect(gameState.state, gameState.winner) {
        if (
            gameState.state == TriviatoeRoundState.FINISHED &&
            !hasPlayedResultSound &&
            gameState.winner != null
        ) {
            hasPlayedResultSound = true
            val isWinner = playerId == gameState.winner
            if (isWinner) {
                playSound(context, R.raw.triviatoe_win)
            } else {
                playSound(context, R.raw.triviatoe_lost)
            }
        }
    }

// Reset the sound flag when the game restarts (new round or rematch)
    LaunchedEffect(gameState.state, gameState.currentRound) {
        if (gameState.state != TriviatoeRoundState.FINISHED) {
            hasPlayedResultSound = false
        }
    }


    val playerName = player?.name ?: ""

    // At the top of your TriviatoePlayScreen composable (before any LaunchedEffect)
    var hasNavigatedToXOAssign by rememberSaveable { mutableStateOf(false) }

    // Store it as a remembered value if you get it from nav args
    val persistentRoomCode = remember { originalRoomCode }

    // Later, in the XO_ASSIGN navigation:
    LaunchedEffect(gameState.state) {
        if (
            gameState.state == TriviatoeRoundState.XO_ASSIGN &&
            !hasNavigatedToXOAssign
        ) {
            hasNavigatedToXOAssign = true
            println("Navigating to XO_ASSIGN with roomCode: $persistentRoomCode")
            navController.navigate(
                "triviatoe/$persistentRoomCode/${playerName}/xo"
            ) {
                launchSingleTop = true
            }
        }
    }

    Box(
        Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.triviatoe_bg1),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        if (gameState.state == TriviatoeRoundState.QUESTION) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                TriviatoeQuestionScreen(
                    session = gameState,
                    playerId = playerId,
                    randomized = gameState.randomized ?: false,
                    onAnswer = { playerAnswer ->
                        scope.launch {
                            session.submitAnswer(
                                playerId,
                                playerAnswer
                            )
                        }
                    },
                    onQuestionResolved = { winnerId, randomized ->
                        scope.launch {
                            session.setFirstToMoveAndAdvance(winnerId, randomized)
                        }
                    }
                )
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(60.dp))

                // ---- GAME BOARD GRID with DYNAMIC border ----
                val gridPercent = 313f / 404f
                BoxWithConstraints(
                    Modifier.align(Alignment.CenterHorizontally)
                ) {
                    val boxSize = minOf(maxWidth, maxHeight)
                    val gridSizeDp = boxSize * gridPercent
                    val cellSize = gridSizeDp / 10

                    Box(
                        modifier = Modifier
                            .size(boxSize)
                            .align(Alignment.Center)
                    ) {
                        Box(
                            Modifier
                                .size(gridSizeDp)
                                .align(Alignment.Center)
                        ) {
                            BoardGrid(
                                board = gameState.board,
                                onCellClick = { row, col ->
                                    if (
                                        (gameState.state == TriviatoeRoundState.MOVE_1 || gameState.state == TriviatoeRoundState.MOVE_2) &&
                                        gameState.currentTurn == playerId &&
                                        !gameState.moves.any { it.playerId == playerId && it.round == gameState.currentRound } &&
                                        gameState.board.none { it.row == row && it.col == col && it.symbol != null }
                                    ) {
                                        playSound(context, R.raw.triviatoe_place_down_piece)
                                        scope.launch {
                                            val symbol = playerSymbol ?: "X"
                                            session.submitMove(playerId, row, col, symbol)
                                        }
                                    }
                                },
                                cellSize = cellSize
                            )
                        }
                        Image(
                            painter = painterResource(id = R.drawable.triviatoe_box_grid2),
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier.matchParentSize()
                        )
                    }
                }

                if (playerId == gameState.players.firstOrNull()?.uid) {
                    val movesThisRound = gameState.moves.count { it.round == gameState.currentRound }
                    LaunchedEffect(movesThisRound, gameState.state, gameState.currentRound) {
                        if (gameState.state == TriviatoeRoundState.MOVE_1 && movesThisRound == 1) {
                            session.afterMove1(gameState.firstToMove!!, gameState.players)
                        }
                        if (gameState.state == TriviatoeRoundState.MOVE_2 && movesThisRound == 2) {
                            session.afterMove2()
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // --- INFO BOX (WINNER/DEFAULT LOGIC) ---
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .fillMaxWidth(0.96f)
                        .heightIn(min = 140.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.triviatoe_box_info),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.matchParentSize()
                    )
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 32.dp, vertical = 56.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (gameState.state == TriviatoeRoundState.FINISHED) {
                            val winnerName = gameState.players.find { it.uid == gameState.winner }?.name ?: "Unknown"
                            Text(
                                "🏆 Winner: $winnerName!",
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(24.dp))
                            RematchExitRow(
                                onRematch = {
                                    scope.launch {
                                        session.requestRematch(playerId)
                                        // Do NOT call tryResetIfAllAgreed here!
                                    }
                                },
                                onExit = {
                                    // STOP the music service before navigating
                                    context.stopService(Intent(context, com.example.gamehub.MusicService::class.java))
                                    navController.popBackStack("mainMenu", false)
                                },
                                rematchCount = "${(gameState.rematchVotes?.values?.count { it } ?: 0)}/${gameState.players.size}"
                            )
                        } else {
                            Text(
                                "Round: ${gameState.currentRound + 1}",
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("You are: ", color = Color.White, style = MaterialTheme.typography.titleMedium)
                                if (playerSymbol == "X" || playerSymbol == "O") {
                                    Image(
                                        painter = painterResource(playerSymbolRes),
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(Modifier.width(24.dp))
                                if (otherPlayer != null && otherSymbol != null) {
                                    Text("${otherPlayer.name} is: ", color = Color.White, style = MaterialTheme.typography.titleMedium)
                                    Image(
                                        painter = painterResource(otherSymbolRes),
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            val currentPlayer = gameState.players.find { it.uid == gameState.currentTurn }
                            Text(
                                "Current turn: ${currentPlayer?.name ?: "?"}",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(10.dp))

                            // Show last question and answer if available (not on first round)
                            val lastQuestion = gameState.lastQuestion as? TriviatoeQuestion.MultipleChoice
                            val lastCorrectIndex = gameState.lastCorrectIndex ?: lastQuestion?.correctIndex
                            if (lastQuestion != null && lastCorrectIndex != null && gameState.currentRound > 0) {
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "Last Question:",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    lastQuestion.question,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(4.dp))
                                val answer = lastQuestion.answers.getOrNull(lastCorrectIndex)
                                Text(
                                    "Correct Answer: ${answer ?: "?"}",
                                    color = Color(0xFF7CFF74),
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center
                                )
                            }

                            when (gameState.state) {
                                TriviatoeRoundState.REVEAL -> {
                                    val firstPlayer = gameState.players.find { it.uid == gameState.firstToMove }
                                    Text(
                                        "First to Move: ${firstPlayer?.name ?: "?"}",
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium,
                                        textAlign = TextAlign.Center
                                    )
                                    LaunchedEffect(gameState.firstToMove) {
                                        if (playerId == gameState.players.firstOrNull()?.uid && gameState.firstToMove != null) {
//                                            kotlinx.coroutines.delay(1000)
                                            scope.launch { session.advanceGameState() }
                                        }
                                    }
                                }
                                TriviatoeRoundState.CHECK_WIN -> {
                                    Text(
                                        "Checking for win...",
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium,
                                        textAlign = TextAlign.Center
                                    )
                                    LaunchedEffect(gameState.state, gameState.currentRound) {
                                        if (
                                            gameState.state == TriviatoeRoundState.CHECK_WIN &&
                                            playerId == gameState.players.firstOrNull()?.uid &&
                                            gameState.currentRound != lastCheckedRound
                                        ) {
                                            lastCheckedRound = gameState.currentRound
                                            scope.launch { session.finishMoveRound(gameState) }
                                        }
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BoardGrid(
    board: List<TriviatoeCell>,
    onCellClick: (row: Int, col: Int) -> Unit,
    cellSize: Dp
) {
    val gridSize = 10
    Column(
        modifier = Modifier.background(Color.Transparent),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (row in 0 until gridSize) {
            Row {
                for (col in 0 until gridSize) {
                    val cell = board.find { it.row == row && it.col == col }
                    Box(
                        Modifier
                            .size(cellSize)
                            .clickable { onCellClick(row, col) },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.triviatoe_grid_tile),
                            contentDescription = null,
                            modifier = Modifier.matchParentSize()
                        )
                        if (cell?.symbol == "X" || cell?.symbol == "O") {
                            Image(
                                painter = painterResource(
                                    if (cell.symbol == "X") R.drawable.x_icon else R.drawable.o_icon
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(cellSize)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpriteButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = MaterialTheme.typography.titleMedium.fontSize // Default
) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(12.dp))
    ) {
        Image(
            painter = painterResource(id = R.drawable.triviatoe_button),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.matchParentSize()
        )
        TextButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxSize(),
            colors = ButtonDefaults.textButtonColors(
                containerColor = Color.Transparent,
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = fontSize),
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .fillMaxWidth()
            )
        }
    }
}

@Suppress("UnusedBoxWithConstraintsScope")
@Composable
fun RematchExitRow(
    onRematch: () -> Unit,
    onExit: () -> Unit,
    rematchCount: String
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Use the minWidth in dp to adjust font size
        val isSmallScreen = maxWidth < 370.dp // 360-380 is typical for S23/S22/S21 etc

        val rematchFontSize =
            if (isSmallScreen) 15.sp // smaller font for small screens
            else MaterialTheme.typography.titleMedium.fontSize

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SpriteButton(
                text = "Rematch ($rematchCount)",
                onClick = onRematch,
                modifier = Modifier
                    .height(48.dp)
                    .padding(end = 16.dp)
                    .width(140.dp),
                fontSize = rematchFontSize // <- set custom font size
            )
            SpriteButton(
                text = "Exit",
                onClick = onExit,
                modifier = Modifier.height(48.dp).width(90.dp)
                // Font size for "Exit" can stay default
            )
        }
    }
}
fun playSound(context: android.content.Context, @RawRes resId: Int) {
    val mediaPlayer = MediaPlayer.create(context, resId)
    mediaPlayer?.setOnCompletionListener { mp ->
        mp.release()
    }
    mediaPlayer?.start()
}
