package com.example.gamehub.features.battleships.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.gamehub.features.battleships.model.Cell
import com.example.gamehub.features.battleships.model.MapRepository
import com.example.gamehub.features.battleships.ui.Ship as UiShip
import com.example.gamehub.lobby.FirestoreSession
import com.example.gamehub.lobby.model.GameSession
import com.example.gamehub.lobby.model.Move
import com.example.gamehub.lobby.model.PowerUp as DomainPowerUp
import com.example.gamehub.lobby.codec.BattleshipsCodec
import com.example.gamehub.navigation.NavRoutes
import com.example.gamehub.repository.interfaces.IBattleShipsRepository // <-- Repository Import
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.content.Context
import android.util.Log // Used for debugging, though not strictly required for the UI logic

// --- UTILITY FUNCTIONS ---

private fun UiShip.coveredCells(): List<Cell> = if (orientation == Orientation.Horizontal) {
    (0 until size).map { offset -> Cell(startRow, startCol + offset) }
} else {
    (0 until size).map { offset -> Cell(startRow + offset, startCol) }
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

@Composable
fun ShipsStatusPanel(
    opponentShips: List<UiShip>,
    yourMoves: List<Move>,
    modifier: Modifier = Modifier
) {
    val movesHitCells = yourMoves.map { Cell(it.y, it.x) }.toSet()
    val destroyed = opponentShips.filter { ship ->
        ship.coveredCells().all { cell -> cell in movesHitCells }
    }
    val alive = opponentShips - destroyed

    Box(
        modifier = modifier
            .background(Color(0xCC222222), shape = MaterialTheme.shapes.medium)
            .padding(12.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text("Opponent's Ships", style = MaterialTheme.typography.titleMedium, color = Color.White)
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
}

fun areAllShipsSunk(ships: List<UiShip>, moves: List<Move>): Boolean {
    return ships.all { ship ->
        ship.coveredCells().all { cell ->
            moves.any { it.x == cell.col && it.y == cell.row }
        }
    }
}

enum class AttackResult { Hit, Miss }

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

fun vibrateDevice(context: Context, duration: Long = 500) {
    // FIX for deprecated VIBRATOR_SERVICE
    @Suppress("DEPRECATION")
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    if (vibrator.hasVibrator()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(duration, 255)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }
}

// --- MAIN SCREEN COMPOSABLE ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BattleshipsPlayScreen(
    navController: NavHostController,
    roomCode: String,
    userName: String,
    battleshipsRepository: IBattleShipsRepository // <-- Injected Dependency
) {
    val context = LocalContext.current
    val uid = Firebase.auth.uid ?: return
    val scope = rememberCoroutineScope()

    // NOTE: Removed: val db = Firebase.firestore
    // NOTE: Removed: val roomRef = remember { db.collection("rooms").document(roomCode) }

    // 1. Core Game State Observation (Still using FirestoreSession for complex state)
    val session = remember { FirestoreSession(roomCode, BattleshipsCodec) }
    val state by session.stateFlow.collectAsState(initial = GameSession.empty(roomCode))

    // 2. Repository-driven State Setup and Observation
    DisposableEffect(roomCode) {
        battleshipsRepository.joinRoom(roomCode) // Starts the internal listener
        onDispose { /* battleshipsRepository.leaveRoom() // Optional cleanup */ }
    }

    // Collect the state flows provided by the Repository
    val gameRoom by battleshipsRepository.gameRoom.collectAsState()
    val rematchVotes by battleshipsRepository.rematchVotes.collectAsState()

    // Use values from the collected flows to determine UI state
    val lobbyStatus = gameRoom?.status
    val surrenderedUserId = gameRoom?.gameState?.gameResult?.takeIf { it != "" }

    val isMyTurn = state.currentTurn == uid

    // ... (All other local state setup remains the same, relying on 'state') ...
    val chosenMapId = state.chosenMap ?: 0
    val mapDef = remember(chosenMapId) { MapRepository.allMaps.first { it.id == chosenMapId } }
    val mapCells = mapDef.validCells

    var placingMine by remember { mutableStateOf(false) }
    var selectedPowerUp by remember { mutableStateOf<PowerUp?>(null) }
    var laserOrientation by remember { mutableStateOf(Orientation.Horizontal) }

    val opponentId = listOf(state.player1Id, state.player2Id)
        .filterNotNull()
        .firstOrNull { it != uid } ?: ""

    val myShips: List<UiShip> = (state.ships[uid] ?: emptyList()).map { ds -> UiShip(ds.startRow, ds.startCol, ds.size, ds.orientation) }
    val oppShips: List<UiShip> = (state.ships[opponentId] ?: emptyList()).map { ds -> UiShip(ds.startRow, ds.startCol, ds.size, ds.orientation) }

    val myDestroyedShips = myShips.filter { ship -> ship.coveredCells().all { cell -> state.moves.filter { it.playerId == opponentId }.any { it.x == cell.col && it.y == cell.row } } }
    val oppDestroyedShips = oppShips.filter { ship -> ship.coveredCells().all { cell -> state.moves.filter { it.playerId == uid }.any { it.x == cell.col && it.y == cell.row } } }

    var showSurrenderDialog by remember { mutableStateOf(false) }

    var animatingMove by remember { mutableStateOf<Cell?>(null) }
    var animIsHit by remember { mutableStateOf<Boolean?>(null) }

    val havePlacedShips = myShips.isNotEmpty() && oppShips.isNotEmpty()
    val myMoves = state.moves.filter { it.playerId == uid }
    val oppMoves = state.moves.filter { it.playerId == opponentId }

    val lastOppMove = oppMoves.lastOrNull()
    val iSunkOpponent = havePlacedShips && areAllShipsSunk(oppShips, myMoves)
    val opponentSunkMe = havePlacedShips && areAllShipsSunk(myShips, oppMoves)

    val wasHit = remember(lastOppMove) { lastOppMove?.let { move -> myShips.any { it.covers(move.y, move.x) } } ?: false }

    LaunchedEffect(lastOppMove) {
        if (wasHit && lastOppMove != null) { vibrateDevice(context) }
    }

    val gameResult = state.gameResult
    val isWinner = gameResult == uid
    val isBackendGameOver = gameResult != null && gameResult != ""
    val isSurrenderedGameOver = surrenderedUserId != null
    val isLocallyGameOver = (iSunkOpponent || opponentSunkMe || isBackendGameOver || isSurrenderedGameOver) && havePlacedShips

    val myMines = state.placedMines[uid] ?: emptyList()
    val enemyMines = state.placedMines[opponentId] ?: emptyList()
    val myTriggeredMines = state.triggeredMines[uid] ?: emptyList()
    val enemyTriggeredMines = state.triggeredMines[opponentId] ?: emptyList()

    var minesAtTurnStart by remember(state.currentTurn) { mutableStateOf(myMines.size) }
    val hasPlacedMineThisTurn = myMines.size > minesAtTurnStart

    // Helper function is cleaner now
    // Note: The energy update is done directly within the PowerUp logic below.

    // --- 3. Launched Effects (Now relies on repo-driven flows) ---

    LaunchedEffect(lobbyStatus) {
        if (lobbyStatus == "ended") {
            navController.navigate(NavRoutes.MAIN_MENU) {
                popUpTo(0)
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(rematchVotes) {
        if (rematchVotes.size == 2 && rematchVotes.values.all { it }) {
            scope.launch {
                // REPLACEMENT: Use repository's reset method
                battleshipsRepository.resetGame(roomCode)

                delay(250)
                navController.navigate(
                    NavRoutes.BATTLE_VOTE
                        .replace("{code}", roomCode)
                        .replace("{userName}", java.net.URLEncoder.encode(userName, "UTF-8"))
                ) {
                    popUpTo(NavRoutes.LOBBY_MENU.replace("{gameId}", "battleships")) { inclusive = true }
                }
            }
        }
    }

    // Animation state
    val attackAnim = state.currentAttack
    val isAttackOnMe = attackAnim?.playerId != uid

    LaunchedEffect(attackAnim?.x, attackAnim?.y, attackAnim?.playerId, attackAnim?.startedAt) {
        if (attackAnim != null && animatingMove == null) {
            animatingMove = Cell(attackAnim.y, attackAnim.x)
            animIsHit = if (isAttackOnMe) {
                myShips.any { it.covers(attackAnim.y, attackAnim.x) }
            } else {
                oppShips.any { it.covers(attackAnim.y, attackAnim.x) }
            }
        }
    }

    // --- UI Structure ---
    Box(modifier = Modifier.fillMaxSize()) {
        Image(painter = painterResource(com.example.gamehub.R.drawable.bg_battleships), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("🐋 Battleships", color = Color.White) },
                    actions = {
                        IconButton(onClick = { showSurrenderDialog = true }) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Surrender", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black, titleContentColor = Color.White, actionIconContentColor = Color.White, navigationIconContentColor = Color.White)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            val scrollState = rememberScrollState()
            Column(
                Modifier
                    .verticalScroll(scrollState)
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Current turn: ${if (isMyTurn) "You" else "Opponent"}",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Spacer(Modifier.height(16.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    // --- Board with correct shape, attack animation logic ---
                    Box(
                        modifier = Modifier.size(32.dp * 10)
                    ) {

                        val boardShips = if (placingMine || !isMyTurn) myShips else emptyList()
                        val boardAttacks = if (placingMine || !isMyTurn) buildAttackMap(myShips, oppMoves) else buildAttackMap(oppShips, myMoves)
                        val boardMines = if (placingMine || !isMyTurn) myMines else emptyList()
                        val boardTriggeredMines = if (placingMine || !isMyTurn) myTriggeredMines else enemyTriggeredMines
                        val boardDestroyedShips = if (placingMine || !isMyTurn) myDestroyedShips else oppDestroyedShips

                        BattleshipMap(
                            gridSize = 10,
                            cellSize = 32.dp,
                            ships = boardShips,
                            destroyedShips = boardDestroyedShips,
                            mineCells = boardMines,
                            triggeredMines = boardTriggeredMines,
                            attacks = boardAttacks,
                            validCells = mapCells,
                            onCellClick = { row, col ->
                                if (!isMyTurn || isLocallyGameOver) return@BattleshipMap

                                when (selectedPowerUp) {
                                    PowerUp.Mine -> {
                                        val isMineHere = myMines.any { it.row == row && it.col == col }
                                        val alreadyHit = oppMoves.any { it.x == col && it.y == row }
                                        if (!isMineHere && !alreadyHit && (state.energy[uid] ?: 0) >= PowerUp.Mine.cost) {
                                            scope.launch {
                                                try {
                                                    // REPLACEMENT: Use repository methods for both update and energy change
                                                    battleshipsRepository.updateEnergy(uid, -PowerUp.Mine.cost)
                                                    battleshipsRepository.placeMine(uid, Cell(row, col))
                                                    placingMine = false
                                                    selectedPowerUp = null
                                                } catch (e: Exception) {
                                                    Log.e("PlayScreen", "Mine error: ${e.message}")
                                                }
                                            }
                                        }
                                    }
                                    PowerUp.Bomb2x2 -> {
                                        if (row in 0 until 9 && col in 0 until 9 && (state.energy[uid] ?: 0) >= PowerUp.Bomb2x2.cost) {
                                            scope.launch {
                                                battleshipsRepository.updateEnergy(uid, -PowerUp.Bomb2x2.cost)
                                                val targets = PowerUp.Bomb2x2.expand(Cell(row, col))
                                                // session.submitMove is fine if it wraps the repo's submitMove
                                                targets.forEach { targetCell -> session.submitMove(targetCell.col, targetCell.row, uid) }
                                                selectedPowerUp = null
                                            }
                                        }
                                    }
                                    PowerUp.Laser -> {
                                        if ((state.energy[uid] ?: 0) >= PowerUp.Laser.cost) {
                                            scope.launch {
                                                battleshipsRepository.updateEnergy(uid, -PowerUp.Laser.cost)
                                                val targets = if (laserOrientation == Orientation.Horizontal) { (0 until 10).map { c -> Cell(row, c) } } else { (0 until 10).map { r -> Cell(r, col) } }
                                                targets.forEach { targetCell -> session.submitMove(targetCell.col, targetCell.row, uid) }
                                                selectedPowerUp = null
                                            }
                                        }
                                    }
                                    else -> {
                                        val cell = Cell(row, col)
                                        val myHitCells = myMoves.map { Cell(it.y, it.x) }.toSet()
                                        if (cell !in myHitCells && animatingMove == null) {
                                            val startedAt = System.currentTimeMillis()
                                            scope.launch {
                                                // REPLACEMENT: Use repository method
                                                battleshipsRepository.updateCurrentAttack(col, row, uid, startedAt)
                                            }
                                        }
                                    }
                                }
                            }
                        )

                        // --- Cannon Attack Animation ---
                        if (animatingMove != null) {
                            CannonAttackAnimation(
                                boardOffset = androidx.compose.ui.geometry.Offset.Zero,
                                cell = animatingMove!!,
                                cellSizePx = with(androidx.compose.ui.platform.LocalDensity.current) { 32.dp.toPx() },
                                isHit = animIsHit,
                                onFinished = {
                                    scope.launch {
                                        val currentAttack = attackAnim
                                        if (currentAttack != null) {
                                            session.submitMove(
                                                currentAttack.x,
                                                currentAttack.y,
                                                currentAttack.playerId
                                            )
                                            // REPLACEMENT: Use repository method
                                            battleshipsRepository.clearCurrentAttack()
                                        }
                                    }
                                    animatingMove = null
                                    animIsHit = null
                                },
                                vibrateOnHit = { vibrateDevice(context, 500) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                // --- PowerUp Cancel Button ---
                if (selectedPowerUp != null || placingMine) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Button(
                                onClick = {
                                    selectedPowerUp = null
                                    placingMine = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Black,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .width(200.dp)
                                    .height(48.dp)
                            ) {
                                Text("Cancel PowerUp")
                            }
                            if (selectedPowerUp == PowerUp.Laser) {
                                Spacer(Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        laserOrientation =
                                            if (laserOrientation == Orientation.Horizontal)
                                                Orientation.Vertical
                                            else
                                                Orientation.Horizontal
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Black,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .width(200.dp)
                                        .height(48.dp)
                                ) {
                                    Text("Laser: ${if (laserOrientation == Orientation.Horizontal) "Row" else "Column"}")
                                }
                            }
                        }
                    }
                }

                ShipsStatusPanel(
                    opponentShips = oppShips,
                    yourMoves = myMoves,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

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
                    Box(
                        modifier = Modifier
                            .background(Color(0xCC222222), shape = MaterialTheme.shapes.medium)
                            .padding(12.dp)
                            .fillMaxWidth()
                    ) {
                        PowerUpPanel(energy, hasPlacedMineThisTurn) { pu ->
                            when (pu) {
                                PowerUp.Mine -> {
                                    placingMine = true
                                    selectedPowerUp = pu
                                }
                                else -> {
                                    selectedPowerUp = pu
                                }
                            }
                        }
                    }
                }
            }

            // --- Dialogs (Final Replacements) ---

            if (showSurrenderDialog) {
                AlertDialog(
                    onDismissRequest = { showSurrenderDialog = false },
                    title = { Text("Surrender?", color = Color.White) },
                    text = { Text("Are you sure you want to surrender? You will lose the game.", color = Color.White) },
                    confirmButton = {
                        Button(
                            onClick = {
                                showSurrenderDialog = false
                                scope.launch {
                                    // REPLACEMENT: Use repository method
                                    battleshipsRepository.surrender(uid)
                                }
                            }
                        ) { Text("Surrender") }
                    },
                    dismissButton = {
                        Button(onClick = { showSurrenderDialog = false }) { Text("Cancel") }
                    },
                    containerColor = Color(0xFF222222)
                )
            }

            if (isLocallyGameOver) {
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text("Game Over", color = Color.White) },
                    text = {
                        when {
                            isSurrenderedGameOver && surrenderedUserId == uid -> Text("You surrendered!", color = Color.White)
                            isSurrenderedGameOver && surrenderedUserId == opponentId -> Text("Opponent surrendered!", color = Color.White)
                            isWinner || (iSunkOpponent && !opponentSunkMe) -> Text("You have won!", color = Color.White)
                            opponentSunkMe && !iSunkOpponent -> Text("Your opponent has won!", color = Color.White)
                            else -> Text("Game Over!", color = Color.White)
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                scope.launch {
                                    // REPLACEMENT: Use repository method
                                    battleshipsRepository.voteForRematch(uid)
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
                                scope.launch {
                                    // REPLACEMENT: Use repository method
                                    battleshipsRepository.endGame(roomCode)
                                }
                                navController.navigate(NavRoutes.MAIN_MENU) {
                                    popUpTo(0)
                                    launchSingleTop = true
                                }
                            }
                        ) {
                            Text("Exit Game")
                        }
                    },
                    containerColor = Color(0xFF222222)
                )
            }
        }
    }
}