package com.example.gamehub.features.codenames.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth

@Composable
fun CodenamesScreen(
    navController: NavController,
    roomCode: String,
    userName: String
) {
    val db = Firebase.firestore
    val auth = Firebase.auth
    val uid = auth.currentUser?.uid ?: return
    
    var counter by remember { mutableStateOf(0) }
    var currentTurn by remember { mutableStateOf<String?>(null) }
    
    // Listen for game state updates
    LaunchedEffect(roomCode) {
        val roomRef = db.collection("rooms").document(roomCode)
        roomRef.addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) {
                val gameState = snapshot.get("gameState.codenames") as? Map<*, *>
                counter = (gameState?.get("counter") as? Number)?.toInt() ?: 0
                currentTurn = gameState?.get("currentTurn") as? String
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Counter: $counter",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Current Turn: ${currentTurn ?: "Waiting..."}",
            style = MaterialTheme.typography.bodyLarge
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                val roomRef = db.collection("rooms").document(roomCode)
                roomRef.update(
                    mapOf(
                        "gameState.codenames.counter" to (counter + 1),
                        "gameState.codenames.currentTurn" to uid
                    )
                )
            },
            enabled = currentTurn != uid
        ) {
            Text("Increment Counter")
        }
    }
} 