package com.example.gamehub.ui

import android.content.Intent
import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import android.util.Log


import com.example.gamehub.features.whereandwhe.model.WhereAndWhenGameState


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
    val localCtx = LocalContext.current

    var roomName by remember { mutableStateOf<String?>(null) }
    var hostName by remember { mutableStateOf<String?>(null) }
    var players by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var maxPlayers by remember { mutableStateOf(0) }
    var status by remember { mutableStateOf("waiting") }
    var showExitDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        context.stopService(Intent(context, com.example.gamehub.MusicService::class.java))
    }

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
                    // Player removal logic from develop branch
                    db.collection("rooms").document(roomId)
                        .get()
                        .addOnSuccessListener { document ->
                            @Suppress("UNCHECKED_CAST")
                            val currentPlayers = document.get("players") as? List<Map<String, Any>> ?: emptyList()
                            val updates = mutableMapOf<String, Any>()
                            currentPlayers.forEach { player ->
                                updates["players"] = FieldValue.arrayRemove(player)
                            }
                            db.collection("rooms").document(roomId)
                                .update(updates) // This seems to intend to remove all players first.
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
                // --- Codenames UI from develop branch ---
                val authUid = auth.currentUser?.uid
                val redTeam = players.filter { (it["team"] ?: "spectator") == "red" }
                val blueTeam = players.filter { (it["team"] ?: "spectator") == "blue" }
                val spectators = players.filter { (it["team"] ?: "spectator") == "spectator" }

                Text("Spectators", style = MaterialTheme.typography.headlineSmall)
                spectators.forEach { player ->
                    val name = player["name"] as? String ?: ""
                    Text("• $name")
                }
                Spacer(Modifier.height(24.dp))

                Text("Red Team", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.headlineSmall)
                redTeam.forEach { player ->
                    val name = player["name"] as? String ?: ""
                    // val isMe = player["uid"] == authUid // isMe not used in display text
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
                                // val currentPlayer = mapOf("uid" to auth.currentUser?.uid, "name" to hostName) // Unused
                                val newPlayer = mapOf("uid" to auth.currentUser?.uid, "name" to hostName, "team" to "red", "role" to "master")
                                db.collection("rooms").document(roomId).get().addOnSuccessListener { document ->
                                    @Suppress("UNCHECKED_CAST")
                                    val currentPlayers = document.get("players") as? List<Map<String, Any>> ?: emptyList()
                                    val updatedPlayers = currentPlayers.filter { it["uid"] != auth.currentUser?.uid }
                                    db.collection("rooms").document(roomId).update("players", updatedPlayers + newPlayer)
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
                                // val currentPlayer = mapOf("uid" to auth.currentUser?.uid, "name" to hostName) // Unused
                                val newPlayer = mapOf("uid" to auth.currentUser?.uid, "name" to hostName, "team" to "red", "role" to "player")
                                db.collection("rooms").document(roomId).get().addOnSuccessListener { document ->
                                    @Suppress("UNCHECKED_CAST")
                                    val currentPlayers = document.get("players") as? List<Map<String, Any>> ?: emptyList()
                                    val updatedPlayers = currentPlayers.filter { it["uid"] != auth.currentUser?.uid }
                                    db.collection("rooms").document(roomId).update("players", updatedPlayers + newPlayer)
                                }
                            }
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))

                Text("Blue Team", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineSmall)
                blueTeam.forEach { player ->
                    val name = player["name"] as? String ?: ""
                    // val isMe = player["uid"] == authUid // isMe not used
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
                                // val currentPlayer = mapOf("uid" to auth.currentUser?.uid, "name" to hostName) // Unused
                                val newPlayer = mapOf("uid" to auth.currentUser?.uid, "name" to hostName, "team" to "blue", "role" to "master")
                                db.collection("rooms").document(roomId).get().addOnSuccessListener { document ->
                                    @Suppress("UNCHECKED_CAST")
                                    val currentPlayers = document.get("players") as? List<Map<String, Any>> ?: emptyList()
                                    val updatedPlayers = currentPlayers.filter { it["uid"] != auth.currentUser?.uid }
                                    db.collection("rooms").document(roomId).update("players", updatedPlayers + newPlayer)
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
                                // val currentPlayer = mapOf("uid" to auth.currentUser?.uid, "name" to hostName) // Unused
                                val newPlayer = mapOf("uid" to auth.currentUser?.uid, "name" to hostName, "team" to "blue", "role" to "player")
                                db.collection("rooms").document(roomId).get().addOnSuccessListener { document ->
                                    @Suppress("UNCHECKED_CAST")
                                    val currentPlayers = document.get("players") as? List<Map<String, Any>> ?: emptyList()
                                    val updatedPlayers = currentPlayers.filter { it["uid"] != auth.currentUser?.uid }
                                    db.collection("rooms").document(roomId).update("players", updatedPlayers + newPlayer)
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
                        // val hostUid = auth.currentUser?.uid // auth.currentUser?.uid is already available

                        if (startingPlayerUid.isNullOrEmpty() || auth.currentUser?.uid.isNullOrEmpty()) {
                            Log.e("HostLobby", "Starting player UID or host UID is null. Cannot start game.")
                            return@launch
                        }

                        val updatesToSendToFirestore = mutableMapOf<String, Any?>(
                            "status" to "started"
                        )

                        if (gameId == "whereandwhen") {
                            updatesToSendToFirestore["gameState.whereandwhen.roundStartTimeMillis"] = System.currentTimeMillis()
                            updatesToSendToFirestore["gameState.whereandwhen.roundStatus"] = WhereAndWhenGameState.STATUS_GUESSING

                        } else {
                            val initialGameStateForOtherGames = when (gameId) {
                                "battleships" -> mapOf(
                                    "player1Id" to players.getOrNull(0)?.get("uid"),
                                    "player2Id" to players.getOrNull(1)?.get("uid"),
                                    "currentTurn" to startingPlayerUid,
                                    "moves" to emptyList<String>(),
                                    "powerUps" to players.associate { (it["uid"] as? String ?: "") to listOf("RADAR", "BOMB") },
                                    "energy" to players.associate { (it["uid"] as? String ?: "") to 3 },
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
                                "triviatoe" -> mapOf(
                                    "players"      to players,
                                    "board"        to emptyList<Map<String, Any>>(),
                                    "moves"        to emptyList<Map<String, Any>>(),
                                    "currentRound" to 0,
                                    "quizQuestion" to null,
                                    "answers"      to emptyMap<String, Any>(),
                                    "firstToMove"  to null,
                                    "currentTurn"  to startingPlayerUid,
                                    "winner"       to null,
                                    "state"        to "QUESTION",
                                    "usedQuestions" to emptyList<Int>()
                                )
                                "codenames" -> CodenamesService.generateGameState()
                                else -> emptyMap()
                            }

                            if (initialGameStateForOtherGames.isNotEmpty()) {
                                updatesToSendToFirestore["gameState.$gameId"] = initialGameStateForOtherGames
                            }

                            if(gameId == "battleships") {
                                val rematchVotes = players.associate {
                                    val uid = it["uid"] as? String ?: ""
                                    uid to false
                                }
                                updatesToSendToFirestore["rematchVotes"] = rematchVotes
                            }
                        }

                        try {
                            Log.d("HostLobby", "Updating Firestore for game start. Updates: $updatesToSendToFirestore")
                            db.collection("rooms").document(roomId).update(updatesToSendToFirestore)
                                .addOnSuccessListener {
                                    println("✅ Game started successfully (HostLobbyScreen)")
                                    Log.d("HostLobby", "Firestore update SUCCESS. Room status: started. GameId: $gameId")
                                }.addOnFailureListener { e ->
                                    println("❌ Failed to start game: ${e.message}")
                                    Log.e("HostLobby", "Firestore update FAILED for starting game. GameId: $gameId", e)
                                }
                        } catch (e: Exception) {
                            println("🔥 Exception during game start: ${e.message}")
                            Log.e("HostLobby", "EXCEPTION during game start. GameId: $gameId", e)
                        }
                    }
                },
                enabled = status == "waiting" &&
                        when (gameId) {
                            "whereandwhen" -> players.size in 2..maxPlayers
                            "battleships" -> players.size == maxPlayers
                            "ohpardon" -> players.size >= 2 && players.size <= maxPlayers
                            "triviatoe" -> players.size == maxPlayers
                            "codenames" -> players.size >= 4 && players.size <= maxPlayers
                            else -> players.size >= 2 && players.size <= maxPlayers // Default for other games
                        }
            ) {
                // BUTTON TEXT LOGIC
                val canStartNow = when (gameId) {
                    "whereandwhen" -> players.size in 2..maxPlayers
                    "battleships" -> players.size == maxPlayers
                    "ohpardon" -> players.size >= 2 && players.size <= maxPlayers
                    "triviatoe" -> players.size == maxPlayers
                    "codenames" -> players.size >= 4 && players.size <= maxPlayers
                    else -> players.size >= 2 && players.size <= maxPlayers
                }
                Text(
                    if (status != "waiting") "Game In Progress..."
                    else if (canStartNow) "Start Game (${players.size}/$maxPlayers)"
                    else {
                        val minPlayersRequired = when (gameId) {
                            "whereandwhen" -> 2
                            "battleships" -> 2
                            "ohpardon" -> 2
                            "triviatoe" -> 2
                            "codenames" -> 4
                            else -> 2
                        }
                        if (players.size < minPlayersRequired) "Waiting for more players... (${players.size}/$maxPlayers, minimum $minPlayersRequired)"
                        else "Waiting for players... (${players.size}/$maxPlayers)" // If min met but not max for games that require max
                    }
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
                        "battleships" -> NavRoutes.BATTLE_VOTE
                        "ohpardon"    -> NavRoutes.OHPARDON_GAME
                        "triviatoe" -> NavRoutes.TRIVIATOE_INTRO_ANIM
                        "whereandwhen" -> NavRoutes.WHERE_AND_WHEN_GAME
                        "codenames"   -> {
                            val currentPlayer = players.find { it["uid"] == auth.currentUser?.uid }
                            val isMaster = currentPlayer?.get("role") == "master"
                            Log.d("CodenamesHostDebug", """
                                HostLobbyScreen - Starting CodenamesActivity:
                                currentPlayer: $currentPlayer
                                isMaster: $isMaster
                                team: ${currentPlayer?.get("team")}
                            """.trimIndent())
                            val intent = Intent(context, CodenamesActivity::class.java).apply {
                                putExtra("roomId", roomId)
                                putExtra("userName", hostName)
                                putExtra("isMaster", isMaster)
                                putExtra("team", currentPlayer?.get("team") as? String ?: "")
                            }
                            context.startActivity(intent)
                            null // Prevent further navigation in this LaunchedEffect
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