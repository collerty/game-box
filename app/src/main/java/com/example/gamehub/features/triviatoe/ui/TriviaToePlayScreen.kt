package com.example.gamehub.features.triviatoe.ui

import TriviatoeQuestionScreen
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    playerId: String // Current player's UID
) {
    val scope = rememberCoroutineScope()
    val gameState by session.stateFlow.collectAsState()

    // UI state for MC answer selection
    var selectedAnswer by remember { mutableStateOf<Int?>(null) }
    var answerSubmitted by remember { mutableStateOf(false) }
    var timeLeft by remember { mutableStateOf(10) }

    val sessionState by session.stateFlow.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var lastCheckedRound by remember { mutableStateOf(-1) }

    var lastAnnouncedRound by remember { mutableStateOf(-1) }
    LaunchedEffect(gameState.state, gameState.currentRound) {
        if (gameState.state == TriviatoeRoundState.MOVE_1 && gameState.currentRound != lastAnnouncedRound) {
            lastAnnouncedRound = gameState.currentRound
            val firstPlayerName = gameState.players.find { it.uid == gameState.firstToMove }?.name ?: "?"
            snackbarHostState.showSnackbar("$firstPlayerName goes first!")
            // Optionally add a small delay so Snackbar is visible
            kotlinx.coroutines.delay(1000)
        }
    }

    // Resolve players
    val player = gameState.players.find { it.uid == playerId }
    val playerSymbol = player?.symbol // "X" or "O"
    val playerSymbolRes = when (playerSymbol) {
        "X" -> R.drawable.x_icon
        "O" -> R.drawable.o_icon
        else -> R.drawable.x_icon // fallback
    }
    val otherPlayer = gameState.players.firstOrNull { it.uid != playerId }
    val otherSymbol = otherPlayer?.symbol
    val otherSymbolRes = when (otherSymbol) {
        "X" -> R.drawable.x_icon
        "O" -> R.drawable.o_icon
        else -> R.drawable.o_icon
    }

    Box(Modifier.fillMaxSize()) {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        // Main UI
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Player identities
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("You are: ", style = MaterialTheme.typography.titleMedium)
                if (playerSymbol == "X" || playerSymbol == "O") {
                    Image(
                        painter = painterResource(playerSymbolRes),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(Modifier.width(24.dp))
                if (otherPlayer != null && otherSymbol != null) {
                    Text("${otherPlayer.name} is: ", style = MaterialTheme.typography.titleMedium)
                    Image(
                        painter = painterResource(otherSymbolRes),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            // 1. Show current round
            Text(
                "Round: ${gameState.currentRound + 1}",
                style = MaterialTheme.typography.titleLarge
            )

            // 2. Winner banner
            gameState.winner?.let { winnerId ->
                val winnerName = gameState.players.find { it.uid == winnerId }?.name ?: "Unknown"
                Text(
                    "🏆 Winner: $winnerName!",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFF4CAF50)
                )
            }

            Spacer(Modifier.height(16.dp))

            LaunchedEffect(gameState.state) {
                println("=== UI State changed to: ${gameState.state} at round ${gameState.currentRound} ===")
            }

            // 3. Main round logic
            when (gameState.state) {

                TriviatoeRoundState.QUESTION -> {
                    val mcQuestion = gameState.quizQuestion as? TriviatoeQuestion.MultipleChoice
                    if (mcQuestion != null) {
                        // The FIX is below: this always maps every player's uid to their answerIndex, or null if they haven't answered.
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
                                        playerAnswer // This is PlayerAnswer(answerIndex, timestamp)
                                    )
                                }
                            },
                            onQuestionResolved = { winnerId ->
                                println("Host ($playerId) is calling setFirstToMoveAndAdvance with winnerId=$winnerId")
                                // Only host sets the winner and advances state
                                if (playerId == gameState.players.firstOrNull()?.uid) {
                                    scope.launch {
                                        session.setFirstToMoveAndAdvance(winnerId)
                                    }
                                }
                            }
                        )
                    }
                }

                TriviatoeRoundState.REVEAL -> {
                    val firstPlayer = gameState.players.find { it.uid == gameState.firstToMove }
                    Text("First to Move: ${firstPlayer?.name ?: "?"}")
                    Spacer(Modifier.height(8.dp))

                    // Automatically advance after 1 second (host only)
                    LaunchedEffect(gameState.firstToMove) {
                        if (playerId == gameState.players.firstOrNull()?.uid && gameState.firstToMove != null) {
                            kotlinx.coroutines.delay(1000)
                            scope.launch { session.advanceGameState() }
                        }
                    }
                }

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
                    Spacer(Modifier.height(8.dp))
                    val currentPlayer = gameState.players.find { it.uid == gameState.currentTurn }
                    Text("Current turn: ${currentPlayer?.name ?: "?"}")

                    // ---- KEY FIX: Host advances state when a move appears ----
                    if (playerId == gameState.players.firstOrNull()?.uid) { // host
                        val movesThisRound = gameState.moves.count { it.round == gameState.currentRound }
                        LaunchedEffect(movesThisRound, gameState.state, gameState.currentRound) {
                            if (gameState.state == TriviatoeRoundState.MOVE_1 && movesThisRound == 1) {
                                // A move was placed, advance!
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
                    Spacer(Modifier.height(8.dp))
                    val currentPlayer = gameState.players.find { it.uid == gameState.currentTurn }
                    Text("Current turn: ${currentPlayer?.name ?: "?"}")

                    // <<--- NEW! Host checks if both players have moved:
                    if (playerId == gameState.players.firstOrNull()?.uid) {
                        val movesThisRound = gameState.moves.count { it.round == gameState.currentRound }
                        LaunchedEffect(movesThisRound, gameState.state, gameState.currentRound) {
                            if (gameState.state == TriviatoeRoundState.MOVE_2 && movesThisRound == 2) {
                                // Both players have moved; host advances state
                                session.afterMove2()
                            }
                        }
                    }
                }

                TriviatoeRoundState.CHECK_WIN -> {
                    Text("Checking for win...")
                    // Only host runs this check
                    LaunchedEffect(gameState.state, gameState.currentRound) {
                        if (
                            gameState.state == TriviatoeRoundState.CHECK_WIN &&
                            playerId == gameState.players.firstOrNull()?.uid &&
                            gameState.currentRound != lastCheckedRound
                        ) {
                            println("Host runs finishMoveRound for round ${gameState.currentRound}")
                            lastCheckedRound = gameState.currentRound
                            session.finishMoveRound(gameState)
                        }
                    }
                }

                TriviatoeRoundState.FINISHED -> {
                    Text("Game Over!")
                    // Show winner or restart option
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
    Column {
        for (row in 0 until 10) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0 until 10) {
                    val cell = board.find { it.row == row && it.col == col }
                    Box(
                        Modifier
                            .size(28.dp)
                            .padding(1.dp)
                            .background(Color.White, RoundedCornerShape(4.dp))
                            .clickable { onCellClick(row, col) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (cell?.symbol == "X" || cell?.symbol == "O") {
                            Image(
                                painter = painterResource(
                                    if (cell.symbol == "X") R.drawable.x_icon else R.drawable.o_icon
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

