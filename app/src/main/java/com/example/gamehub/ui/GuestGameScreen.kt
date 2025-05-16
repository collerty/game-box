package com.example.gamehub.ui

import android.net.Uri  // ← Add this import!
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.example.gamehub.navigation.NavRoutes

@Composable
fun GuestGameScreen(
    navController: NavController,
    gameId: String,
    code: String,
    userName: String
) {
    val db = Firebase.firestore

    var players    by remember { mutableStateOf<List<String>>(emptyList()) }
    var maxPlayers by remember { mutableStateOf(0) }
    var status     by remember { mutableStateOf("waiting") }

    // Listen for room updates
    LaunchedEffect(code) {
        db.collection("rooms").document(code)
            .addSnapshotListener { snap, _ ->
                if (snap == null || !snap.exists()) {
                    navController.popBackStack()
                } else {
                    players = (snap.get("players") as? List<Map<String,Any>>)
                        ?.mapNotNull { it["name"] as? String }
                        ?: emptyList()
                    maxPlayers = snap.getLong("maxPlayers")?.toInt() ?: 0
                    status     = snap.getString("status") ?: "waiting"
                }
            }
    }

    // Navigate when game starts
    LaunchedEffect(status) {
        if (status == "started") {
            val route = when (gameId) {
                "battleships" -> NavRoutes.BATTLESHIPS_GAME
                "ohpardon"    -> NavRoutes.OHPARDON_GAME
                else          -> null
            }
            route?.let {
                navController.navigate(
                    it.replace("{code}", code)
                        .replace("{userName}", Uri.encode(userName))
                )
            }
        }
    }

    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Room ID: $code", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text("Players (${players.size}/$maxPlayers):")
            players.forEach { Text("• $it") }
            Spacer(Modifier.height(24.dp))
            Text(
                if (status == "started") "Game is starting!"
                else "Waiting for host…",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = {
                db.collection("rooms").document(code)
                    .update(
                        "players",
                        FieldValue.arrayRemove(
                            mapOf(
                                "uid"  to Firebase.auth.uid,
                                "name" to userName
                            )
                        )
                    )
                    .addOnSuccessListener {
                        navController.popBackStack()
                    }
            }) {
                Text("Leave Room")
            }
        }
    }
}
