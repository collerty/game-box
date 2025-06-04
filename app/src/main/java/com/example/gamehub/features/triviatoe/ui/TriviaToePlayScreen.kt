package com.example.gamehub.features.triviatoe.ui

import TriviatoeQuestionScreen
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gamehub.features.triviatoe.model.*
import com.example.gamehub.features.triviatoe.FirestoreTriviatoeSession
import com.example.gamehub.R
import kotlinx.coroutines.launch

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

        // Show ONLY the question screen in QUESTION phase (all else hidden)
        if (gameState.state == TriviatoeRoundState.QUESTION) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                val mcQuestion = gameState.quizQuestion as? TriviatoeQuestion.MultipleChoice
                if (mcQuestion != null) {
                    val allAnswers: Map<String, PlayerAnswer?> = gameState.players.associate { player ->
                        player.uid to gameState.answers[player.uid]
                    }
                    TriviatoeQuestionScreen(
                        question = mcQuestion,
                        playerId = playerId,
                        players = gameState.players,
                        allAnswers = allAnswers,
                        correctIndex = mcQuestion.correctIndex,
                        onAnswer = { playerAnswer ->
                            scope.launch {
                                session.submitAnswer(
                                    playerId,
                                    playerAnswer
                                )
                            }
                        },
                        onQuestionResolved = { winnerId ->
                            if (playerId == gameState.players.firstOrNull()?.uid) {
                                scope.launch {
                                    session.setFirstToMoveAndAdvance(winnerId)
                                }
                            }
                        }
                    )
                }
            }
        } else {
            // All non-question phases use this layout:
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(60.dp)) // Space from top

                // Game board grid, outlined
                Box(
                    Modifier
                        .border(3.dp, Color.Black, RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .padding(8.dp)
                        .align(Alignment.CenterHorizontally)
                ) {
                    // WHICH PHASE? only allow click in MOVE_1 and MOVE_2 and only if allowed!
                    when (gameState.state) {
                        TriviatoeRoundState.MOVE_1 -> {
                            val movePlaced = gameState.moves.any {
                                it.playerId == playerId && it.round == gameState.currentRound
                            }
                            BoardGrid(
                                board = gameState.board,
                                onCellClick = { row, col ->
                                    if (
                                        gameState.currentTurn == playerId &&
                                        !movePlaced &&
                                        gameState.board.none { it.row == row && it.col == col && it.symbol != null }
                                    ) {
                                        scope.launch {
                                            val symbol = playerSymbol ?: "X"
                                            session.submitMove(playerId, row, col, symbol)
                                            // DO NOT call afterMove1 here!
                                        }
                                    }
                                }
                            )
                            // Host triggers afterMove1 only when exactly 1 move has been made this round
                            if (playerId == gameState.players.firstOrNull()?.uid) {
                                val movesThisRound = gameState.moves.count { it.round == gameState.currentRound }
                                LaunchedEffect(movesThisRound, gameState.state, gameState.currentRound) {
                                    if (gameState.state == TriviatoeRoundState.MOVE_1 && movesThisRound == 1) {
                                        session.afterMove1(gameState.firstToMove!!, gameState.players)
                                    }
                                }
                            }
                        }
                        TriviatoeRoundState.MOVE_2 -> {
                            val movePlaced = gameState.moves.any {
                                it.playerId == playerId && it.round == gameState.currentRound
                            }
                            BoardGrid(
                                board = gameState.board,
                                onCellClick = { row, col ->
                                    if (
                                        gameState.currentTurn == playerId &&
                                        !movePlaced &&
                                        gameState.board.none { it.row == row && it.col == col && it.symbol != null }
                                    ) {
                                        scope.launch {
                                            val symbol = playerSymbol ?: "X"
                                            session.submitMove(playerId, row, col, symbol)
                                        }
                                    }
                                }
                            )
                            // Host triggers afterMove2 only when exactly 2 moves have been made this round
                            if (playerId == gameState.players.firstOrNull()?.uid) {
                                val movesThisRound = gameState.moves.count { it.round == gameState.currentRound }
                                LaunchedEffect(movesThisRound, gameState.state, gameState.currentRound) {
                                    if (gameState.state == TriviatoeRoundState.MOVE_2 && movesThisRound == 2) {
                                        session.afterMove2()
                                    }
                                }
                            }
                        }
                        else -> {
                            BoardGrid(
                                board = gameState.board,
                                onCellClick = { _, _ -> }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // All game info in a single background box, below grid
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                        .border(2.dp, Color.Black, RoundedCornerShape(18.dp))
                        .padding(vertical = 20.dp, horizontal = 20.dp)
                        .fillMaxWidth(0.96f)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // ROUND
                        Text(
                            "Round: ${gameState.currentRound + 1}",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        // PLAYER INFO
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
                        // CURRENT TURN
                        val currentPlayer = gameState.players.find { it.uid == gameState.currentTurn }
                        Text(
                            "Current turn: ${currentPlayer?.name ?: "?"}",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(10.dp))

                        // State banners inside info box
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
    onCellClick: (row: Int, col: Int) -> Unit
) {
    val gridSize = 10
    Column(
        modifier = Modifier.background(Color.Transparent),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (row in 0 until gridSize) {
            Row(Modifier.align(Alignment.CenterHorizontally)) {
                for (col in 0 until gridSize) {
                    val cell = board.find { it.row == row && it.col == col }
                    Box(
                        Modifier
                            .size(32.dp)
                            .padding(1.dp)
                            .clickable { onCellClick(row, col) },
                        contentAlignment = Alignment.Center
                    ) {
                        // BG tile for the cell
                        Image(
                            painter = painterResource(id = R.drawable.triviatoe_grid_tile), // <---- YOUR TILE IMAGE
                            contentDescription = null,
                            modifier = Modifier.matchParentSize()
                        )
                        // X/O overlay if present
                        if (cell?.symbol == "X" || cell?.symbol == "O") {
                            Image(
                                painter = painterResource(
                                    if (cell.symbol == "X") R.drawable.x_icon else R.drawable.o_icon
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
