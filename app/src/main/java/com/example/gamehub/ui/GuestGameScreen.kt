package com.example.gamehub.ui

import GameBoxFontFamily
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.gamehub.features.codenames.ui.CodenamesActivity
import com.example.gamehub.navigation.NavRoutes
import com.example.gamehub.ui.components.NinePatchBorder
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun GuestGameScreen(
    navController: NavController,
    gameId: String,
    code: String,
    userName: String,
    context: Context
) {
    val db = Firebase.firestore
    val auth = Firebase.auth
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("waiting") }
    var players by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var roomName by remember { mutableStateOf<String?>(null) }
    var maxPlayers by remember { mutableStateOf(0) }
    var roomDeleted by remember { mutableStateOf(false) }

    // Add player as spectator when they first join
    LaunchedEffect(Unit) {
        val currentPlayer = mapOf(
            "uid" to auth.currentUser?.uid,
            "name" to userName,
            "team" to "spectator",
            "role" to "player"
        )
        db.collection("rooms").document(code).get().addOnSuccessListener { document ->
            @Suppress("UNCHECKED_CAST")
            val currentPlayers = document.get("players") as? List<Map<String, Any>> ?: emptyList()
            if (!currentPlayers.any { it["uid"] == auth.currentUser?.uid }) {
                db.collection("rooms").document(code).update("players", currentPlayers + currentPlayer)
            }
        }
    }

    // Listen for room status and players, and detect room deletion
    LaunchedEffect(code) {
        db.collection("rooms").document(code)
            .addSnapshotListener { snap, _ ->
                if (snap == null || !snap.exists()) {
                    roomDeleted = true
                } else {
                    status = snap.getString("status") ?: "waiting"
                    roomName = snap.getString("name")
                    maxPlayers = snap.getLong("maxPlayers")?.toInt() ?: 0
                    @Suppress("UNCHECKED_CAST")
                    players = snap.get("players") as? List<Map<String, Any>> ?: emptyList()
                }
            }
    }

    LaunchedEffect(roomDeleted) {
        if (roomDeleted) {
            navController.navigate(NavRoutes.LOBBY_MENU.replace("{gameId}", gameId)) {
                popUpTo(0)
                launchSingleTop = true
            }
        }
    }

    // When status flips to started, navigate this guest into the correct game
    LaunchedEffect(status) {
        if (status == "started") {
            val route = when (gameId) {
                "battleships" -> NavRoutes.BATTLE_VOTE
                "ohpardon"    -> NavRoutes.OHPARDON_GAME
                "whereandwhen" -> NavRoutes.WHERE_AND_WHEN_GAME
                "triviatoe" -> NavRoutes.TRIVIATOE_INTRO_ANIM
                "codenames"   -> {
                    val currentPlayer = players.find { it["uid"] == auth.currentUser?.uid }
                    val isMaster = currentPlayer?.get("role") == "master"
                    val team = currentPlayer?.get("team") as? String ?: ""
                    Log.d("CodenamesDebug", """
                        GuestGameScreen Values:
                        currentPlayer: $currentPlayer
                        isMaster: $isMaster
                        team: $team
                        players: $players
                    """.trimIndent())
                    val intent = Intent(context, CodenamesActivity::class.java).apply {
                        putExtra("roomId", code)
                        putExtra("userName", userName)
                        putExtra("isMaster", isMaster)
                        putExtra("team", team)
                    }
                    context.startActivity(intent)
                    null
                }
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

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = com.example.gamehub.R.drawable.game_box_bg1),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 18.dp)
                        .heightIn(min = 220.dp)
                ) {
                    NinePatchBorder(
                        modifier = Modifier.matchParentSize(),
                        drawableRes = com.example.gamehub.R.drawable.game_list_border
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Room name centered, then room code, then player count, then status
                        Text(
                            text = "Room: ${roomName ?: code}",
                            fontFamily = GameBoxFontFamily,
                            fontSize = 26.sp,
                            color = Color(0xFFc08cdc),
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "Room Code: $code",
                            fontFamily = GameBoxFontFamily,
                            fontSize = 16.sp,
                            color = Color(0xFFc08cdc),
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${players.size} / $maxPlayers players",
                            fontFamily = GameBoxFontFamily,
                            fontSize = 18.sp,
                            color = Color(0xFFc08cdc),
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            when {
                                status != "waiting" -> "Game in progress..."
                                players.size < minPlayersRequired(gameId) -> {
                                    val min = minPlayersRequired(gameId)
                                    "Waiting for more players... (minimum $min)"
                                }
                                else -> "Ready to start!"
                            },
                            fontFamily = GameBoxFontFamily,
                            fontSize = 16.sp,
                            color = Color(0xFFc08cdc),
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))

                        // --- Player/team code for codenames ---
                        if (gameId == "codenames") {
                            val redTeam = players.filter { (it["team"] ?: "spectator") == "red" }
                            val blueTeam = players.filter { (it["team"] ?: "spectator") == "blue" }
                            val spectators = players.filter { (it["team"] ?: "spectator") == "spectator" }

                            Text("Spectators", fontFamily = GameBoxFontFamily, fontSize = 19.sp, color = Color(0xFFc08cdc), modifier = Modifier.align(Alignment.CenterHorizontally), textAlign = TextAlign.Center)
                            spectators.forEach { player ->
                                val name = player["name"] as? String ?: ""
                                Text("• $name", fontFamily = GameBoxFontFamily, modifier = Modifier.align(Alignment.CenterHorizontally), textAlign = TextAlign.Center)
                            }
                            
                            // Add team join buttons for spectators
                            val currentPlayer = players.find { it["uid"] == auth.currentUser?.uid }
                            Log.d("CodenamesDebug", """
                                GuestGameScreen Team Selection Debug:
                                Current Player: $currentPlayer
                                Current Player Team: ${currentPlayer?.get("team")}
                                Is Spectator: ${currentPlayer?.get("team") == "spectator"}
                                Is Null: ${currentPlayer == null}
                                All Players: $players
                                Red Team Size: ${redTeam.size}
                                Blue Team Size: ${blueTeam.size}
                            """.trimIndent())
                            
                            // Show team join buttons if player is not on any team
                            if (currentPlayer?.get("team") == null || currentPlayer?.get("team") == "spectator") {
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Button(
                                        onClick = {
                                            Log.d("CodenamesDebug", "Attempting to join Red Team")
                                            val newPlayer = mapOf(
                                                "uid" to auth.currentUser?.uid,
                                                "name" to userName,
                                                "team" to "red",
                                                "role" to "player"
                                            )
                                            db.collection("rooms").document(code).get().addOnSuccessListener { document ->
                                                @Suppress("UNCHECKED_CAST")
                                                val currentPlayers = document.get("players") as? List<Map<String, Any>> ?: emptyList()
                                                val updatedPlayers = currentPlayers.filter { it["uid"] != auth.currentUser?.uid }
                                                db.collection("rooms").document(code).update("players", updatedPlayers + newPlayer)
                                                    .addOnSuccessListener {
                                                        Log.d("CodenamesDebug", "Successfully joined Red Team")
                                                    }
                                                    .addOnFailureListener { e ->
                                                        Log.e("CodenamesDebug", "Failed to join Red Team", e)
                                                    }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE84855))
                                    ) {
                                        Text("Join Red Team", fontFamily = GameBoxFontFamily)
                                    }
                                    
                                    Button(
                                        onClick = {
                                            Log.d("CodenamesDebug", "Attempting to join Blue Team")
                                            val newPlayer = mapOf(
                                                "uid" to auth.currentUser?.uid,
                                                "name" to userName,
                                                "team" to "blue",
                                                "role" to "player"
                                            )
                                            db.collection("rooms").document(code).get().addOnSuccessListener { document ->
                                                @Suppress("UNCHECKED_CAST")
                                                val currentPlayers = document.get("players") as? List<Map<String, Any>> ?: emptyList()
                                                val updatedPlayers = currentPlayers.filter { it["uid"] != auth.currentUser?.uid }
                                                db.collection("rooms").document(code).update("players", updatedPlayers + newPlayer)
                                                    .addOnSuccessListener {
                                                        Log.d("CodenamesDebug", "Successfully joined Blue Team")
                                                    }
                                                    .addOnFailureListener { e ->
                                                        Log.e("CodenamesDebug", "Failed to join Blue Team", e)
                                                    }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A86FF))
                                    ) {
                                        Text("Join Blue Team", fontFamily = GameBoxFontFamily)
                                    }
                                }
                            }
                            
                            Spacer(Modifier.height(20.dp))

                            Text("Red Team", fontFamily = GameBoxFontFamily, color = Color(0xFFE84855), fontSize = 19.sp, modifier = Modifier.align(Alignment.CenterHorizontally), textAlign = TextAlign.Center)
                            redTeam.forEach { player ->
                                val name = player["name"] as? String ?: ""
                                val isMaster = player["role"] == "master"
                                val isCurrentPlayer = player["uid"] == auth.currentUser?.uid
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("• $name ${if (isMaster) "(Master)" else "(Player)"}", 
                                        fontFamily = GameBoxFontFamily, 
                                        modifier = Modifier.align(Alignment.CenterVertically),
                                        textAlign = TextAlign.Center
                                    )
                                    if (isCurrentPlayer && !isMaster && redTeam.none { it["role"] == "master" }) {
                                        Spacer(Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                val newPlayer = mapOf(
                                                    "uid" to auth.currentUser?.uid,
                                                    "name" to userName,
                                                    "team" to "red",
                                                    "role" to "master"
                                                )
                                                db.collection("rooms").document(code).get().addOnSuccessListener { document ->
                                                    @Suppress("UNCHECKED_CAST")
                                                    val currentPlayers = document.get("players") as? List<Map<String, Any>> ?: emptyList()
                                                    val updatedPlayers = currentPlayers.filter { it["uid"] != auth.currentUser?.uid }
                                                    db.collection("rooms").document(code).update("players", updatedPlayers + newPlayer)
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE84855))
                                        ) {
                                            Text("Become Master", fontFamily = GameBoxFontFamily)
                                        }
                                    }
                                }
                            }
                            
                            Spacer(Modifier.height(18.dp))

                            Text("Blue Team", fontFamily = GameBoxFontFamily, color = Color(0xFF3A86FF), fontSize = 19.sp, modifier = Modifier.align(Alignment.CenterHorizontally), textAlign = TextAlign.Center)
                            blueTeam.forEach { player ->
                                val name = player["name"] as? String ?: ""
                                val isMaster = player["role"] == "master"
                                val isCurrentPlayer = player["uid"] == auth.currentUser?.uid
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("• $name ${if (isMaster) "(Master)" else "(Player)"}", 
                                        fontFamily = GameBoxFontFamily, 
                                        modifier = Modifier.align(Alignment.CenterVertically),
                                        textAlign = TextAlign.Center
                                    )
                                    if (isCurrentPlayer && !isMaster && blueTeam.none { it["role"] == "master" }) {
                                        Spacer(Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                val newPlayer = mapOf(
                                                    "uid" to auth.currentUser?.uid,
                                                    "name" to userName,
                                                    "team" to "blue",
                                                    "role" to "master"
                                                )
                                                db.collection("rooms").document(code).get().addOnSuccessListener { document ->
                                                    @Suppress("UNCHECKED_CAST")
                                                    val currentPlayers = document.get("players") as? List<Map<String, Any>> ?: emptyList()
                                                    val updatedPlayers = currentPlayers.filter { it["uid"] != auth.currentUser?.uid }
                                                    db.collection("rooms").document(code).update("players", updatedPlayers + newPlayer)
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A86FF))
                                        ) {
                                            Text("Become Master", fontFamily = GameBoxFontFamily)
                                        }
                                    }
                                }
                            }
                        } else {
                            // Just show all player names, centered
                            players.forEach { player ->
                                val name = player["name"] as? String ?: ""
                                Text("• $name", fontFamily = GameBoxFontFamily, modifier = Modifier.align(Alignment.CenterHorizontally), textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }

            // LEAVE BUTTON
            Spacer(Modifier.height(16.dp))
            SpriteMenuButton(
                text = "Leave Room",
                onClick = {
                    // Remove this player
                    db.collection("rooms").document(code)
                        .get()
                        .addOnSuccessListener { document ->
                            @Suppress("UNCHECKED_CAST")
                            val currentPlayers = document.get("players") as? List<Map<String, Any>> ?: emptyList()
                            val currentPlayer = currentPlayers.find { it["uid"] == Firebase.auth.uid }
                            db.collection("rooms").document(code)
                                .update(
                                    "players",
                                    FieldValue.arrayRemove(currentPlayer)
                                )
                                .addOnSuccessListener { navController.popBackStack() }
                        }
                },
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .height(70.dp),
                normalRes = com.example.gamehub.R.drawable.menu_button_long,
                pressedRes = com.example.gamehub.R.drawable.menu_button_long_pressed,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = GameBoxFontFamily,
                    fontSize = 24.sp
                )
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun minPlayersRequired(gameId: String) = when (gameId) {
    "whereandwhen" -> 2
    "battleships" -> 2
    "ohpardon" -> 2
    "triviatoe" -> 2
    "codenames" -> 1
    else -> 2
}

private fun canStartGame(gameId: String, players: List<Map<String, Any>>, maxPlayers: Int): Boolean = when (gameId) {
    "whereandwhen" -> players.size in 2..maxPlayers
    "battleships" -> players.size == maxPlayers
    "ohpardon" -> players.size >= 2 && players.size <= maxPlayers
    "triviatoe" -> players.size == maxPlayers
    "codenames" -> players.size >= 1 && players.size <= maxPlayers
    else -> players.size >= 2 && players.size <= maxPlayers
}
