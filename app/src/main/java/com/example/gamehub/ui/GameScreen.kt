package com.example.gamehub.ui

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@Composable
fun GameScreen(navController: NavController, roomCode: String) {
    val db = Firebase.firestore

    // hold the gameId (so we can display it) and the list of player‐names
    var gameId by remember { mutableStateOf<String?>(null) }
    var players by remember { mutableStateOf<List<String>>(emptyList()) }

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        context.stopService(Intent(context, com.example.gamehub.MusicService::class.java))
    }

    // subscribe to the room document
    LaunchedEffect(roomCode) {
        val docRef = db.collection("rooms").document(roomCode)
        docRef.addSnapshotListener { snap, _ ->
            if (snap != null && snap.exists()) {
                gameId = snap.getString("gameId")
                // extract the "players" array of maps → list of names
                players = (snap.get("players") as? List<Map<String,Any>>)
                    ?.mapNotNull { it["name"] as? String }
                    ?: emptyList()
            } else {
                // if the room has been deleted upstream (host quit), go back
                navController.popBackStack()
            }
        }
    }

    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Game: ${gameId ?: ""}",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Game going with players:",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            players.forEach { name ->
                Text("• $name", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
