package com.example.gamehub.features.battleships.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.gamehub.navigation.NavRoutes
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.gamehub.features.battleships.ui.Orientation
import com.example.gamehub.features.battleships.ui.Ship
import com.google.firebase.auth.ktx.auth
import com.example.gamehub.features.battleships.model.MapRepository
import com.example.gamehub.features.battleships.model.Cell

@Composable
fun ShipPlacementScreen(
    navController: NavHostController,
    code: String,
    userName: String,
    mapId: Int
) {
    val db = Firebase.firestore
    val roomRef = remember { db.collection("rooms").document(code) }
    val scope = rememberCoroutineScope()
    val shipSizes = listOf(5, 4, 3, 3, 2)

    var placedShips by remember { mutableStateOf<List<Ship>>(emptyList()) }
    var orientation by remember { mutableStateOf(Orientation.Horizontal) }
    // Change: use a queue of pending ships, not a single ship
    var pendingShips by remember { mutableStateOf<List<Ship>>(emptyList()) }

    // READY STATE
    var readyMap by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var totalPlayers by remember { mutableStateOf(2) }
    var everyoneReady by remember { mutableStateOf(false) }
    val uid = Firebase.auth.currentUser?.uid ?: ""

    // Map info
    val mapDef = remember(mapId) { MapRepository.allMaps.first { it.id == mapId } }
    val mapCells = mapDef.validCells

    // Determine which ship is being placed: first picked up, else nextSize
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
        // Not overlapping and within allowed cells
        return positions.all { cell ->
            cell in mapCells &&
                    cell.row in 0 until 10 && cell.col in 0 until 10 &&
                    placedShips.none { it.covers(cell.row, cell.col) }
        }
    }

    DisposableEffect(code) {
        val listener = roomRef.addSnapshotListener { snap, _ ->
            val gameState = snap?.get("gameState") as? Map<*, *> ?: run { return@addSnapshotListener }
            val battleships = gameState["battleships"] as? Map<*, *> ?: run { return@addSnapshotListener }
            val readyAny = (battleships["ready"] as? Map<*, *>) ?: emptyMap<Any, Any>()
            val ready: Map<String, Boolean> = readyAny.mapKeys { it.key.toString() }.mapValues { it.value == true }
            val shipsMap = (battleships["ships"] as? Map<*, *>) ?: emptyMap<Any, Any>()
            val players = shipsMap.keys.map { it.toString() }
            val totalPlayersNow = maxOf(players.size, 2)
            val readyCountNow = ready.values.count { it }
            val everyoneReadyNow = (readyCountNow == totalPlayersNow && totalPlayersNow > 1)
            if (everyoneReadyNow) {
                scope.launch {
                    kotlinx.coroutines.delay(300)
                    val route = NavRoutes.BATTLESHIPS_GAME
                        .replace("{code}", code)
                        .replace("{userName}", Uri.encode(userName))
                    navController.navigate(route) {
                        popUpTo(0)
                        launchSingleTop = true
                    }
                }
            }
            readyMap = ready
            totalPlayers = totalPlayersNow
            everyoneReady = everyoneReadyNow
        }
        onDispose { listener.remove() }
    }

    val readyCount = readyMap.values.count { it }
    val iAmReady = readyMap[uid] == true

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Centered Title
        Text(
            text = "Ship Placement Screen",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1
        )

        // Center the board
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            BattleshipMap(
                gridSize = 10,
                cellSize = 32.dp,
                ships = placedShips,
                highlightShip = { row, col ->
                    isValidPlacement(row, col)
                },
                onCellClick = { row, col ->
                    val removed = placedShips.firstOrNull { it.covers(row, col) }
                    if (removed != null) {
                        placedShips = placedShips - removed
                        // Add the picked-up ship to the queue for re-placement
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
                            // Remove the first picked-up ship from the queue, if any
                            if (pendingShips.isNotEmpty()) {
                                pendingShips = pendingShips.drop(1)
                            }
                        }
                    }
                }
            )
        }

        Spacer(Modifier.height(18.dp))

        // Placing Ship label and ship
        if (activeShip != null) {
            Column(
                Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Placing Ship:",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.height(8.dp))
                ShipPreview(activeShip)
            }
            Spacer(Modifier.height(24.dp))
        }

        // Centered Row for Buttons
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = {
                    orientation = if (orientation == Orientation.Horizontal)
                        Orientation.Vertical else Orientation.Horizontal
                    // If a ship is waiting to be placed, rotate only the first in the queue
                    if (pendingShips.isNotEmpty()) {
                        pendingShips = listOf(
                            pendingShips.first().copy(orientation = orientation)
                        ) + pendingShips.drop(1)
                    }
                },
                enabled = (pendingShips.isEmpty() && placedShips.size < shipSizes.size && !iAmReady) ||
                        (pendingShips.isNotEmpty() && !iAmReady)
            ) {
                Text("Rotate")
            }

            Spacer(Modifier.width(16.dp))

            Button(
                onClick = {
                    scope.launch {
                        val payload = placedShips.map { ship ->
                            mapOf(
                                "startRow" to ship.startRow,
                                "startCol" to ship.startCol,
                                "size" to ship.size,
                                "orientation" to ship.orientation.name
                            )
                        }
                        try {
                            roomRef.update("gameState.battleships.ships.$uid", payload).await()
                            roomRef.update("gameState.battleships.ready.$uid", true).await()
                        } catch (e: Exception) {
                            println("DEBUG: [WRITE-FAIL] Firestore update failed: ${e.message}")
                        }
                    }
                },
                enabled = placedShips.size == shipSizes.size && pendingShips.isEmpty() && !iAmReady && !everyoneReady
            ) {
                Text("Done $readyCount/$totalPlayers")
            }
        }

        Spacer(Modifier.height(24.dp))

        if (iAmReady && !everyoneReady) {
            Text("Waiting for other player to finish...", color = MaterialTheme.colorScheme.secondary)
        }
    }
}

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
                            .border(2.dp, Color.Black, RectangleShape)
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
                            .border(2.dp, Color.Black, RectangleShape)
                    )
                    if (it != ship.size - 1) Spacer(Modifier.height(2.dp))
                }
            }
        }
    }
}

fun Ship.covers(row: Int, col: Int): Boolean =
    if (orientation == Orientation.Horizontal) {
        row == startRow && col in startCol until (startCol + size)
    } else {
        col == startCol && row in startRow until (startRow + size)
    }
