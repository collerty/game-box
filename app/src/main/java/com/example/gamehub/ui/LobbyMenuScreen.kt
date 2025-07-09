package com.example.gamehub.ui

import GameBoxFontFamily
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Text
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.gamehub.lobby.LobbyService
import com.example.gamehub.navigation.NavRoutes
import com.example.gamehub.ui.components.NinePatchBorder
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import com.example.gamehub.ui.GameBoxFontFamily

private enum class Mode { HOST, JOIN }

// --- Custom Floating Label Outlined TextField ---
@Composable
fun PixelOutlinedTextField(
    restingLabelYOffset: Dp = 14.dp,
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    textColor: Color = Color(0xFFc08cdc),
    bgColor: Color = Color(0xFF2A1536),
    borderColor: Color = Color(0xFFc08cdc),
    labelShape: RoundedCornerShape = RoundedCornerShape(4.dp),
    fontFamily: FontFamily = GameBoxFontFamily
) {
    var isFocused by remember { mutableStateOf(false) }
    val floatLabel by animateFloatAsState(
        targetValue = if (isFocused || value.isNotBlank()) 1f else 0f, label = ""
    )
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

    // Label offset & scaling
    val labelXRest = 18.dp     // When not floating: right
    val labelYRest = 14.dp     // When not floating: down
    val labelXFloating = 2.dp  // When floating: more left
    val labelYFloating = (-10).dp // When floating: up

    val labelScaleRest = 1.10f
    val labelScaleFloating = 0.82f

    val labelX = lerp(labelXRest, labelXFloating, floatLabel)
    val labelY = lerp(restingLabelYOffset, labelYFloating, floatLabel)
    val labelScale = lerp(labelScaleRest, labelScaleFloating, floatLabel)
    val labelPaddingH = lerp(12.dp, 6.dp, floatLabel)
    val labelPaddingV = lerp(4.dp, 5.dp, floatLabel)

    Box(
        modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
    ) {
        // Always show border for field
        Box(
            Modifier
                .background(bgColor, labelShape)
                .border(2.dp, borderColor, labelShape)
                .padding(top = 18.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    color = textColor,
                    fontFamily = fontFamily,
                    fontSize = 20.sp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }

        // Floating label: border only when floating
        Box(
            Modifier
                .offset(y = labelY, x = labelX)
                .scale(labelScale)
                .background(bgColor, labelShape)
                .then(
                    if (floatLabel > 0.5f)
                        Modifier.border(2.dp, borderColor, labelShape)
                    else
                        Modifier
                )
                .padding(horizontal = labelPaddingH, vertical = labelPaddingV)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { focusRequester.requestFocus() } // This line does the trick!
        ) {
            Text(
                label,
                color = borderColor,
                fontFamily = fontFamily,
                fontSize = 15.sp
            )
        }
    }
}

// Helper lerp functions
fun lerp(start: Dp, stop: Dp, fraction: Float): Dp = start + (stop - start) * fraction
fun lerp(start: Float, stop: Float, fraction: Float): Float = start + (stop - start) * fraction

// -- Main Screen --
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun LobbyMenuScreen(
    navController: NavController,
    gameId: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sharedTextColor = Color(0xFFc08cdc)
    val labelShape = RoundedCornerShape(4.dp)
    val textFieldBgColor = Color(0xFF2A1536)

    // form state
    var mode by remember { mutableStateOf(Mode.HOST) }
    var username by remember { mutableStateOf("") }
    var roomName by remember { mutableStateOf("") }
    var roomCode by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val availableColors = listOf("Red", "Blue", "Green", "Yellow")
    var selectedColor by remember { mutableStateOf("Red") }

    LaunchedEffect(Unit) {
        context.stopService(Intent(context, com.example.gamehub.MusicService::class.java))
    }

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
            modifier = Modifier.fillMaxSize()
        ) {
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
                            color = sharedTextColor
                        )
                        Spacer(Modifier.height(12.dp))
                        // Host / Join Toggle
                        Row(
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PixelCheckbox(selected = (mode == Mode.HOST), onClick = { mode = Mode.HOST })
                            Text("Host", fontFamily = GameBoxFontFamily, fontSize = 18.sp, modifier = Modifier.padding(end = 20.dp), color = Color(0xFFC08CDC))
                            PixelCheckbox(selected = (mode == Mode.JOIN), onClick = { mode = Mode.JOIN })
                            Text("Join", fontFamily = GameBoxFontFamily, fontSize = 18.sp , color = Color(0xFFC08CDC))
                        }
                        Spacer(Modifier.height(12.dp))
                        // Username
                        PixelOutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = "Your username",
                            restingLabelYOffset = 18.dp,
                            modifier = Modifier.fillMaxWidth(0.96f),
                            textColor = sharedTextColor,
                            bgColor = textFieldBgColor,
                            borderColor = sharedTextColor,
                            fontFamily = GameBoxFontFamily
                        )
                        Spacer(Modifier.height(10.dp))
                        if (mode == Mode.HOST) {
                            PixelOutlinedTextField(
                                value = roomName,
                                onValueChange = { roomName = it },
                                label = "Room name (required)",
                                modifier = Modifier.fillMaxWidth(0.96f),
                                textColor = sharedTextColor,
                                bgColor = textFieldBgColor,
                                borderColor = sharedTextColor,
                                fontFamily = GameBoxFontFamily
                            )
                        } else {
                            PixelOutlinedTextField(
                                value = roomCode,
                                onValueChange = { roomCode = it },
                                label = "Room code",
                                modifier = Modifier.fillMaxWidth(0.96f),
                                textColor = sharedTextColor,
                                bgColor = textFieldBgColor,
                                borderColor = sharedTextColor,
                                fontFamily = GameBoxFontFamily
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        PixelOutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = "Password (optional)",
                            modifier = Modifier.fillMaxWidth(0.96f),
                            textColor = sharedTextColor,
                            bgColor = textFieldBgColor,
                            borderColor = sharedTextColor,
                            fontFamily = GameBoxFontFamily
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
                                                gameId = gameId,
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
                        Text("Available rooms:", fontFamily = GameBoxFontFamily, fontSize = 20.sp, color = sharedTextColor)
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
                                    color = sharedTextColor,
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
    color: String? = null
) {
    val borderRes = com.example.gamehub.R.drawable.border_game_icon
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
