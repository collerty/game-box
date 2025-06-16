import GameBoxFontFamily
import android.content.Intent
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.gamehub.features.codenames.service.CodenamesService
import com.example.gamehub.features.codenames.ui.CodenamesActivity
import com.example.gamehub.navigation.NavRoutes
import com.example.gamehub.ui.components.NinePatchBorder
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import com.example.gamehub.features.whereandwhe.model.WhereAndWhenGameState
import com.example.gamehub.ui.PixelCheckbox
import com.example.gamehub.ui.SpriteMenuButton
import kotlinx.coroutines.tasks.await
import android.util.Log

@RequiresApi(Build.VERSION_CODES.O)
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

    LaunchedEffect(Unit) {
        context.stopService(Intent(context, com.example.gamehub.MusicService::class.java))
    }

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

    BackHandler { showExitDialog = true }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = com.example.gamehub.R.drawable.game_box_bg1),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // LOBBY BOX
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .heightIn(min = 250.dp)
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
                        if (!showExitDialog) {
                            Text(
                                text = "Room: ${roomName ?: roomId}",
                                fontFamily = GameBoxFontFamily,
                                fontSize = 26.sp,
                                color = Color(0xFFc08cdc),
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(3.dp))
                            // Room number centered under name
                            Text(
                                text = "Room Code: $roomId",
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
                                    !canStartGame(gameId, players, maxPlayers) -> {
                                        val min = minPlayersRequired(gameId)
                                        if (players.size < min)
                                            "Waiting for more players... (minimum $min)"
                                        else
                                            "Waiting for players..."
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
                            // --- PLAYER/TEAM UI FOR CODENAMES ---
                            if (gameId == "codenames") {
                                val authUid = auth.currentUser?.uid
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
                                    HostLobbyScreen Team Selection Debug:
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
                                                Log.d("CodenamesDebug", "Host attempting to join Red Team")
                                                val newPlayer = mapOf(
                                                    "uid" to auth.currentUser?.uid,
                                                    "name" to hostName,
                                                    "team" to "red",
                                                    "role" to "player"
                                                )
                                                db.collection("rooms").document(roomId).get().addOnSuccessListener { document ->
                                                    @Suppress("UNCHECKED_CAST")
                                                    val currentPlayers = document.get("players") as? List<Map<String, Any>> ?: emptyList()
                                                    val updatedPlayers = currentPlayers.filter { it["uid"] != auth.currentUser?.uid }
                                                    db.collection("rooms").document(roomId).update("players", updatedPlayers + newPlayer)
                                                        .addOnSuccessListener {
                                                            Log.d("CodenamesDebug", "Host successfully joined Red Team")
                                                        }
                                                        .addOnFailureListener { e ->
                                                            Log.e("CodenamesDebug", "Host failed to join Red Team", e)
                                                        }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE84855))
                                        ) {
                                            Text("Join Red Team", fontFamily = GameBoxFontFamily)
                                        }
                                        
                                        Button(
                                            onClick = {
                                                Log.d("CodenamesDebug", "Host attempting to join Blue Team")
                                                val newPlayer = mapOf(
                                                    "uid" to auth.currentUser?.uid,
                                                    "name" to hostName,
                                                    "team" to "blue",
                                                    "role" to "player"
                                                )
                                                db.collection("rooms").document(roomId).get().addOnSuccessListener { document ->
                                                    @Suppress("UNCHECKED_CAST")
                                                    val currentPlayers = document.get("players") as? List<Map<String, Any>> ?: emptyList()
                                                    val updatedPlayers = currentPlayers.filter { it["uid"] != auth.currentUser?.uid }
                                                    db.collection("rooms").document(roomId).update("players", updatedPlayers + newPlayer)
                                                        .addOnSuccessListener {
                                                            Log.d("CodenamesDebug", "Host successfully joined Blue Team")
                                                        }
                                                        .addOnFailureListener { e ->
                                                            Log.e("CodenamesDebug", "Host failed to join Blue Team", e)
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
                                                        "name" to hostName,
                                                        "team" to "red",
                                                        "role" to "master"
                                                    )
                                                    db.collection("rooms").document(roomId).get().addOnSuccessListener { document ->
                                                        @Suppress("UNCHECKED_CAST")
                                                        val currentPlayers = document.get("players") as? List<Map<String, Any>> ?: emptyList()
                                                        val updatedPlayers = currentPlayers.filter { it["uid"] != auth.currentUser?.uid }
                                                        db.collection("rooms").document(roomId).update("players", updatedPlayers + newPlayer)
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
                                                        "name" to hostName,
                                                        "team" to "blue",
                                                        "role" to "master"
                                                    )
                                                    db.collection("rooms").document(roomId).get().addOnSuccessListener { document ->
                                                        @Suppress("UNCHECKED_CAST")
                                                        val currentPlayers = document.get("players") as? List<Map<String, Any>> ?: emptyList()
                                                        val updatedPlayers = currentPlayers.filter { it["uid"] != auth.currentUser?.uid }
                                                        db.collection("rooms").document(roomId).update("players", updatedPlayers + newPlayer)
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
                                players.forEach { player ->
                                    val name = player["name"] as? String ?: ""
                                    Text("• $name", fontFamily = GameBoxFontFamily, modifier = Modifier.align(Alignment.CenterHorizontally), textAlign = TextAlign.Center)
                                }
                            }
                        } else {
                            // --- DIALOG INSIDE THE PANEL ---
                            Spacer(Modifier.height(24.dp))
                            Text(
                                "Close Room?",
                                fontFamily = GameBoxFontFamily,
                                fontSize = 23.sp,
                                color = Color(0xFFC08CDC),
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(14.dp))
                            Text(
                                "Closing the lobby will disconnect all players.",
                                fontFamily = GameBoxFontFamily,
                                fontSize = 17.sp,
                                color = Color(0xFFC08CDC),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.CenterHorizontally),
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(26.dp))
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                SpriteMenuButton(
                                    text = "Cancel",
                                    onClick = { showExitDialog = false },
                                    modifier = Modifier.weight(1f).height(52.dp),
                                    normalRes = com.example.gamehub.R.drawable.menu_button_long,
                                    pressedRes = com.example.gamehub.R.drawable.menu_button_long_pressed,
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontFamily = GameBoxFontFamily,
                                        fontSize = 18.sp,
                                    )
                                )
                                Spacer(Modifier.width(12.dp))
                                SpriteMenuButton(
                                    text = "Close Room",
                                    onClick = {
                                        showExitDialog = false
                                        scope.launch {
                                            try {
                                                val document = db.collection("rooms").document(roomId).get().await()
                                                @Suppress("UNCHECKED_CAST")
                                                val currentPlayers = document.get("players") as? List<Map<String, Any>> ?: emptyList()
                                                val updates = mutableMapOf<String, Any>()
                                                currentPlayers.forEach { player ->
                                                    updates["players"] = FieldValue.arrayRemove(player)
                                                }
                                                db.collection("rooms").document(roomId).update(updates).await()
                                                db.collection("rooms").document(roomId).delete().await()
                                                navController.popBackStack()
                                            } catch (e: Exception) {
                                                navController.popBackStack()
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(52.dp),
                                    normalRes = com.example.gamehub.R.drawable.menu_button_long,
                                    pressedRes = com.example.gamehub.R.drawable.menu_button_long_pressed,
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontFamily = GameBoxFontFamily,
                                        fontSize = 18.sp,
                                        color = Color(0xFFFF5555)
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                if (!showExitDialog) {
                    val enoughPlayers = canStartGame(gameId, players, maxPlayers)
                    val isButtonEnabled = enoughPlayers && status == "waiting"
                    SpriteMenuButton(
                        text = if (isButtonEnabled) "Start Game" else "${players.size} / $maxPlayers players",
                        onClick = {
                            if (!isButtonEnabled) return@SpriteMenuButton
                            scope.launch {
                                val startingPlayerUid = players.firstOrNull()?.get("uid") as? String
                                if (startingPlayerUid.isNullOrEmpty() || auth.currentUser?.uid.isNullOrEmpty()) return@launch

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
                                    db.collection("rooms").document(roomId).update(updatesToSendToFirestore)
                                        .addOnSuccessListener { }
                                } catch (_: Exception) { }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .height(70.dp)
                            .alpha(if (isButtonEnabled) 1f else 0.5f),
                        normalRes = com.example.gamehub.R.drawable.menu_button_long,
                        pressedRes = com.example.gamehub.R.drawable.menu_button_long_pressed,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = GameBoxFontFamily,
                            fontSize = 22.sp,
                            color = if (isButtonEnabled) Color(0xFFc08cdc) else Color(0xFFAAAAAA)
                        )
                    )

                    Spacer(Modifier.height(16.dp))

                    SpriteMenuButton(
                        text = "Close Room",
                        onClick = { showExitDialog = true },
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .height(64.dp),
                        normalRes = com.example.gamehub.R.drawable.menu_button_long,
                        pressedRes = com.example.gamehub.R.drawable.menu_button_long_pressed,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = GameBoxFontFamily,
                            fontSize = 20.sp,
                            color = Color(0xFFFF5555)
                        )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            LaunchedEffect(status, hostName) {
                if (status == "started" && hostName != null) {
                    val route = when (gameId) {
                        "battleships" -> NavRoutes.BATTLE_VOTE
                        "ohpardon"    -> NavRoutes.OHPARDON_GAME
                        "triviatoe"   -> NavRoutes.TRIVIATOE_INTRO_ANIM
                        "whereandwhen"-> NavRoutes.WHERE_AND_WHEN_GAME
                        "codenames"   -> {
                            val currentPlayer = players.find { it["uid"] == auth.currentUser?.uid }
                            val isMaster = currentPlayer?.get("role") == "master"
                            val intent = Intent(context, CodenamesActivity::class.java).apply {
                                putExtra("roomId", roomId)
                                putExtra("userName", hostName)
                                putExtra("isMaster", isMaster)
                                putExtra("team", currentPlayer?.get("team") as? String ?: "")
                            }
                            context.startActivity(intent)
                            null
                        }
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

// Helpers for min/max players (reuse from previous)
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
