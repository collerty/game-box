package com.example.gamehub.features.battleships.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.gamehub.navigation.NavRoutes
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Full placement screen: writes placements to Firestore under
 * gameState.battleships.placements.<userName>, shows Done count,
 * enforces 10s auto‐complete, and waits for both players before navigating.
 */
@Composable
fun ShipPlacementScreen(
    navController: NavHostController,
    code: String,
    userName: String,
    gridSize: Int = 10,
    cellSize: Dp = 32.dp,
    shipSizes: List<Int> = listOf(5, 4, 3, 3, 2)
) {
    // Firestore setup
    val db = Firebase.firestore
    val roomDoc = remember { db.collection("rooms").document(code) }
    val scope = rememberCoroutineScope()

    // 1) Listen to players & placements map
    var playersCount by remember { mutableStateOf(0) }
    var placements by remember { mutableStateOf<Map<String, List<Ship>>>(emptyMap()) }
    LaunchedEffect(code) {
        roomDoc.addSnapshotListener { snap, _ ->
            playersCount = (snap?.get("players") as? List<*>)?.size ?: 0
            val raw = (snap?.get("gameState.battleships.placements") as? Map<*,*>) ?: emptyMap<Any,Any>()
            placements = raw.mapNotNull { (k,v) ->
                val name = k as? String ?: return@mapNotNull null
                val list = v as? List<*> ?: return@mapNotNull null
                val ships = list.mapNotNull { item ->
                    (item as? Map<*,*>)?.let { m ->
                        val r = (m["startRow"] as? Long)?.toInt()
                        val c = (m["startCol"] as? Long)?.toInt()
                        val s = (m["size"] as? Long)?.toInt()
                        val o = (m["orientation"] as? String)
                        if (r!=null && c!=null && s!=null && o!=null)
                            Ship(r, c, s, Orientation.valueOf(o))
                        else null
                    }
                }
                name to ships
            }.toMap()
        }
    }

    // 2) Local state
    var placedShips by remember { mutableStateOf(placements[userName] ?: emptyList()) }
    var currentIndex by remember { mutableStateOf(placedShips.size) }
    var orientation by remember { mutableStateOf(Orientation.Horizontal) }
    var hasDone by remember { mutableStateOf(placements.containsKey(userName)) }
    var timeLeft by remember { mutableStateOf(10) }

    // 3) 10-second timer → auto place & mark done
    LaunchedEffect(timeLeft, hasDone) {
        if (timeLeft > 0 && !hasDone) {
            delay(1_000L)
            timeLeft--
        } else if (timeLeft <= 0 && !hasDone) {
            // auto‐place remaining ships
            val rnd = Random.Default
            shipSizes.drop(currentIndex).forEach { size ->
                var ship: Ship
                do {
                    val r = rnd.nextInt(gridSize)
                    val c = rnd.nextInt(gridSize)
                    val o = if (rnd.nextBoolean()) Orientation.Horizontal else Orientation.Vertical
                    ship = Ship(r, c, size, o)
                } while (!isValidPlacement(ship, placedShips, gridSize))
                placedShips = placedShips + ship
                currentIndex++
            }
            // write placements and mark done
            finishPlacement(roomDoc, placedShips, scope, userName)
            hasDone = true
        }
    }

    // 4) Navigate when both done
    if (placements.size == 2 && hasDone) {
        navController.navigate(
            NavRoutes.BATTLESHIPS_GAME
                .replace("{code}", code)
                .replace("{userName}", userName)
        )
    }

    // 5) UI
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Done ${placements.size}/$playersCount", style = MaterialTheme.typography.titleMedium)
        Text("Time left: $timeLeft s")
        Spacer(Modifier.height(16.dp))

        BattleshipMap(
            gridSize = gridSize,
            cellSize = cellSize,
            ships    = placedShips,
            onCellClick = { r, c ->
                if (hasDone) return@BattleshipMap
                // pick-up
                val tapped = placedShips.firstOrNull { ship ->
                    if (ship.orientation == Orientation.Horizontal)
                        r == ship.startRow && c in ship.startCol until ship.startCol+ship.size
                    else
                        c == ship.startCol && r in ship.startRow until ship.startRow+ship.size
                }
                if (tapped != null) {
                    placedShips = placedShips - tapped
                    currentIndex = placedShips.size
                    return@BattleshipMap
                }
                // place next
                if (currentIndex >= shipSizes.size) return@BattleshipMap
                val size = shipSizes[currentIndex]
                val cand = Ship(r, c, size, orientation)
                if (isValidPlacement(cand, placedShips, gridSize)) {
                    placedShips = placedShips + cand
                    currentIndex++
                }
            },
            highlightShip = shipSizes.getOrNull(currentIndex)?.let { size ->
                { r, c -> isValidPlacement(Ship(r, c, size, orientation), placedShips, gridSize) }
            }
        )

        Spacer(Modifier.height(16.dp))

        Row {
            Button(
                onClick = {
                    orientation = if (orientation == Orientation.Horizontal)
                        Orientation.Vertical else Orientation.Horizontal
                },
                enabled = !hasDone
            ) { Text("Rotate") }
            Spacer(Modifier.width(16.dp))
            Button(
                onClick = {
                    finishPlacement(roomDoc, placedShips, scope, userName)
                    hasDone = true
                },
                enabled = !hasDone
            ) { Text("Done") }
        }
    }
}

// Helper to write placements
private fun finishPlacement(
    roomDoc: com.google.firebase.firestore.DocumentReference,
    ships: List<Ship>,
    scope: CoroutineScope,
    userName: String
) {
    val encoded = ships.map { ship ->
        mapOf(
            "startRow"    to ship.startRow,
            "startCol"    to ship.startCol,
            "size"        to ship.size,
            "orientation" to ship.orientation.name
        )
    }
    scope.launch {
        roomDoc.update("gameState.battleships.placements.$userName", encoded)
    }
}

// Bounds & overlap check
fun isValidPlacement(ship: Ship, placed: List<Ship>, gridSize: Int): Boolean {
    if (ship.orientation == Orientation.Horizontal) {
        if (ship.startRow !in 0 until gridSize ||
            ship.startCol < 0 ||
            ship.startCol + ship.size > gridSize) return false
    } else {
        if (ship.startCol !in 0 until gridSize ||
            ship.startRow < 0 ||
            ship.startRow + ship.size > gridSize) return false
    }
    val occupied = placed.flatMap { shipCells(it) }.toSet()
    return shipCells(ship).none { it in occupied }
}

fun shipCells(ship: Ship): Set<Pair<Int, Int>> =
    if (ship.orientation == Orientation.Horizontal) {
        (0 until ship.size).map { ship.startRow to (ship.startCol + it) }.toSet()
    } else {
        (0 until ship.size).map { (ship.startRow + it) to ship.startCol }.toSet()
    }

@Preview(showBackground = true)
@Composable
fun ShipPlacementScreenPreview() {
    ShipPlacementScreen(
        navController = androidx.navigation.compose.rememberNavController(),
        code          = "",
        userName      = ""
    )
}
