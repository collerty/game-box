package com.example.gamehub.features.battleships.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.navigation.NavHostController
import com.example.gamehub.features.battleships.model.Cell
import com.example.gamehub.features.battleships.ui.PowerUp
import com.example.gamehub.features.battleships.ui.PowerUpPanel
import com.example.gamehub.features.battleships.ui.Ship as UiShip
import com.example.gamehub.features.battleships.ui.Orientation
import com.example.gamehub.lobby.FirestoreSession
import com.example.gamehub.lobby.LobbyService
import com.example.gamehub.lobby.model.GameSession
import com.example.gamehub.lobby.model.Move
import com.example.gamehub.lobby.model.PowerUp as DomainPowerUp
import com.example.gamehub.lobby.codec.BattleshipsCodec
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

// ----------------------------------------------------------------
// Helpers for ships status (add at top, above @Composable fun BattleshipsPlayScreen)
// ----------------------------------------------------------------

// Returns all cells covered by this ship
private fun UiShip.coveredCells(): List<Cell> = if (orientation == Orientation.Horizontal) {
    (0 until size).map { offset -> Cell(startRow, startCol + offset) }
} else {
    (0 until size).map { offset -> Cell(startRow + offset, startCol) }
}

@Composable
fun ShipsStatusPanel(
    opponentShips: List<UiShip>,
    yourMoves: List<Move>
) {
    val movesHitCells = yourMoves.map { Cell(it.y, it.x) }.toSet()
    val destroyed = opponentShips.filter { ship ->
        ship.coveredCells().all { cell -> cell in movesHitCells }
    }
    val alive = opponentShips - destroyed

    Column(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Text("Opponent's Ships", style = MaterialTheme.typography.titleMedium)

        Text("Ships alive:", color = Color(0xFF4B8B1D))
        Column(Modifier.padding(bottom = 8.dp)) {
            alive.forEach { ship ->
                ShipBox(ship.size, destroyed = false)
                Spacer(Modifier.height(4.dp))
            }
            if (alive.isEmpty()) Text("None", color = Color.Gray)
        }

        Text("Ships destroyed:", color = Color.Red)
        Column {
            destroyed.forEach { ship ->
                ShipBox(ship.size, destroyed = true)
                Spacer(Modifier.height(4.dp))
            }
            if (destroyed.isEmpty()) Text("None", color = Color.Gray)
        }
    }
}


@Composable
fun ShipBox(size: Int, destroyed: Boolean) {
    Row {
        repeat(size) {
            Box(
                Modifier
                    .size(18.dp, 18.dp)
                    .background(if (destroyed) Color.Red else Color(0xFF4B8B1D), shape = MaterialTheme.shapes.small)
                    .border(1.dp, Color.Black, MaterialTheme.shapes.small)
            )
            if (it != size - 1) Spacer(Modifier.width(2.dp))
        }
    }
}

