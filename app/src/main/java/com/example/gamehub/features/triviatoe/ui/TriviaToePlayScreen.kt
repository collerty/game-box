package com.example.gamehub.features.triviatoe.ui

import TriviatoeQuestionScreen
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
import com.example.gamehub.features.triviatoe.model.*
import com.example.gamehub.features.triviatoe.FirestoreTriviatoeSession
import com.example.gamehub.R
import kotlinx.coroutines.launch

@Suppress("UnusedBoxWithConstraintsScope")
@Composable
fun TriviatoePlayScreen(
    session: FirestoreTriviatoeSession,
    playerId: String
) {
    val scope = rememberCoroutineScope()
    val gameState by session.stateFlow.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var lastCheckedRound by remember { mutableStateOf(-1) }
    var lastAnnouncedRound by remember { mutableStateOf(-1) }

    LaunchedEffect(gameState.state, gameState.currentRound) {
        if (gameState.state == TriviatoeRoundState.MOVE_1 && gameState.currentRound != lastAnnouncedRound) {
            lastAnnouncedRound = gameState.currentRound
            val firstPlayerName = gameState.players.find { it.uid == gameState.firstToMove }?.name ?: "?"
            snackbarHostState.showSnackbar("$firstPlayerName goes first!")
            kotlinx.coroutines.delay(1000)
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
                        if (playerId == gameState.players.firstOrNull()?.uid) {
                            scope.launch {
                                session.setFirstToMoveAndAdvance(winnerId, randomized)
                            }
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
                // PNG: 404x404, grid area: 45,45 to 358,358 (313x313)
                // so: gridPercent = 313 / 404 = 0.7748f
                // and marginPercent = 45 / 404 = 0.1114f
                val gridPercent = 313f / 404f

                BoxWithConstraints(
                    Modifier.align(Alignment.CenterHorizontally)
                ) {
                    // use the smallest of width or height so box stays square
                    val boxSize = minOf(maxWidth, maxHeight)
                    val gridSizeDp = boxSize * gridPercent
                    val cellSize = gridSizeDp / 10

                    Box(
                        modifier = Modifier
                            .size(boxSize)
                            .align(Alignment.Center)
                    ) {
                        // Centered grid with perfect fit in border's transparent area
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
                                        scope.launch {
                                            val symbol = playerSymbol ?: "X"
                                            session.submitMove(playerId, row, col, symbol)
                                        }
                                    }
                                },
                                cellSize = cellSize
                            )
                        }
                        // Overlay the border PNG
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

                // Info box below grid (unchanged)
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
                            .padding(horizontal = 32.dp, vertical = 24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
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
                                        kotlinx.coroutines.delay(1000)
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
                            TriviatoeRoundState.FINISHED -> {
                                val winnerName = gameState.players.find { it.uid == gameState.winner }?.name ?: "Unknown"
                                Text(
                                    "🏆 Winner: $winnerName!",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleLarge,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Game Over!",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleLarge,
                                    textAlign = TextAlign.Center
                                )
                            }
                            else -> {}
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
