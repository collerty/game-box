package com.example.gamehub.features.battleships.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clipToBounds
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
import kotlinx.coroutines.delay
import com.example.gamehub.navigation.NavRoutes
import com.google.firebase.firestore.ktx.firestore
import kotlinx.coroutines.tasks.await

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

// --------------------- LOCAL GAME OVER FIX ------------------------
fun areAllShipsSunk(ships: List<UiShip>, moves: List<Move>): Boolean {
    return ships.all { ship ->
        ship.coveredCells().all { cell ->
            moves.any { it.x == cell.col && it.y == cell.row }
        }
    }
}
// -------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BattleshipsPlayScreen(
    navController: NavHostController,
    roomCode: String,
    userName: String
) {
    val uid = Firebase.auth.uid ?: return
    val scope = rememberCoroutineScope()
    val db = Firebase.firestore
    val roomRef = remember { db.collection("rooms").document(roomCode) }

    val session = remember { FirestoreSession(roomCode, BattleshipsCodec) }
    val state by session.stateFlow.collectAsState(initial = GameSession.empty(roomCode))
    val isMyTurn = state.currentTurn == uid

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

    // Compute destroyed ships for overlays
    val myDestroyedShips = myShips.filter { ship ->
        ship.coveredCells().all { cell ->
            state.moves.filter { it.playerId == opponentId }
                .any { it.x == cell.col && it.y == cell.row }
        }
    }
    val oppDestroyedShips = oppShips.filter { ship ->
        ship.coveredCells().all { cell ->
            state.moves.filter { it.playerId == uid }
                .any { it.x == cell.col && it.y == cell.row }
        }
    }

    // --- Win/Rematch/Exit/Surrender logic ---
    var rematchVotes by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var lobbyStatus by remember { mutableStateOf<String?>(null) }
    var surrenderedUserId by remember { mutableStateOf<String?>(null) }
    var showSurrenderDialog by remember { mutableStateOf(false) }

    // -------------- LOCAL GAME OVER LOGIC -----------------
    val havePlacedShips = myShips.isNotEmpty() && oppShips.isNotEmpty()
    val myMoves = state.moves.filter { it.playerId == uid }
    val oppMoves = state.moves.filter { it.playerId == opponentId }
    val iSunkOpponent = havePlacedShips && areAllShipsSunk(oppShips, myMoves)
    val opponentSunkMe = havePlacedShips && areAllShipsSunk(myShips, oppMoves)

    val gameResult = state.gameResult
    val isWinner = gameResult == uid
    val isBackendGameOver = gameResult != null && gameResult != ""
    val isSurrenderedGameOver = surrenderedUserId != null
    val isLocallyGameOver = (iSunkOpponent || opponentSunkMe || isBackendGameOver || isSurrenderedGameOver) && havePlacedShips
    // -------------------------------------------------------------------------

    // --- Unified Firestore listener ---
    DisposableEffect(roomCode) {
        val reg = roomRef.addSnapshotListener { snap, _ ->
            val bs = ((snap?.get("gameState") as? Map<*, *>)?.get("battleships") as? Map<*, *>) ?: return@addSnapshotListener
            val votes = (bs["rematchVotes"] as? Map<*, *>)?.mapKeys { it.key.toString() }?.mapValues { it.value == true } ?: emptyMap()
            rematchVotes = votes
            lobbyStatus = snap.get("status") as? String
            surrenderedUserId = bs["surrendered"] as? String
            println("UI DEBUG: SnapshotListener: rematchVotes=$rematchVotes, lobbyStatus=$lobbyStatus, surrenderedUserId=$surrenderedUserId")
        }
        onDispose { reg.remove() }
    }

    // If status is ended, pop to lobby for all players
    LaunchedEffect(lobbyStatus) {
        if (lobbyStatus == "ended") {
            println("UI DEBUG: Navigating to LOBBY_MENU due to status=ended")
            navController.navigate(
                NavRoutes.LOBBY_MENU.replace("{gameId}", "battleships")
            ) {
                popUpTo(0)
                launchSingleTop = true
            }
        }
    }

    // Handle rematch: if both have voted, reset and go to BATTLE_VOTE
    LaunchedEffect(rematchVotes) {
        if (rematchVotes.size == 2 && rematchVotes.values.all { it }) {
            println("UI DEBUG: Both players voted for rematch, resetting game.")
            scope.launch {
                roomRef.update(mapOf(
                    "gameState.battleships.moves" to emptyList<Any>(),
                    "gameState.battleships.availablePowerUps" to emptyMap<String,Any>(),
                    "gameState.battleships.energy" to emptyMap<String,Any>(),
                    "gameState.battleships.powerUpMoves" to emptyList<Any>(),
                    "gameState.battleships.mapVotes" to emptyMap<String,Any>(),
                    "gameState.battleships.chosenMap" to null,
                    "gameState.battleships.ready" to emptyMap<String,Any>(),
                    "gameState.battleships.ships" to emptyMap<String,Any>(),
                    "gameState.battleships.gameResult" to null,
                    "gameState.battleships.rematchVotes" to emptyMap<String,Any>(),
                    "gameState.battleships.surrendered" to null
                )).await()
                delay(250)
                navController.navigate(
                    NavRoutes.BATTLE_VOTE
                        .replace("{code}", roomCode)
                        .replace("{userName}", java.net.URLEncoder.encode(userName, "UTF-8"))
                ) {
                    popUpTo(0)
                    launchSingleTop = true
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🐋 Battleships") },
                actions = {
                    IconButton(onClick = {
                        println("UI DEBUG: Surrender button pressed")
                        showSurrenderDialog = true
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Surrender")
                    }
                    IconButton(onClick = { println("UI DEBUG: Settings button pressed") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
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

            // Center the grid perfectly
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                // ---- MAIN FIX HERE: Lock the board if game over (LOCALLY or backend) ----
                if (isMyTurn) {
                    // Show opponent’s board, attacks are your moves
                    BoardGrid(
                        gridSize = 10,
                        cellSize = 32.dp,
                        ships = emptyList(),
                        attacks = buildAttackMap(oppShips, myMoves),
                        enabled = !isLocallyGameOver,
                        destroyedShips = oppDestroyedShips,
                        onCellClick = { row, col ->
                            println("UI DEBUG: Player $uid attempting to attack ($row, $col)")
                            scope.launch {
                                session.submitMove(col, row, uid)
                            }
                        }
                    )
                } else {
                    // Show your own board with ships, attacks are their moves
                    BoardGrid(
                        gridSize = 10,
                        cellSize = 32.dp,
                        ships = myShips,
                        attacks = buildAttackMap(myShips, oppMoves),
                        enabled = false,
                        destroyedShips = myDestroyedShips,
                        onCellClick = { _, _ -> }
                    )
                }
            }

            // Ships status panel
            ShipsStatusPanel(
                opponentShips = oppShips,
                yourMoves = myMoves
            )

            Spacer(Modifier.height(16.dp))

            // Power-up panel: only on your turn AND NOT game over!
            if (isMyTurn && !isLocallyGameOver) {
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
                    println("UI DEBUG: Player $uid used powerup $pu")
                    pu.expand(Cell(0, 0)).forEach { cell ->
                        scope.launch {
                            session.submitMove(cell.col, cell.row, uid)
                        }
                    }
                }
            }
        }

        // --- SURRENDER DIALOG ---
        if (showSurrenderDialog) {
            AlertDialog(
                onDismissRequest = { showSurrenderDialog = false },
                title = { Text("Surrender?") },
                text = { Text("Are you sure you want to surrender? You will lose the game.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showSurrenderDialog = false
                            // Save surrender to Firestore for multiplayer
                            scope.launch {
                                roomRef.update("gameState.battleships.surrendered", uid).await()
                            }
                        }
                    ) { Text("Surrender") }
                },
                dismissButton = {
                    Button(onClick = { showSurrenderDialog = false }) { Text("Cancel") }
                }
            )
        }

        // --- GAME OVER DIALOG ---
        if (isLocallyGameOver) {
            println("UI DEBUG: Showing Win Dialog: gameResult=$gameResult, isWinner=$isWinner, rematchVotes=$rematchVotes, surrenderedUserId=$surrenderedUserId")
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Game Over") },
                text = {
                    when {
                        isSurrenderedGameOver && surrenderedUserId == uid -> Text("You surrendered!")
                        isSurrenderedGameOver && surrenderedUserId == opponentId -> Text("Opponent surrendered!")
                        isWinner || (iSunkOpponent && !opponentSunkMe) -> Text("You have won!")
                        opponentSunkMe && !iSunkOpponent -> Text("Your opponent has won!")
                        else -> Text("Game Over!")
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            println("UI DEBUG: Rematch button pressed by $uid")
                            scope.launch {
                                // Rematch vote
                                roomRef.update("gameState.battleships.rematchVotes.$uid", true).await()
                            }
                        },
                        enabled = !(rematchVotes[uid] == true)
                    ) {
                        Text("Rematch (${rematchVotes.size}/2)")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            println("UI DEBUG: Exit button pressed by $uid, setting status=ended")
                            scope.launch {
                                roomRef.update("status", "ended").await()
                            }
                            navController.navigate(
                                NavRoutes.LOBBY_MENU.replace("{gameId}", "battleships")
                            ) {
                                popUpTo(0)
                                launchSingleTop = true
                            }
                        }
                    ) {
                        Text("Exit Game")
                    }
                }
            )
        }
    }
}

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

