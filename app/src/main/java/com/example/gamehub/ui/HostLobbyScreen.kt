package com.example.gamehub.ui

import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gamehub.features.whereandwhen.model.WhereAndWhenGameState
import com.example.gamehub.features.whereandwhen.ui.gameChallenges
import com.example.gamehub.navigation.NavRoutes
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

@Composable
fun HostLobbyScreen(
    navController: NavController,
    gameId: String,
    roomId: String
) {
    val db = Firebase.firestore
    val auth = Firebase.auth
    val scope = rememberCoroutineScope()

    var roomName by remember { mutableStateOf<String?>(null) }
    var hostName by remember { mutableStateOf<String?>(null) }
    var players by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var maxPlayers by remember { mutableStateOf(0) }
    var status by remember { mutableStateOf("waiting") }
    var showExitDialog by remember { mutableStateOf(false) }

    // Live-update lobby
    DisposableEffect(roomId) {
        val listener: ListenerRegistration = db.collection("rooms").document(roomId)
            .addSnapshotListener { snap, _ ->
                if (snap == null || !snap.exists()) {
                    navController.popBackStack()
                } else {
                    roomName = snap.getString("name")
                    hostName = snap.getString("hostName")
                    maxPlayers = snap.getLong("maxPlayers")?.toInt() ?: 0
                    status = snap.getString("status") ?: "waiting"
                    @Suppress("UNCHECKED_CAST")
                    players = snap.get("players") as? List<Map<String, Any>> ?: emptyList()
                }
            }
        onDispose { listener.remove() }
    }

    // BackHandler: confirm exit
    BackHandler {
        showExitDialog = true
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit Lobby?") },
            text = { Text("Closing the lobby will disconnect all players.") },
            confirmButton = {
                TextButton(onClick = {
                    db.collection("rooms").document(roomId)
                        .delete()
                        .addOnSuccessListener {
                            navController.popBackStack()
                        }
                }) { Text("Close Room") }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                "Hosting: ${roomName ?: roomId}",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(8.dp))
            Text("Players: ${players.size} / $maxPlayers")
            Spacer(Modifier.height(12.dp))
            players.forEach { player ->
                val name = player["name"] as? String ?: ""
                Text("• $name")
            }
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        // ... (existing uid checks)

                        // Prepare the updates
                        val updates = mutableMapOf<String, Any?>(
                            "status" to "started"
                            // Keep existing rematchVotes reset if applicable from your original code
                            // "rematchVotes" to players.associate { (it["uid"] as? String ?: "") to false }
                        )

                        // Game-specific initial state ON START
                        when (gameId) {
                            "battleships" -> {
                                // Battleships already has player1Id, player2Id, etc. set during LobbyService.host/join
                                // It might need to ensure 'currentTurn' is set correctly if not already.
                                // And reset other relevant fields if starting fresh (e.g. moves, energy for a new game vs rematch)
                                updates["gameState.$gameId.currentTurn"] = players.firstOrNull()?.get("uid") as? String ?: ""
                                // Add other necessary initializations for battleships if they weren't done in LobbyService.host
                                // For example, if LobbyService.host only sets up player IDs and initial energy,
                                // here you might set chosenMap to null, clear mapVotes, clear ships, clear ready status.
                                // The current structure in LobbyService for battleships is quite complete for initial hosting though.
                            }
                            "ohpardon" -> {
                                updates["gameState.$gameId.currentPlayer"] = players.firstOrNull()?.get("uid") as? String ?: ""
                                updates["gameState.$gameId.diceRoll"] = null // Ensure dice is not pre-rolled
                                // Other OhPardon initializations
                            }
                            "whereandwhen" -> {
                                // Key fix: Set roundStartTimeMillis when the game actually starts
                                updates["gameState.$gameId.roundStartTimeMillis"] = System.currentTimeMillis()
                                updates["gameState.$gameId.currentRoundIndex"] = 0 // Ensure it's round 0
                                updates["gameState.$gameId.currentChallengeId"] = gameChallenges.firstOrNull()?.id ?: ""
                                updates["gameState.$gameId.roundStatus"] = WhereAndWhenGameState.STATUS_GUESSING
                                updates["gameState.$gameId.playerGuesses"] = emptyMap<String, Any>()
                                updates["gameState.$gameId.roundResults"] = mapOf("challengeId" to "", "results" to emptyMap<String,Any>())
                                updates["gameState.$gameId.playersReadyForNextRound"] = emptyMap<String, Boolean>()
                            }
                        }

                        try {
                            db.collection("rooms").document(roomId).update(updates)
                                .addOnSuccessListener {
                                    Log.d("HostLobby", "Game $gameId started successfully. Room status set to 'started'.")
                                    // Navigation will happen via the LaunchedEffect observing 'status'
                                }
                                .addOnFailureListener { e ->
                                    Log.e("HostLobby", "Failed to start game $gameId: ${e.message}", e)
                                    // Show a toast or error message to the host
                                }
                        } catch (e: Exception) {
                            Log.e("HostLobby", "Exception during game start for $gameId: ${e.message}", e)
                        }
                    }
                },
                enabled = players.size >= 2 && players.size <= maxPlayers && status == "waiting" // Updated condition
            ) {
                Text(
                    if (status != "waiting") "Game In Progress..."
                    else if (players.size >= 2 && players.size <= maxPlayers) "Start Game (${players.size}/$maxPlayers)"
                    else "Waiting for more players... (${players.size}/$maxPlayers)"
                )
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { showExitDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Close Room")
            }

            // Navigate to next screen when status flips
            LaunchedEffect(status, hostName) {
                if (status == "started" && hostName != null) {
                    val route = when (gameId) {
                        "battleships" -> NavRoutes.BATTLE_VOTE // Go to vote first, not directly to game!
                        "ohpardon"    -> NavRoutes.OHPARDON_GAME
                        "whereandwhen" -> NavRoutes.WHERE_AND_WHEN_GAME
                        else -> null
                    }
                    route?.let {
                        navController.navigate(
                            it
                                .replace("{code}", roomId)
                                .replace("{userName}", Uri.encode(hostName!!))
                        )
                    }
                }
            }
        }
    }
}
