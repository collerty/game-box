package com.example.gamehub.features.battleships.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gamehub.R
import com.example.gamehub.lobby.FirestoreSession
import com.example.gamehub.lobby.LobbyService
import com.example.gamehub.lobby.codec.BattleshipsCodec
import com.example.gamehub.lobby.codec.BattleshipsMove
import com.example.gamehub.lobby.codec.BattleshipsState
import com.example.gamehub.navigation.NavRoutes
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BattleshipsPlayScreen(
    navController: NavController,
    code: String,
    userName: String
) {
    val uid = Firebase.auth.uid ?: return
    val db  = Firebase.firestore
    val room = remember { db.collection("rooms").document(code) }
    val session = remember { FirestoreSession(code, BattleshipsCodec) }
    val scope = rememberCoroutineScope()

    // --- Shared state ---
    var gameState by remember { mutableStateOf<BattleshipsState?>(null) }
    var placements by remember { mutableStateOf<Map<String, List<Ship>>>(emptyMap()) }
    var players by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var showSettings by remember { mutableStateOf(false) }

    // 1) Listen to game state (moves & turn)
    LaunchedEffect(code) {
        session.stateFlow.collect { state ->
            gameState = state
        }
    }

    // 2) Listen to Firestore players & placements
    LaunchedEffect(code) {
        room.addSnapshotListener { snap, _ ->
            if (snap == null) return@addSnapshotListener
            // players list
            val list = snap.get("players") as? List<Map<String,Any>> ?: emptyList()
            players = list.associate { it["uid"] as String to it["name"] as String }
            // placements map
            val raw = snap.get("gameState.battleships.placements") as? Map<*,*> ?: emptyMap<Any,Any>()
            placements = raw.mapNotNull { (k,v) ->
                val name = k as? String ?: return@mapNotNull null
                val arr  = v as? List<*> ?: return@mapNotNull null
                val ships = arr.mapNotNull { item ->
                    (item as? Map<*,*>)?.let { m ->
                        Ship(
                            (m["startRow"] as Long).toInt(),
                            (m["startCol"] as Long).toInt(),
                            (m["size"]    as Long).toInt(),
                            Orientation.valueOf(m["orientation"] as String)
                        )
                    }
                }
                name to ships
            }.toMap()
        }
    }

    // Identify opponent
    val opponentName = players.keys.firstOrNull { it != userName } ?: ""

    // 3) Derive attack lists (simplified alternation logic)
    val moves = gameState?.moves.orEmpty()
    val myMoves  = moves.filterIndexed { i, _ -> true /* replace with real logic */ }
    val oppMoves = moves - myMoves.toSet()
    val myAttacks  = myMoves.map { parsePos(it) }
    val oppAttacks = oppMoves.map { parsePos(it) }

    // 4) Energy & Power‐ups UI state
    var energy by remember { mutableStateOf(5) }
    var nextPower by remember { mutableStateOf<PowerUp?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🐋 Battleships") },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            // Turn indicator
            Text(
                "Current turn: ${players[gameState?.currentTurn] ?: "..."}",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(16.dp))

            // Opponent's board + PowerUps
            Row {
                BoardGrid(
                    gridSize = 10,
                    cellSize = 32.dp,
                    ships    = emptyList(),
                    attacks  = myAttacks.associateWith { cell ->
                        val hit = placements[opponentName]?.any { it.covers(cell) } == true
                        if (hit) AttackResult.Hit else AttackResult.Miss
                    },
                    enabled = (gameState?.currentTurn == uid),
                    onCellClick = { cell ->
                        val cost = nextPower?.cost ?: 1
                        if (energy >= cost) {
                            energy -= cost
                            val targets = nextPower?.expand(cell) ?: listOf(cell)
                            targets.forEach { targ ->
                                scope.launch {
                                    session.sendMove(BattleshipsMove("${targ.row},${targ.col}", uid))
                                }
                            }
                            nextPower = null
                        }
                    }
                )
                Spacer(Modifier.width(16.dp))
                PowerUpPanel(energy = energy, onSelect = { pu ->
                    if (energy >= pu.cost) nextPower = pu
                })
            }

            Spacer(Modifier.height(24.dp))

            // Your board
            Text("Your board", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            BoardGrid(
                gridSize = 10,
                cellSize = 32.dp,
                ships    = placements[userName].orEmpty(),
                attacks  = oppAttacks.associateWith { cell ->
                    val hit = placements[userName]?.any { it.covers(cell) } == true
                    if (hit) AttackResult.Hit else AttackResult.Miss
                },
                enabled = false,
                onCellClick = null
            )
        }

        // --- Settings / Surrender dialog ---
        if (showSettings) {
            AlertDialog(
                onDismissRequest = { showSettings = false },
                title = { Text("Game Settings") },
                confirmButton = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            scope.launch {
                                LobbyService.battleshipsSurrender(code, "battleships", uid)
                            }
                            showSettings = false
                        }) { Text("Surrender") }

                        Button(onClick = {
                            scope.launch {
                                LobbyService.deleteRoom(code)
                                navController.popBackStack()
                            }
                        }) { Text("Leave Game") }

                        OutlinedButton(onClick = { showSettings = false }) {
                            Text("Cancel")
                        }
                    }
                }
            )
        }
    }
}