@Composable
fun BoardGrid(
    gridSize: Int,
    cellSize: Dp,
    ships: List<UiShip>,
    attacks: Map<Cell, AttackResult>,
    enabled: Boolean,
    destroyedShips: List<UiShip> = emptyList(),
    onCellClick: (row: Int, col: Int) -> Unit
) {
    val frame = rememberWaterFrame(
        spriteRes    = com.example.gamehub.R.drawable.ocean_spritesheet,
        framesPerRow = 8,
        totalFrames  = 16,
        fps          = 8
    )

    Box(
        modifier = Modifier
            .size(cellSize * gridSize)
            .clipToBounds()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            for (row in 0 until gridSize) {
                Row {
                    for (col in 0 until gridSize) {
                        Box(
                            modifier = Modifier
                                .size(cellSize)
                                .then(
                                    if (enabled)
                                        Modifier.clickable { onCellClick(row, col) }
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
                            // Ships (no border here, just fill)
                            if (ships.any { it.covers(row, col) }) {
                                Box(
                                    Modifier
                                        .matchParentSize()
                                        .background(Color(0xFF4B8B1D))
                                )
                            }
                            attacks[Cell(row, col)]?.let { res ->
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
        }

        // --- Outline all ships with a black rectangle (unless destroyed, which will get red) ---
        ships.forEach { ship ->
            if (!destroyedShips.contains(ship)) {
                val top = cellSize * ship.startRow
                val left = cellSize * ship.startCol
                val width = if (ship.orientation == Orientation.Horizontal) cellSize * ship.size else cellSize
                val height = if (ship.orientation == Orientation.Horizontal) cellSize else cellSize * ship.size
                Box(
                    Modifier
                        .absoluteOffset(x = left, y = top)
                        .size(width, height)
                        .border(2.dp, Color.Black, RectangleShape)
                )
            }
        }

        // --- Outline destroyed ships with a thick red rectangle ---
        destroyedShips.forEach { ship ->
            val top = cellSize * ship.startRow
            val left = cellSize * ship.startCol
            val width = if (ship.orientation == Orientation.Horizontal) cellSize * ship.size else cellSize
            val height = if (ship.orientation == Orientation.Horizontal) cellSize else cellSize * ship.size
            Box(
                Modifier
                    .absoluteOffset(x = left, y = top)
                    .size(width, height)
                    .border(3.dp, Color.Red, RectangleShape)
            )
        }

        // Grid lines
        Box(modifier = Modifier.matchParentSize()) {
            for (i in 1 until gridSize) {
                // Vertical
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .absoluteOffset(x = cellSize * i, y = 0.dp)
                        .background(Color.Gray)
                )
                // Horizontal
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .absoluteOffset(x = 0.dp, y = cellSize * i)
                        .background(Color.Gray)
                )
            }
        }
    }
}

enum class AttackResult { Hit, Miss }
