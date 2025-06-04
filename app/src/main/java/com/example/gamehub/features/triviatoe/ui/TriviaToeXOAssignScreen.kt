package com.example.gamehub.features.triviatoe.ui

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
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip

@Composable
fun TriviatoeXOAssignScreen(
    session: FirestoreTriviatoeSession,
    playerId: String,
    userName: String,
    navController: NavController
) {
    val scope = rememberCoroutineScope()
    val state by session.stateFlow.collectAsState()
    var animating by remember { mutableStateOf(true) }
    var showResult by remember { mutableStateOf(false) }
    var animationSymbol by remember { mutableStateOf("X") }
    var assigned by remember { mutableStateOf(false) } // ensure assignSymbolsRandomly is called once

    // Debug print: see what's in state
    Text(
        "DEBUG: " + state.players.joinToString { "${it.name}:${it.symbol}" },
        color = androidx.compose.ui.graphics.Color.Gray
    )

    // Animation: alternate X/O for 2 seconds, then reveal
    LaunchedEffect(animating) {
        if (animating) {
            repeat(12) { i ->
                animationSymbol = if (i % 2 == 0) "X" else "O"
                delay(150)
            }
            animating = false
            showResult = true
        }
    }

    // Automatically assign after animation is done and both players are loaded, only by host
    LaunchedEffect(showResult, state.players) {
        if (!assigned && showResult && state.players.size == 2 && state.players.firstOrNull()?.uid == playerId) {
            assigned = true
            println("Auto-assigning symbols now!")
            scope.launch { session.assignSymbolsRandomly() }
        }
    }

    LaunchedEffect(state.players) {
        println("Current players from state: $state.players")
    }


    val assignedSymbol = state.players.find { it.uid == playerId }?.symbol
    val otherPlayer = state.players.firstOrNull { it.uid != playerId }
    val otherSymbol = otherPlayer?.symbol

    LaunchedEffect(assignedSymbol, state.players) {
        if (
            showResult && // animation done
            assignedSymbol != null &&
            state.players.size == 2 &&
            playerId == state.players.firstOrNull()?.uid
        ) {
            // Only host triggers!
            delay(2000)
            scope.launch { session.startNextRound() }
        }
    }

    // Auto-navigate to PlayScreen only when QUESTION is ready in Firestore!
    LaunchedEffect(state.state, state.quizQuestion) {
        if (state.state == TriviatoeRoundState.QUESTION && state.quizQuestion != null) {
            navController.navigate(
                "triviatoe/${session.roomCode}/${Uri.encode(userName)}"
            )
        }
    }

    // Helper to get resource id
    fun symbolRes(symbol: String?): Int =
        when (symbol) {
            "X" -> R.drawable.x_icon
            "O" -> R.drawable.o_icon
            else -> R.drawable.x_icon // fallback to X
        }
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.triviatoe_bg1),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )

        // Center the overlay box
        // Center the overlay box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(24.dp)) // round the corners
                .height(IntrinsicSize.Min) // let height grow with content
        ) {
            // --- Background sprite, stretches to fit the box! ---
            Image(
                painter = painterResource(id = R.drawable.triviatoe_box1), // <--- your sprite!
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.matchParentSize()
            )

            // --- Foreground UI goes here, exactly as before ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp, horizontal = 20.dp), // padding inside the sprite
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

                // Animation / symbol display
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
    // Defensive timer: if stuck for 10s, show retry UI
    var assignTimeout by remember { mutableStateOf(false) }
    LaunchedEffect(showResult, assigned) {
        if (showResult && assigned) {
            delay(10000)
            assignTimeout = true
        }
    }
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
            }
        }
    }

}