// Only THIS extension in the file! Only for UiShip.
//private fun UiShip.covers(row: Int, col: Int): Boolean =
//    if (orientation == Orientation.Horizontal)
//        row == startRow && col in startCol until (startCol + size)
//    else
//        col == startCol && row in startRow until (startRow + size)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BattleshipsPlayScreen(
    navController: NavHostController,
    roomCode: String,
    userName: String
) {
    val uid = Firebase.auth.uid ?: return
    var showDialog by remember { mutableStateOf(false) }
    val session = remember { FirestoreSession(roomCode, BattleshipsCodec) }
    val state by session.stateFlow.collectAsState(initial = GameSession.empty(roomCode))
    val isMyTurn = state.currentTurn == uid
    val scope = rememberCoroutineScope()

    // Get the opponent's UID (assume always 2 players)
    val opponentId = listOf(state.player1Id, state.player2Id)
        .filterNotNull()
        .firstOrNull { it != uid } ?: ""

    // Map domain ships to UI ships
    val myShips: List<UiShip> = (state.ships[uid] ?: emptyList()).map { ds ->
        UiShip(ds.startRow, ds.startCol, ds.size, ds.orientation)
    }
    val oppShips: List<UiShip> = (state.ships[opponentId] ?: emptyList()).map { ds ->
        UiShip(ds.startRow, ds.startCol, ds.size, ds.orientation)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🐋 Battleships") },
                actions = {
                    IconButton(onClick = { showDialog = true }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Surrender")
                    }
                    IconButton(onClick = { /* settings… */ }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        // Surrender dialog
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title   = { Text("Surrender Game?") },
                text    = { Text("This will end the match immediately.") },
                confirmButton = {
                    TextButton(onClick = {
                        showDialog = false
                        scope.launch {
                            LobbyService.battleshipsSurrender(
                                roomCode        = roomCode,
                                gameId          = "battleships",
                                surrenderingUid = uid
                            )
                        }
                    }) { Text("Yes, Surrender") }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) { Text("Cancel") }
                }
            )
        }

        // *** SCROLL FIX STARTS HERE ***
        val scrollState = rememberScrollState()
        Column(
            Modifier
                .verticalScroll(scrollState)
                .padding(padding)
                .padding(16.dp)
        ) {
            // Turn indicator
            Text(
                text = "Current turn: ${if (isMyTurn) "You" else "Opponent"}",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(16.dp))

            // Show exactly one board: attack (your turn) or defend (their turn)
            if (isMyTurn) {
                // Your turn: show opponent’s board, attacks are your moves (show hits/misses only)
                BoardGrid(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp * 10),
                    gridSize = 10,
                    cellSize = 32.dp,
                    ships    = emptyList(),
                    attacks  = buildAttackMap(oppShips, state.moves.filter { it.playerId == uid }),
                    enabled  = true
                ) { row, col ->
                    scope.launch {
                        session.submitMove(col, row, uid) // x = col, y = row!
                    }
                }

                // Show opponent's ships status (from YOUR perspective)
                // After BoardGrid (for both turns)
                ShipsStatusPanel(
                    opponentShips = oppShips,
                    yourMoves = state.moves.filter { it.playerId == uid }
                )

            } else {
                // Not your turn: show your own board with ships, attacks are their moves
                BoardGrid(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp * 10),
                    gridSize = 10,
                    cellSize = 32.dp,
                    ships    = myShips,
                    attacks  = buildAttackMap(myShips, state.moves.filter { it.playerId == opponentId }),
                    enabled  = false
                ) { _, _ -> }

                // Show YOUR ships status (from opponent's perspective)
                // After BoardGrid (for both turns)
                ShipsStatusPanel(
                    opponentShips = oppShips,
                    yourMoves = state.moves.filter { it.playerId == uid }
                )

            }

            Spacer(Modifier.height(16.dp))

            // Power-up panel: only on your turn!
            if (isMyTurn) {
                val modelPUs = state.availablePowerUps[uid] ?: emptyList()
                val uiPUs = modelPUs.mapNotNull { mp ->
                    when (mp) {
                        DomainPowerUp.MINE  -> PowerUp.Mine
                        DomainPowerUp.BOMB  -> PowerUp.Bomb2x2
                        DomainPowerUp.RADAR -> PowerUp.Laser
                        else                -> null
                    }
                }
                val energy = state.energy[uid] ?: 0
                PowerUpPanel(energy) { pu ->
                    // For now, just submit a move for each cell. (For full logic, implement a session.submitPowerUpMove)
                    pu.expand(Cell(0, 0)).forEach { cell ->
                        scope.launch {
                            session.submitMove(cell.col, cell.row, uid)
                        }
                    }
                }
            }
        }
        // *** SCROLL FIX ENDS HERE ***
    }
}

// ----------------------------------------------------------------
// Helpers
// ----------------------------------------------------------------

private fun buildAttackMap(
    ships: List<UiShip>,
    moves: List<Move>
): Map<Cell, AttackResult> =
    moves
        .map { mv -> Cell(mv.y, mv.x) }
        .distinct()
        .associateWith { cell ->
            if (ships.any { it.covers(cell.row, cell.col) })
                AttackResult.Hit
            else
                AttackResult.Miss
        }

// ----------------------------------------------------------------
// BoardGrid + supporting types
// ----------------------------------------------------------------
@Composable
fun BoardGrid(
    modifier: Modifier = Modifier,
    gridSize: Int,
    cellSize: Dp,
    ships: List<UiShip>,
    attacks: Map<Cell, AttackResult>,
    enabled: Boolean,
    onCellClick: (row: Int, col: Int) -> Unit
) {
    val frame = rememberWaterFrame(
        spriteRes    = com.example.gamehub.R.drawable.ocean_spritesheet,
        framesPerRow = 8,
        totalFrames  = 16,
        fps          = 8
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(gridSize),
        modifier = modifier.then(Modifier.size(cellSize * gridSize))
    ) {
        items((0 until gridSize).flatMap { r -> (0 until gridSize).map { c -> Cell(r, c) } }) { cell ->
            Box(
                Modifier
                    .size(cellSize)
                    .then(
                        if (enabled)
                            Modifier.clickable { onCellClick(cell.row, cell.col) }
                        else
                            Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                AnimatedWaterCell(
                    frame        = frame,
                    spriteRes    = com.example.gamehub.R.drawable.ocean_spritesheet,
                    framesPerRow = 8,
                    totalFrames  = 16,
                    size         = cellSize
                )

                if (ships.any { it.covers(cell.row, cell.col) }) {
                    Box(
                        Modifier
                            .matchParentSize()
                            .background(Color(0xFF4B8B1D))
                    )
                }

                attacks[cell]?.let { res ->
                    @DrawableRes val img =
                        if (res == AttackResult.Hit)
                            com.example.gamehub.R.drawable.hit_circle
                        else
                            com.example.gamehub.R.drawable.miss_circle

                    Image(
                        painter            = painterResource(img),
                        contentDescription = null,
                        modifier           = Modifier.size(cellSize / 2)
                    )
                }
            }
        }
    }
}

enum class AttackResult { Hit, Miss }