// --- BoardGrid composable ---
@Composable
fun BoardGrid(
    gridSize: Int,
    cellSize: Dp,
    ships: List<Ship>,
    attacks: Map<Cell, AttackResult>,
    enabled: Boolean,
    onCellClick: ((Cell) -> Unit)?
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(gridSize),
        modifier = Modifier.size(cellSize * gridSize)
    ) {
        items((0 until gridSize).flatMap { r -> (0 until gridSize).map { c -> Cell(r, c) } }) { cell ->
            Box(
                Modifier
                    .size(cellSize)
                    .background(Color(0xFF1A1A1A))
                    .clickable(enabled) { onCellClick?.invoke(cell) },
                contentAlignment = Alignment.Center
            ) {
                // draw ship if present
                if (ships.any { it.covers(cell) }) {
                    Box(Modifier
                        .fillMaxSize()
                        .background(Color(0xFF4B8B1D))
                    )
                }
                // overlay attack marker
                attacks[cell]?.let { result ->
                    val img = if (result == AttackResult.Hit)
                        painterResource(R.drawable.hit_circle)
                    else painterResource(R.drawable.miss_circle)
                    Image(img, contentDescription = null, modifier = Modifier.size(cellSize / 2))
                }
            }
        }
    }
}

enum class AttackResult { Hit, Miss }

data class Cell(val row: Int, val col: Int)

fun Ship.covers(cell: Cell): Boolean =
    if (orientation == Orientation.Horizontal)
        cell.row == startRow && cell.col in startCol until (startCol + size)
    else
        cell.col == startCol && cell.row in startRow until (startRow + size)

fun parsePos(pos: String): Cell {
    val (r, c) = pos.split(",").map { it.toInt() }
    return Cell(r, c)
}

// --- PowerUp definitions ---
sealed class PowerUp(val cost: Int) {
    abstract fun expand(origin: Cell): List<Cell>

    object Mine : PowerUp(2) {
        override fun expand(origin: Cell): List<Cell> = listOf(origin)
    }

    object Bomb2x2 : PowerUp(3) {
        override fun expand(origin: Cell): List<Cell> =
            listOf(
                origin,
                Cell(origin.row, origin.col + 1),
                Cell(origin.row + 1, origin.col),
                Cell(origin.row + 1, origin.col + 1)
            )
    }

    object Laser : PowerUp(2) {
        override fun expand(origin: Cell): List<Cell> {
            // default to entire row
            return (0 until 10).map { Cell(origin.row, it) }
        }
    }
}

@Composable
fun PowerUpPanel(energy: Int, onSelect: (PowerUp) -> Unit) {
    Column {
        Text("Energy: $energy", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        listOf(PowerUp.Mine, PowerUp.Bomb2x2, PowerUp.Laser).forEach { pu ->
            Button(
                onClick = { onSelect(pu) },
                enabled = energy >= pu.cost,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    when (pu) {
                        PowerUp.Mine    -> "Mine (2ℇ)"
                        PowerUp.Bomb2x2 -> "Bomb 2×2 (3ℇ)"
                        PowerUp.Laser   -> "Laser (2ℇ)"
                    }
                )
            }
        }
    }
}
