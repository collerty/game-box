package com.example.gamehub.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gamehub.features.codenames.service.CodenamesService
import com.example.gamehub.features.codenames.ui.CodenamesActivity
import com.example.gamehub.navigation.NavRoutes
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

@Composable
fun HostLobbyScreen(
    navController: NavController,
    gameId: String,
    roomId: String,
    context: Context
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
                    // First get all players' information
                    db.collection("rooms").document(roomId)
                        .get()
                        .addOnSuccessListener { document ->
                            @Suppress("UNCHECKED_CAST")
                            val currentPlayers = document.get("players") as? List<Map<String, Any>> ?: emptyList()
                            
                            // Remove all players with their full information
                            val updates = mutableMapOf<String, Any>()
                            currentPlayers.forEach { player ->
                                updates["players"] = FieldValue.arrayRemove(player)
                            }
                            
                            // Then delete the room
                            db.collection("rooms").document(roomId)
                                .update(updates)
                                .addOnSuccessListener {
                                    db.collection("rooms").document(roomId)
                                        .delete()
                                        .addOnSuccessListener {
                                            navController.popBackStack()
                                        }
                                }
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

            if (gameId == "codenames") {
                // Team selection UI for Codenames
                val authUid = auth.currentUser?.uid
                val redTeam = players.filter { (it["team"] ?: "spectator") == "red" }
                val blueTeam = players.filter { (it["team"] ?: "spectator") == "blue" }
                val spectators = players.filter { (it["team"] ?: "spectator") == "spectator" }
                
                // Show spectators section
                Text("Spectators", style = MaterialTheme.typography.headlineSmall)
                spectators.forEach { player ->
                    val name = player["name"] as? String ?: ""
                    Text("• $name")
                }
                Spacer(Modifier.height(24.dp))

                // Red Team section
                Text("Red Team", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.headlineSmall)
                redTeam.forEach { player ->
                    val name = player["name"] as? String ?: ""
                    val isMe = player["uid"] == authUid
                    val isMaster = player["role"] == "master"
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("• $name ${if (isMaster) "(Master)" else "(Player)"}")
                    }
                }
                if (redTeam.size < 2) {
                    val hasMaster = redTeam.any { it["role"] == "master" }
                    val hasPlayer = redTeam.any { it["role"] == "player" }
                    
                    if (!hasMaster) {
                        Text(
                            "Join as master",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.clickable {
                                val currentPlayer = mapOf(
                                    "uid" to auth.currentUser?.uid,
                                    "name" to hostName
                                )
                                val newPlayer = mapOf(
                                    "uid" to auth.currentUser?.uid,
                                    "name" to hostName,
                                    "team" to "red",
                                    "role" to "master"
                                )
                                
                                // First remove the player from any team
                                db.collection("rooms").document(roomId)
                                    .get()
                                    .addOnSuccessListener { document ->
                                        @Suppress("UNCHECKED_CAST")
                                        val currentPlayers = document.get("players") as? List<Map<String, Any>> ?: emptyList()
                                        val updatedPlayers = currentPlayers.filter { it["uid"] != auth.currentUser?.uid }
                                        
                                        // Then add the player to the new role
                                        db.collection("rooms").document(roomId)
                                            .update("players", updatedPlayers + newPlayer)
                                    }
                            }
                        )
                    }
                    if (!hasPlayer) {
                        Text(
                            "Join as player",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.clickable {
                                val currentPlayer = mapOf(
                                    "uid" to auth.currentUser?.uid,
                                    "name" to hostName
                                )
                                val newPlayer = mapOf(
                                    "uid" to auth.currentUser?.uid,
                                    "name" to hostName,
                                    "team" to "red",
                                    "role" to "player"
                                )
                                
                                // First remove the player from any team
                                db.collection("rooms").document(roomId)
                                    .get()
                                    .addOnSuccessListener { document ->
                                        @Suppress("UNCHECKED_CAST")
                                        val currentPlayers = document.get("players") as? List<Map<String, Any>> ?: emptyList()
                                        val updatedPlayers = currentPlayers.filter { it["uid"] != auth.currentUser?.uid }
                                        
                                        // Then add the player to the new role
                                        db.collection("rooms").document(roomId)
                                            .update("players", updatedPlayers + newPlayer)
                                    }
                            }
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))

                // Blue Team section
                Text("Blue Team", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineSmall)
                blueTeam.forEach { player ->
                    val name = player["name"] as? String ?: ""
                    val isMe = player["uid"] == authUid
                    val isMaster = player["role"] == "master"
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("• $name ${if (isMaster) "(Master)" else "(Player)"}")
                    }
                }
                if (blueTeam.size < 2) {
                    val hasMaster = blueTeam.any { it["role"] == "master" }
                    val hasPlayer = blueTeam.any { it["role"] == "player" }
                    
                    if (!hasMaster) {
                        Text(
                            "Join as master",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                val currentPlayer = mapOf(
                                    "uid" to auth.currentUser?.uid,
                                    "name" to hostName
                                )
                                val newPlayer = mapOf(
                                    "uid" to auth.currentUser?.uid,
                                    "name" to hostName,
                                    "team" to "blue",
                                    "role" to "master"
                                )
                                
                                // First remove the player from any team
                                db.collection("rooms").document(roomId)
                                    .get()
                                    .addOnSuccessListener { document ->
                                        @Suppress("UNCHECKED_CAST")
                                        val currentPlayers = document.get("players") as? List<Map<String, Any>> ?: emptyList()
                                        val updatedPlayers = currentPlayers.filter { it["uid"] != auth.currentUser?.uid }
                                        
                                        // Then add the player to the new role
                                        db.collection("rooms").document(roomId)
                                            .update("players", updatedPlayers + newPlayer)
                                    }
                            }
                        )
                    }
                    if (!hasPlayer) {
                        Text(
                            "Join as player",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                val currentPlayer = mapOf(
                                    "uid" to auth.currentUser?.uid,
                                    "name" to hostName
                                )
                                val newPlayer = mapOf(
                                    "uid" to auth.currentUser?.uid,
                                    "name" to hostName,
                                    "team" to "blue",
                                    "role" to "player"
                                )
                                
                                // First remove the player from any team
                                db.collection("rooms").document(roomId)
                                    .get()
                                    .addOnSuccessListener { document ->
                                        @Suppress("UNCHECKED_CAST")
                                        val currentPlayers = document.get("players") as? List<Map<String, Any>> ?: emptyList()
                                        val updatedPlayers = currentPlayers.filter { it["uid"] != auth.currentUser?.uid }
                                        
                                        // Then add the player to the new role
                                        db.collection("rooms").document(roomId)
                                            .update("players", updatedPlayers + newPlayer)
                                    }
                            }
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            } else {
                players.forEach { player ->
                    val name = player["name"] as? String ?: ""
                    Text("• $name")
                }
                Spacer(Modifier.height(24.dp))
            }

            Button(
                onClick = {
                    scope.launch {
                        val startingPlayerUid = players.firstOrNull()?.get("uid") as? String
                        val hostUid = auth.currentUser?.uid
                        if (startingPlayerUid.isNullOrEmpty() || hostUid.isNullOrEmpty()) return@launch

                        // Game-specific initial state
                        val initialGameState = when (gameId) {
                            "battleships" -> mapOf(
                                "player1Id" to players.getOrNull(0)?.get("uid"),
                                "player2Id" to players.getOrNull(1)?.get("uid"),
                                "currentTurn" to startingPlayerUid,
                                "moves" to emptyList<String>(),
                                "powerUps" to players.associate { it["uid"] as String to listOf("RADAR", "BOMB") },
                                "energy" to players.associate { it["uid"] as String to 3 },
                                "gameResult" to null,
                                "mapVotes" to emptyMap<String, Int>(),
                                "chosenMap" to null,
                                "powerUpMoves" to emptyList<String>()
                            )
                            "ohpardon" -> mapOf(
                                "currentPlayer" to startingPlayerUid,
                                "scores" to emptyMap<String, Int>(),
                                "gameResult" to null,
                                "diceRoll" to null
                            )
                            "codenames" -> CodenamesService.generateGameState()
                            else -> emptyMap()
                        }

                        val rematchVotes = players.associate {
                            val uid = it["uid"] as? String ?: ""
                            uid to false
                        }

                        try {
                            if(gameId == "battleships") {
                                db.collection("rooms").document(roomId).update(
                                    mapOf(
                                        "status" to "started",
                                        "gameState.$gameId" to initialGameState,
                                        "rematchVotes" to rematchVotes
                                    )
                                ).addOnSuccessListener {
                                    println("✅ Game started successfully")
                                }.addOnFailureListener {
                                    println("❌ Failed to start game: ${it.message}")
                                }
                            }
                            else{
                                db.collection("rooms").document(roomId).update(
                                    mapOf(
                                        "status" to "started",
                                        "gameState.$gameId" to initialGameState,
                                    )
                                ).addOnSuccessListener {
                                    println("✅ Game started successfully")
                                }.addOnFailureListener {
                                    println("❌ Failed to start game: ${it.message}")
                                }
                            }
                        } catch (e: Exception) {
                            println("🔥 Exception during game start: ${e.message}")
                        }
                    }
                },
                enabled = players.size >= maxPlayers // Only when lobby is full
            ) {
                Text(if (players.size >= maxPlayers) "Start Game" else "Waiting for players…")
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
                        "codenames"   -> {
                            val intent = Intent(context, CodenamesActivity::class.java).apply {
                                putExtra("roomId", roomId)
                                putExtra("userName", hostName)
                            }
                            context.startActivity(intent)
                            null
                        }
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
