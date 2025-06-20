package com.example.gamehub.features.triviatoe.ui

import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gamehub.features.triviatoe.FirestoreTriviatoeSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.gamehub.R
import com.example.gamehub.features.triviatoe.model.TriviatoeRoundState
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext

@Composable
fun TriviatoeXOAssignScreen(
    session: FirestoreTriviatoeSession,
    playerId: String,
    userName: String,
    navController: NavController
) {
    val scope = rememberCoroutineScope()
    val state by session.stateFlow.collectAsState()
    val context = LocalContext.current
    var diceMediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // Animation state (just for UI, not logic)
    var animating by remember { mutableStateOf(true) }
    var showResult by remember { mutableStateOf(false) }
    var animationSymbol by remember { mutableStateOf("X") }
    var assignTimeout by remember { mutableStateOf(false) }
    var lastAssignAttempt by remember { mutableStateOf(0L) }
    var lastStartGameAttempt by remember { mutableStateOf(0L) }
    var hasSetReady by remember { mutableStateOf(false) }

    // --- Animation: alternate X/O for 2 seconds, then reveal ---
    LaunchedEffect(animating) {
        if (animating) {
            diceMediaPlayer = MediaPlayer.create(context, R.raw.triviatoe_dice_rolling)
            diceMediaPlayer?.isLooping = true
            diceMediaPlayer?.start()
            repeat(12) { i ->
                animationSymbol = if (i % 2 == 0) "X" else "O"
                delay(150)
            }
            animating = false
            showResult = true
            diceMediaPlayer?.stop()
            diceMediaPlayer?.release()
            diceMediaPlayer = null
        }
    }

    // After animation finishes, set ready flag in Firestore (only once)
    LaunchedEffect(showResult) {
        if (showResult && !hasSetReady) {
            hasSetReady = true
            println("setReadyForQuestion called for $playerId")
            scope.launch { session.setReadyForQuestion(playerId) }
            // Defensive: if stuck more than 12s after anim, show retry
            delay(12000)
            assignTimeout = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            diceMediaPlayer?.stop()
            diceMediaPlayer?.release()
            diceMediaPlayer = null
        }
    }

    // --- Firestore state-driven progression ---
    val assignedSymbol = state.players.find { it.uid == playerId }?.symbol
    val otherPlayer = state.players.firstOrNull { it.uid != playerId }
    val otherSymbol = otherPlayer?.symbol

    // Main effect: Handles assignment, ready, and round start (auto-retries)
    LaunchedEffect(state.players, state.readyForQuestion, state.state) {
        val validPlayers = state.players.filter { !it.uid.isNullOrEmpty() }
        val readyMap = state.readyForQuestion ?: emptyMap()
        val bothAssigned = validPlayers.size == 2 && validPlayers.all { !it.symbol.isNullOrEmpty() }
        val allReady = validPlayers.size == 2 && validPlayers.all { p -> readyMap[p.uid] == true }

        println("Effect: bothAssigned=$bothAssigned allReady=$allReady state=${state.state}")

        if (!bothAssigned) {
            if (System.currentTimeMillis() - lastAssignAttempt > 3000) {
                lastAssignAttempt = System.currentTimeMillis()
                println("Trying assignSymbolsRandomly")
                scope.launch { session.assignSymbolsRandomly() }
            }
        } else if (bothAssigned && allReady) {
            // Only trigger if not already at QUESTION state
            if (bothAssigned && allReady) {
                if (System.currentTimeMillis() - lastStartGameAttempt > 3000) {
                    lastStartGameAttempt = System.currentTimeMillis()
                    println("Trying startNextRound (both ready)")
                    scope.launch { session.startNextRound() }
                }
            }
        }
    }

    // Navigate forward as soon as Firestore says round is ready!
    LaunchedEffect(state.state, state.quizQuestion) {
        println("NAV CHECK: state=${state.state} question=${state.quizQuestion}")
        if (state.state == TriviatoeRoundState.QUESTION && state.quizQuestion != null) {
            println("Navigating to play screen!")
            navController.navigate(
                "triviatoe/${session.roomCode}/${Uri.encode(userName)}"
            ) {
                popUpTo("triviatoe/${session.roomCode}/${Uri.encode(userName)}/xo") { inclusive = true }
            }
        }
    }

    // Helper to get resource id
    fun symbolRes(symbol: String?): Int =
        when (symbol) {
            "X" -> R.drawable.x_icon
            "O" -> R.drawable.o_icon
            else -> R.drawable.x_icon // fallback to X
        }

    // --- UI ---
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.triviatoe_bg1),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(24.dp))
                .height(IntrinsicSize.Min)
        ) {
            Image(
                painter = painterResource(id = R.drawable.triviatoe_box1),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.matchParentSize()
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "YOU WILL BE",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(40.dp))
                val currentRes = if (showResult && assignedSymbol != null)
                    symbolRes(assignedSymbol)
                else
                    symbolRes(animationSymbol)
                Image(
                    painter = painterResource(currentRes),
                    contentDescription = null,
                    modifier = Modifier.size(96.dp)
                )
                Spacer(Modifier.height(32.dp))
                if (showResult && assignedSymbol != null) {
                    if (otherPlayer != null && otherSymbol != null) {
                        Text(
                            "${otherPlayer.name} is:",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Image(
                            painter = painterResource(symbolRes(otherSymbol)),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }

    // If stuck too long, show retry/leave
    if (assignTimeout) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Still waiting... Something may be wrong.",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = { navController.popBackStack() }) {
                    Text("Leave Room")
                }
                Button(onClick = {
                    assignTimeout = false
                    scope.launch {
                        session.assignSymbolsRandomly()
                        session.setReadyForQuestion(playerId)
                        session.startNextRound()
                    }
                }) {
                    Text("Retry Assign")
                }
            }
        }
    }
}
