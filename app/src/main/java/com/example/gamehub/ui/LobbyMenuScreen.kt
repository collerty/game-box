package com.example.gamehub.ui

import GameBoxFontFamily
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.gamehub.lobby.LobbyService
import com.example.gamehub.navigation.NavRoutes
import com.example.gamehub.ui.components.NinePatchBorder
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private enum class Mode { HOST, JOIN }

@RequiresApi(Build.VERSION_CODES.O)
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
    val availableColors = listOf("Red", "Blue", "Green", "Yellow")
    var selectedColor by remember { mutableStateOf("Red") }

    LaunchedEffect(Unit) {
        context.stopService(Intent(context, com.example.gamehub.MusicService::class.java))
    }

    // live list of waiting rooms for this game
    val rooms by LobbyService
        .publicRoomsFlow(gameId)
        .collectAsState(initial = emptyList())

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = com.example.gamehub.R.drawable.game_box_bg1),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // All form & rooms content is scrollable together!
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ----- TOP FORM SECTION IN BORDER -----
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
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
                        Text(
                            text = "${gameId.replaceFirstChar { it.uppercaseChar() }}",
                            fontFamily = GameBoxFontFamily,
                            fontSize = 28.sp,
                            color = Color(0xFFc08cdc)
                        )
                        Spacer(Modifier.height(12.dp))
                        // Host / Join Toggle
                        Row(
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PixelCheckbox(selected = (mode == Mode.HOST), onClick = { mode = Mode.HOST })
                            Text("Host", fontFamily = GameBoxFontFamily, fontSize = 18.sp, modifier = Modifier.padding(end = 20.dp))
                            PixelCheckbox(selected = (mode == Mode.JOIN), onClick = { mode = Mode.JOIN })
                            Text("Join", fontFamily = GameBoxFontFamily, fontSize = 18.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        // Username
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Your username") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(0.96f)
                        )
                        Spacer(Modifier.height(10.dp))
                        if (mode == Mode.HOST) {
                            OutlinedTextField(
                                value = roomName,
                                onValueChange = { roomName = it },
                                label = { Text("Room name (required)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(0.96f)
                            )
                        } else {
                            OutlinedTextField(
                                value = roomCode,
                                onValueChange = { roomCode = it },
                                label = { Text("Room code") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(0.96f)
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(0.96f)
                        )
                        Spacer(Modifier.height(10.dp))
                        // Color Picker (centered squares, NO text)
                        if (gameId == "ohpardon") {
                            Text("Pick a Color:", fontFamily = GameBoxFontFamily, fontSize = 17.sp)
                            Row(
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                availableColors.forEach { color ->
                                    PixelCheckbox(
                                        selected = selectedColor == color,
                                        onClick = { selectedColor = color },
                                        color = color
                                    )
                                    Spacer(Modifier.width(12.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                        // ACTION BUTTON -- SpriteMenuButton for Create/Join Room
                        SpriteMenuButton(
                            text = if (mode == Mode.HOST) "Create Room" else "Join Room",
                            onClick = {
                                if (username.isBlank()) {
                                    Toast.makeText(context, "Enter a username", Toast.LENGTH_SHORT).show()
                                    return@SpriteMenuButton
                                }

                                scope.launch {
                                    try {
                                        if (mode == Mode.HOST) {
                                            if (roomName.isBlank()) {
                                                Toast.makeText(context, "Room name is required", Toast.LENGTH_SHORT).show()
                                                return@launch
                                            }

                                            val code = LobbyService.host(
                                                gameId   = gameId,
                                                roomName = roomName,
                                                hostName = username,
                                                password = password.takeIf { it.isNotBlank() },
                                                selectedColor = selectedColor
                                            )

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

                                            try {
                                                val joinedGame = LobbyService.join(
                                                    code = roomCode,
                                                    userName = username,
                                                    password = password.takeIf { it.isNotBlank() },
                                                    selectedColor = selectedColor
                                                )

                                                if (joinedGame != gameId) {
                                                    Toast.makeText(
                                                        context,
                                                        "Bad code or password, or room full",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    return@launch
                                                }
                                            } catch (e: IllegalStateException) {
                                                when (e.message) {
                                                    "ColorAlreadyTaken" -> {
                                                        Toast.makeText(context, "Color already taken", Toast.LENGTH_SHORT).show()
                                                    }
                                                    else -> {
                                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                                return@launch
                                            }

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
                    }
                }

                // ---- Available Rooms Section in BORDER (extends down) ----
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    NinePatchBorder(
                        modifier = Modifier.matchParentSize(),
                        drawableRes = com.example.gamehub.R.drawable.game_list_border
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Available rooms:", fontFamily = GameBoxFontFamily, fontSize = 20.sp, color = Color(0xFFc08cdc))
                        Spacer(Modifier.height(8.dp))
                        rooms.forEach { room ->
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
                                    fontFamily = GameBoxFontFamily,
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

            // The BACK BUTTON always at the end of the screen!
            Spacer(Modifier.height(18.dp))
            SpriteMenuButton(
                text = "Back",
                onClick = { navController.popBackStack() },
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

// --- Custom Pixel Checkbox (square) ---
@Composable
fun PixelCheckbox(
    selected: Boolean,
    onClick: () -> Unit,
    color: String? = null // can use for custom coloring if needed
) {
    val borderRes = com.example.gamehub.R.drawable.border_game_icon // Your pixel border
    Box(
        modifier = Modifier
            .size(32.dp)
            .padding(2.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        NinePatchBorder(
            modifier = Modifier.matchParentSize(),
            drawableRes = borderRes
        )
        if (selected) {
            Box(
                Modifier
                    .fillMaxSize(0.7f)
                    .padding(2.dp)
                    .background(colorForPixelBox(color))
            )
        }
    }
}

// Helper for pixel box color (customize to your color palette)
@Composable
fun colorForPixelBox(color: String?): Color {
    return when (color) {
        "Red" -> Color(0xFFE84855)
        "Blue" -> Color(0xFF3A86FF)
        "Green" -> Color(0xFF00C896)
        "Yellow" -> Color(0xFFFFE066)
        else -> Color(0xFFc08cdc)
    }
}
