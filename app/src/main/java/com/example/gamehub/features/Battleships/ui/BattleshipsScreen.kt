package com.example.gamehub.features.battleships.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gamehub.lobby.FirestoreSession
import com.example.gamehub.lobby.LobbyService
import com.example.gamehub.lobby.codec.BattleshipsCodec
import com.example.gamehub.lobby.codec.BattleshipsState
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BattleshipsScreen(
    navController: NavController,
    code: String,
    userName: String
) {
    val playerUid = Firebase.auth.uid ?: return
    val db = Firebase.firestore
    val session = remember { FirestoreSession(code, BattleshipsCodec) }
    val scope = rememberCoroutineScope()

    var gameState by remember { mutableStateOf<BattleshipsState?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var gameEnded by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf("") }
    var rematchVotes by remember { mutableStateOf(0) }
    var totalPlayers by remember { mutableStateOf(2) }
    var players by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var resetAttempted by remember { mutableStateOf(false) }

    // Load player names
    LaunchedEffect(code) {
        db.collection("rooms").document(code)
            .addSnapshotListener { snap, _ ->
                val list = snap?.get("players") as? List<Map<String, Any>> ?: return@addSnapshotListener
                players = list.associate {
                    val uid = it["uid"] as? String ?: ""
                    val name = it["name"] as? String ?: ""
                    uid to name
                }
            }
    }

    // Watch game state
    LaunchedEffect(Unit) {
        session.stateFlow.collect { state ->
            gameState = state
        }
    }

    // Watch status & rematch state
    LaunchedEffect(Unit) {
        db.collection("rooms").document(code)
            .addSnapshotListener { snap, _ ->
                if (snap != null && snap.exists()) {
                    val status = snap.getString("status")
                    val gameId = snap.getString("gameId") ?: return@addSnapshotListener
                    val playersList = snap.get("players") as? List<Map<String, Any>> ?: return@addSnapshotListener
                    val uids = playersList.mapNotNull { it["uid"] as? String }
                    val votes = snap.get("rematchVotes") as? Map<String, Boolean> ?: return@addSnapshotListener

                    rematchVotes = votes.count { it.value }
                    totalPlayers = uids.size

                    if (status == "ended") {
                        val result = snap.get("gameState.$gameId.gameResult") as? Map<*, *>
                        val winnerUid = result?.get("winner") as? String ?: "Unknown"
                        val reason = result?.get("reason") as? String ?: "unknown reason"
                        val winnerName = players[winnerUid] ?: winnerUid
                        resultText = "🏆 $winnerName wins by $reason"
                        gameEnded = true
                    }

                    if (status == "ended" && votes.all { it.value } && !resetAttempted) {
                        resetAttempted = true
                        scope.launch {
                            LobbyService.resetGameIfRematchReady(code, gameId, uids)
                        }
                    }

                    if (status == "playing" && resetAttempted) {
                        resetAttempted = false
                        gameEnded = false
                    }
                }
            }
    }

    // Main UI
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🐋 Battleships") },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            gameState?.let { state ->
                val currentPlayerName = players[state.currentTurn] ?: state.currentTurn
                Text("Current turn: $currentPlayerName")
                Spacer(Modifier.height(8.dp))
                Text("Moves: ${state.moves.joinToString()}")
            } ?: Text("Waiting for game state...")
        }
    }

    // Game Settings Dialog
    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("Game Settings") },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        scope.launch {
                            LobbyService.surrender(code, "battleships", playerUid)
                        }
                        showSettings = false
                    }) {
                        Text("Surrender")
                    }

                    Button(onClick = {
                        scope.launch {
                            LobbyService.deleteRoom(code)
                            navController.popBackStack()
                        }
                    }) {
                        Text("Leave Game")
                    }

                    OutlinedButton(onClick = { showSettings = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    // Game Over Dialog
    if (gameEnded) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Game Over") },
            text = {
                Column {
                    Text(resultText)
                    Spacer(Modifier.height(8.dp))
                    Text("Rematch votes: $rematchVotes / $totalPlayers")
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = {
                        scope.launch {
                            LobbyService.voteRematch(code, playerUid)
                        }
                    }) { Text("Rematch") }

                    Button(onClick = {
                        scope.launch {
                            LobbyService.deleteRoom(code)
                            navController.popBackStack()
                        }
                    }) { Text("Leave Game") }
                }
            }
        )
    }
}
