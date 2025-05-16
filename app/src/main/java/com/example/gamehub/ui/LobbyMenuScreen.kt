package com.example.gamehub.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gamehub.lobby.LobbyService
import com.example.gamehub.lobby.LobbyService.RoomSummary
import com.example.gamehub.navigation.NavRoutes
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import androidx.compose.foundation.clickable

private enum class Mode { HOST, JOIN }

@Composable
fun LobbyMenuScreen(
    navController: NavController,
    gameId: String
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    // form state
    var mode     by remember { mutableStateOf(Mode.HOST) }
    var username by remember { mutableStateOf("") }
    var roomName by remember { mutableStateOf("") }
    var roomCode by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // live list of waiting rooms for this game
    val rooms by LobbyService
        .publicRoomsFlow(gameId)
        .collectAsState(initial = emptyList())

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Title
        Text(
            text = "Game: ${gameId.replaceFirstChar { it.uppercaseChar() }}",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(16.dp))

        // 1️⃣ Mode toggle
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = (mode == Mode.HOST), onClick = { mode = Mode.HOST })
            Text("Host", Modifier.padding(end = 24.dp))
            RadioButton(selected = (mode == Mode.JOIN), onClick = { mode = Mode.JOIN })
            Text("Join")
        }
        Spacer(Modifier.height(16.dp))

        // 2️⃣ Username (always required)
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Your username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        // 3️⃣ Room name (host) or code (join)
        if (mode == Mode.HOST) {
            OutlinedTextField(
                value = roomName,
                onValueChange = { roomName = it },
                label = { Text("Room name (required)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            OutlinedTextField(
                value = roomCode,
                onValueChange = { roomCode = it },
                label = { Text("Room code") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(12.dp))

        // 4️⃣ Optional password
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        // 5️⃣ Action button
        Button(
            onClick = {
                if (username.isBlank()) {
                    Toast.makeText(context, "Enter a username", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                scope.launch {
                    try {
                        if (mode == Mode.HOST) {
                            if (roomName.isBlank()) {
                                Toast.makeText(context, "Room name is required", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            // host & get code
                            val code = LobbyService.host(
                                gameId   = gameId,
                                roomName = roomName,
                                hostName = username,
                                password = password.takeIf { it.isNotBlank() }
                            )
                            // navigate into host lobby (carrying gameId + code)
                            navController.navigate(
                                NavRoutes.HOST_LOBBY
                                    .replace("{gameId}", gameId)
                                    .replace("{code}", code)
                            )
                        } else {
                            if (roomCode.isBlank()) {
                                Toast.makeText(context, "Enter the room code", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            val joinedGame = LobbyService.join(
                                code     = roomCode,
                                userName = username,
                                password = password.takeIf { it.isNotBlank() }
                            )
                            if (joinedGame != gameId) {
                                Toast.makeText(context, "Bad code or password, or room full", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            // navigate into guest UI (carrying gameId, code, userName)
                            val safeName = URLEncoder.encode(username, StandardCharsets.UTF_8.toString())
                            navController.navigate(
                                NavRoutes.GUEST_GAME
                                    .replace("{gameId}", gameId)
                                    .replace("{code}", roomCode)
                                    .replace("{userName}", safeName)
                            )
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (mode == Mode.HOST) "Create Room" else "Join Room")
        }

        Spacer(Modifier.height(24.dp))
        Divider()
        Spacer(Modifier.height(12.dp))

        // 6️⃣ Available rooms
        Text("Available rooms:", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(rooms) { room ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            roomCode = room.code
                            mode = Mode.JOIN
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${room.name}  (${room.currentPlayers}/${room.maxPlayers})",
                        modifier = Modifier.weight(1f)
                    )
                    if (room.hasPassword) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
