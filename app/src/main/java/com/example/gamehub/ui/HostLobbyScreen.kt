package com.example.gamehub.ui

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gamehub.navigation.NavRoutes
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

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

    // 1️⃣ Live‐update listener tied to this screen’s lifecycle
    DisposableEffect(roomId) {
        val ref = db.collection("rooms").document(roomId)
        val listener: ListenerRegistration = ref.addSnapshotListener { snap, _ ->
            if (snap == null || !snap.exists()) {
                // Room deleted or invalid
                navController.popBackStack()
            } else {
                // Update room status & capacity
                status     = snap.getString("status") ?: "waiting"
                maxPlayers = snap.getLong("maxPlayers")?.toInt() ?: 0

                // Re-extract the players array on every update
                val raw = snap.get("players") as? List<*>
                players = raw
                    ?.mapNotNull { (it as? Map<*, *>)?.get("name") as? String }
                    ?: emptyList()

                // Host is the first in that list
                hostName = players.firstOrNull()
            }
        }
        onDispose { listener.remove() }
    }

    // 2️⃣ When status flips to “started”, send the host into the VOTE screen (not play)
    LaunchedEffect(status, hostName) {
        if (status == "started") {
            val name = hostName ?: return@LaunchedEffect
            val route = when (gameId) {
                "battleships" -> NavRoutes.BATTLE_VOTE   // vote, not play :contentReference[oaicite:2]{index=2}
                "ohpardon"    -> NavRoutes.OHPARDON_GAME
                else          -> null
            }
            route?.let {
                navController.navigate(
                    it
                        .replace("{code}", roomId)
                        .replace("{userName}", Uri.encode(name))
                )
            }
        }
    }

    // 3️⃣ UI: show room code, dynamic players list, and a “Start Game” button
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

            Button(
                onClick = {
                    // Host starts the match
                    db.collection("rooms")
                        .document(roomId)
                        .update("status", "started")
                },
                enabled = players.size >= maxPlayers   // only when full
            ) {
                Text("Start Game")
            }
        }
    }
}
