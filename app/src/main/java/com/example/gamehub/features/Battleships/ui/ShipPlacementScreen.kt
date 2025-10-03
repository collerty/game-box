package com.example.gamehub.features.battleships.ui

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.gamehub.navigation.NavRoutes
// REMOVED: import com.google.firebase.firestore.ktx.firestore
// REMOVED: import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay // Used delay explicitly
// REMOVED: import kotlinx.coroutines.tasks.await
import com.example.gamehub.features.battleships.ui.Orientation
import com.example.gamehub.features.battleships.ui.Ship
import com.google.firebase.auth.ktx.auth
import com.example.gamehub.features.battleships.model.MapRepository
import com.example.gamehub.features.battleships.model.Cell
// NEW: Repository Imports
import com.example.gamehub.repository.interfaces.IBattleShipsRepository
import com.google.firebase.ktx.Firebase
import com.example.gamehub.features.battleships.model.Ship as DomainShip // Import domain model Ship

@Composable
fun ShipPlacementScreen(
    navController: NavHostController,
    code: String,
    userName: String,
    mapId: Int,
    battleshipsRepository: IBattleShipsRepository // 1. Inject the Repository
) {
    // REMOVED: val db = Firebase.firestore
    // REMOVED: val roomRef = remember { db.collection("rooms").document(code) }

    val scope = rememberCoroutineScope()
    val shipSizes = listOf(5, 4, 3, 3, 2)
    val uid = Firebase.auth.currentUser?.uid ?: return // Return if no UID

    // --- State Read from Repository Flow ---

    // 2a. Start the listener/Join the room
    DisposableEffect(code) {
        battleshipsRepository.joinRoom(code)
        onDispose { /* Optional: battleshipsRepository.leaveRoom() */ }
    }

    // 2b. Collect the game room state
    val gameRoom by battleshipsRepository.gameRoom.collectAsState()

    // 2c. Derive necessary state from the gameRoom
    val battleshipsState = gameRoom?.gameState

    // Safety map: If `ready` isn't in model/parsed, default to emptyMap
    val readyMap: Map<String, Boolean> = battleshipsState?.ready ?: emptyMap()

    // Determine total players from gameRoom, defaulting to 2
    val players = listOfNotNull(gameRoom?.player1Id, gameRoom?.player2Id)
    val totalPlayers = players.size.coerceAtLeast(2)

    val readyCount = readyMap.values.count { it }
    val everyoneReady = readyCount == totalPlayers && totalPlayers > 1
    val iAmReady = readyMap[uid] == true

    // --- Local UI State (Unchanged) ---
    var placedShips by remember { mutableStateOf<List<Ship>>(emptyList()) }
    var orientation by remember { mutableStateOf(Orientation.Horizontal) }
    var pendingShips by remember { mutableStateOf<List<Ship>>(emptyList()) }

    // REMOVED: var readyMap by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    // REMOVED: var totalPlayers by remember { mutableStateOf(2) }
    // REMOVED: var everyoneReady by remember { mutableStateOf(false) }

    val mapDef = remember(mapId) { MapRepository.allMaps.first { it.id == mapId } }
    val mapCells = mapDef.validCells

    val activeShip: Ship? = pendingShips.firstOrNull() ?: run {
        val nextSize = shipSizes.getOrNull(placedShips.size)
        if (nextSize != null) Ship(0, 0, nextSize, orientation) else null
    }

    fun isValidPlacement(row: Int, col: Int): Boolean {
        val ship = activeShip ?: return false
        val positions = if (ship.orientation == Orientation.Horizontal) {
            (0 until ship.size).map { offset -> Cell(row, col + offset) }
        } else {
            (0 until ship.size).map { offset -> Cell(row + offset, col) }
        }
        return positions.all { cell ->
            cell in mapCells &&
                    cell.row in 0 until 10 && cell.col in 0 until 10 &&
                    placedShips.none { it.covers(cell.row, cell.col) }
        }
    }

    // REMOVED: The manual Firestore listener block (it is replaced by collectAsState)

    // --- Navigation Effect (Triggered by Repository Flow) ---
    LaunchedEffect(everyoneReady) {
        if (everyoneReady) {
            delay(300)
            val route = NavRoutes.BATTLESHIPS_GAME
                .replace("{code}", code)
                .replace("{userName}", Uri.encode(userName))
            navController.navigate(route) {
                popUpTo(0)
                launchSingleTop = true
            }
        }
    }

    // --- UI (Only the "Done" button is modified) ---
    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(com.example.gamehub.R.drawable.bg_battleships),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // ... (Title and Board rendering logic remains unchanged) ...

            // Top Title, centered, white
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Ship Placement Screen",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = true,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(4.dp))

            // Board centered, with black border
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                ) {
                    BattleshipMap(
                        gridSize = 10,
                        cellSize = 32.dp,
                        ships = placedShips,
                        highlightShip = { row, col -> isValidPlacement(row, col) },
                        onCellClick = { row, col ->
                            val removed = placedShips.firstOrNull { it.covers(row, col) }
                            if (removed != null) {
                                placedShips = placedShips - removed
                                pendingShips = pendingShips + removed
                                orientation = removed.orientation
                            } else {
                                val shipToPlace = activeShip
                                if (shipToPlace != null && isValidPlacement(row, col)) {
                                    placedShips = placedShips + shipToPlace.copy(
                                        startRow = row,
                                        startCol = col,
                                        orientation = shipToPlace.orientation
                                    )
                                    if (pendingShips.isNotEmpty()) {
                                        pendingShips = pendingShips.drop(1)
                                    }
                                }
                            }
                        },
                        validCells = mapCells
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            // ----------- Placing Ship / Buttons Box -----------
            if (!iAmReady && !everyoneReady) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xCC222222), shape = MaterialTheme.shapes.medium)
                        .padding(18.dp)
                ) {
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (activeShip != null) {
                            Text(
                                text = "Placing Ship:",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Spacer(Modifier.height(8.dp))
                            ShipPreview(activeShip)
                            Spacer(Modifier.height(24.dp))
                        }

                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    orientation = if (orientation == Orientation.Horizontal)
                                        Orientation.Vertical else Orientation.Horizontal
                                    if (pendingShips.isNotEmpty()) {
                                        pendingShips = listOf(
                                            pendingShips.first().copy(orientation = orientation)
                                        ) + pendingShips.drop(1)
                                    }
                                },
                                enabled = (pendingShips.isEmpty() && placedShips.size < shipSizes.size && !iAmReady) ||
                                        (pendingShips.isNotEmpty() && !iAmReady),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF333333),
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Rotate")
                            }

                            Spacer(Modifier.width(16.dp))

                            Button(
                                onClick = {
                                    scope.launch {
                                        // 3. Write State via Repository
                                        val domainShips = placedShips.map { uiShip ->
                                            DomainShip(
                                                startRow = uiShip.startRow,
                                                startCol = uiShip.startCol,
                                                size = uiShip.size,
                                                orientation = uiShip.orientation.name // Map to String
                                            )
                                        }
                                        try {
                                            // 🛑 REPLACEMENT: Single call to the repository method
                                            battleshipsRepository.setPlayerReady(uid, domainShips)
                                        } catch (e: Exception) {
                                            println("DEBUG: [REPO-FAIL] Ship placement failed: ${e.message}")
                                        }
                                    }
                                },
                                enabled = placedShips.size == shipSizes.size && pendingShips.isEmpty() && !iAmReady && !everyoneReady,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF333333),
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Done $readyCount/$totalPlayers")
                            }
                        }
                    }
                }
            }
            // -------------------------------------------------------------------------------------------

            Spacer(Modifier.height(24.dp))

            if (iAmReady && !everyoneReady) {
                Text(
                    "Waiting for other player to finish...",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

// ... (ShipPreview and Ship.covers functions remain unchanged) ...

@Composable
fun ShipPreview(ship: Ship) {
    Row(
        Modifier.wrapContentWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (ship.orientation == Orientation.Horizontal) {
            Row {
                repeat(ship.size) {
                    Box(
                        Modifier
                            .size(24.dp)
                            .background(Color(0xFF4B8B1D))
                            .border(3.dp, Color.Black, RectangleShape)
                    )
                    if (it != ship.size - 1) Spacer(Modifier.width(2.dp))
                }
            }
        } else {
            Column {
                repeat(ship.size) {
                    Box(
                        Modifier
                            .size(24.dp)
                            .background(Color(0xFF4B8B1D))
                            .border(3.dp, Color.Black, RectangleShape)
                    )
                    if (it != ship.size - 1) Spacer(Modifier.height(2.dp))
                }
            }
        }
    }
}

// Util for ship coverage
fun Ship.covers(row: Int, col: Int): Boolean =
    if (orientation == Orientation.Horizontal) {
        row == startRow && col in startCol until (startCol + size)
    } else {
        col == startCol && row in startRow until (startRow + size)
    }