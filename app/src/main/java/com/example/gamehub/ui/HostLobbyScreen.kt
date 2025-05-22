// app/src/main/java/com/example/gamehub/ui/HostLobbyScreen.kt
package com.example.gamehub.ui

import android.net.Uri
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

    // UI state
    var players    by remember { mutableStateOf<List<String>>(emptyList()) }
    var maxPlayers by remember { mutableStateOf(0) }
    var status     by remember { mutableStateOf("waiting") }
    var hostName   by remember { mutableStateOf<String?>(null) }

    // 1) Listen for room updates
    LaunchedEffect(roomId) {
        db.collection("rooms").document(roomId)
            .addSnapshotListener { snap, _ ->
                if (snap == null || !snap.exists()) {
                    navController.popBackStack()
                } else {
                    // Pull status & maxPlayers
                    status     = snap.getString("status") ?: "waiting"
                    maxPlayers = snap.getLong("maxPlayers")?.toInt() ?: 0

                    // Pull list of player‐names
                    val raw = snap.get("players") as? List<Map<String, Any>>
                    players = raw?.mapNotNull { it["name"] as? String } ?: emptyList()

                    // The host is the first player in that list (if any)
                    hostName = players.firstOrNull()
                }
            }
    }

    // 2) Navigate to vote when status flips to “started”
    LaunchedEffect(status, hostName) {
        val name = hostName ?: return@LaunchedEffect
        if (status == "started") {
            val route = when (gameId) {
                "battleships" -> NavRoutes.BATTLE_VOTE
                "ohpardon"    -> NavRoutes.OHPARDON_GAME
                else          -> null
            }
            route?.let {
                navController.navigate(
                    it.replace("{code}", roomId)
                        .replace("{userName}", Uri.encode(name))
                )
            }
        }
    }

    // 3) UI: show room info & Start button
    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Room ID: $roomId", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text("Players (${players.size}/$maxPlayers):")
            players.forEach { Text("• $it") }
            Spacer(Modifier.height(24.dp))
            Button(onClick = {
                // Host clicks “Start”
                db.collection("rooms").document(roomId)
                    .update("status", "started")
            }) {
                Text("Start Game")
            }
        }
    }
}
