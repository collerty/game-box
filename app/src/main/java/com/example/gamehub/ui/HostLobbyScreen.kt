package com.example.gamehub.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gamehub.navigation.NavRoutes
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
    val scope = rememberCoroutineScope()

    var roomName by remember { mutableStateOf<String?>(null) }
    var hostName by remember { mutableStateOf<String?>(null) }
    var players by remember { mutableStateOf<List<String>>(emptyList()) }
    var maxPlayers by remember { mutableStateOf(0) }
    var status by remember { mutableStateOf("waiting") }
    var showExitDialog by remember { mutableStateOf(false) }

    LaunchedEffect(roomId) {
        db.collection("rooms").document(roomId)
            .addSnapshotListener { snap, _ ->
                if (snap == null || !snap.exists()) {
                    navController.popBackStack()
                } else {
                    roomName = snap.getString("name")
                    hostName = snap.getString("hostName")
                    maxPlayers = snap.getLong("maxPlayers")?.toInt() ?: 0
                    status = snap.getString("status") ?: "waiting"

                    @Suppress("UNCHECKED_CAST")
                    players = (snap.get("players") as? List<Map<String, Any>>)
                        ?.mapNotNull { it["name"] as? String }
                        ?: emptyList()
                }
            }
    }

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
                }) {
                    Text("Close Room")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Cancel")
                }
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
            players.forEach { name ->
                Text("• $name")
            }
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        db.collection("rooms")
                            .document(roomId)
                            .update("status", "started")
                    }
                },
                enabled = players.size >= 2
            ) {
                Text(
                    if (players.size >= 2) "Start Game"
                    else "Waiting for players…"
                )
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { showExitDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Close Room")
            }

            LaunchedEffect(status) {
                if (status == "started" && hostName != null) {
                    val route = when (gameId) {
                        "battleships" -> NavRoutes.BATTLESHIPS_GAME
                        "ohpardon" -> NavRoutes.OHPARDON_GAME
                        else -> null
                    }
                    route?.let {
                        navController.navigate(
                            it.replace("{code}", roomId)
                                .replace("{userName}", Uri.encode(hostName!!))
                        )
                    }
                }
            }
        }
    }
}
