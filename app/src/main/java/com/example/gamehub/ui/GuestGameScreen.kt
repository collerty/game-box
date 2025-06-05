// app/src/main/java/com/example/gamehub/ui/GuestGameScreen.kt
package com.example.gamehub.ui

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gamehub.navigation.NavRoutes
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import kotlinx.coroutines.launch

@Composable
fun GuestGameScreen(
    navController: NavController,
    gameId: String,
    code: String,
    userName: String
) {
    val db = Firebase.firestore
    val auth = Firebase.auth
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("waiting") }
    var players by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

    // Listen for room status and players
    LaunchedEffect(code) {
        db.collection("rooms").document(code)
            .addSnapshotListener { snap, _ ->
                if (snap != null && snap.exists()) {
                    status = snap.getString("status") ?: "waiting"
                    @Suppress("UNCHECKED_CAST")
                    players = snap.get("players") as? List<Map<String, Any>> ?: emptyList()
                }
            }
    }

    // When status flips to started, navigate *this* guest into vote
    LaunchedEffect(status) {
        if (status == "started") {
            val route = when (gameId) {
                "battleships" -> NavRoutes.BATTLE_VOTE
                "ohpardon"    -> NavRoutes.OHPARDON_GAME
                "codenames"   -> NavRoutes.CODENAMES_GAME
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

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        if (gameId == "codenames") {
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
                                "name" to userName
                            )
                            val newPlayer = mapOf(
                                "uid" to auth.currentUser?.uid,
                                "name" to userName,
                                "team" to "red",
                                "role" to "master"
                            )
                            
                            // First remove the player from any team
                            db.collection("rooms").document(code)
                                .get()
                                .addOnSuccessListener { document ->
                                    @Suppress("UNCHECKED_CAST")
                                    val currentPlayers = document.get("players") as? List<Map<String, Any>> ?: emptyList()
                                    val updatedPlayers = currentPlayers.filter { it["uid"] != auth.currentUser?.uid }
                                    
                                    // Then add the player to the new role
                                    db.collection("rooms").document(code)
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
                                "name" to userName
                            )
                            val newPlayer = mapOf(
                                "uid" to auth.currentUser?.uid,
                                "name" to userName,
                                "team" to "red",
                                "role" to "player"
                            )
                            
                            // First remove the player from any team
                            db.collection("rooms").document(code)
                                .get()
                                .addOnSuccessListener { document ->
                                    @Suppress("UNCHECKED_CAST")
                                    val currentPlayers = document.get("players") as? List<Map<String, Any>> ?: emptyList()
                                    val updatedPlayers = currentPlayers.filter { it["uid"] != auth.currentUser?.uid }
                                    
                                    // Then add the player to the new role
                                    db.collection("rooms").document(code)
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
                                "name" to userName
                            )
                            val newPlayer = mapOf(
                                "uid" to auth.currentUser?.uid,
                                "name" to userName,
                                "team" to "blue",
                                "role" to "master"
                            )
                            
                            // First remove the player from any team
                            db.collection("rooms").document(code)
                                .get()
                                .addOnSuccessListener { document ->
                                    @Suppress("UNCHECKED_CAST")
                                    val currentPlayers = document.get("players") as? List<Map<String, Any>> ?: emptyList()
                                    val updatedPlayers = currentPlayers.filter { it["uid"] != auth.currentUser?.uid }
                                    
                                    // Then add the player to the new role
                                    db.collection("rooms").document(code)
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
                                "name" to userName
                            )
                            val newPlayer = mapOf(
                                "uid" to auth.currentUser?.uid,
                                "name" to userName,
                                "team" to "blue",
                                "role" to "player"
                            )
                            
                            // First remove the player from any team
                            db.collection("rooms").document(code)
                                .get()
                                .addOnSuccessListener { document ->
                                    @Suppress("UNCHECKED_CAST")
                                    val currentPlayers = document.get("players") as? List<Map<String, Any>> ?: emptyList()
                                    val updatedPlayers = currentPlayers.filter { it["uid"] != auth.currentUser?.uid }
                                    
                                    // Then add the player to the new role
                                    db.collection("rooms").document(code)
                                        .update("players", updatedPlayers + newPlayer)
                                }
                        }
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        } else {
            Text("Waiting for host to start…")
            Spacer(Modifier.height(8.dp))
        }
        Button(onClick = {
            db.collection("rooms").document(code)
                .update(
                    "players",
                    FieldValue.arrayRemove(
                        mapOf("uid" to Firebase.auth.uid, "name" to userName)
                    )
                )
                .addOnSuccessListener { navController.popBackStack() }
        }) {
            Text("Leave Room")
        }
    }
}
