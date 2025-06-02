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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.navigation.NavHostController
import com.example.gamehub.features.battleships.model.Cell
import com.example.gamehub.features.battleships.ui.Ship as UiShip
import com.example.gamehub.lobby.FirestoreSession
import com.example.gamehub.lobby.model.GameSession
import com.example.gamehub.lobby.model.Move
import com.example.gamehub.lobby.model.PowerUp as DomainPowerUp
import com.example.gamehub.lobby.codec.BattleshipsCodec
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.gamehub.navigation.NavRoutes
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import kotlinx.coroutines.tasks.await
import com.example.gamehub.features.battleships.ui.CannonAttackAnimation
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.runtime.Composable
import android.content.Context
import androidx.compose.ui.platform.LocalContext


private fun UiShip.coveredCells(): List<Cell> = if (orientation == Orientation.Horizontal) {
    (0 until size).map { offset -> Cell(startRow, startCol + offset) }
} else {
    (0 until size).map { offset -> Cell(startRow + offset, startCol) }
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
            .background(Color(0xCC222222), shape = MaterialTheme.shapes.medium) // semi-transparent grey
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

fun areAllShipsSunk(ships: List<UiShip>, moves: List<Move>): Boolean {
    return ships.all { ship ->
        ship.coveredCells().all { cell ->
            moves.any { it.x == cell.col && it.y == cell.row }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BattleshipsPlayScreen(
    navController: NavHostController,
    roomCode: String,
    userName: String
) {

    val context = LocalContext.current
    val uid = Firebase.auth.uid ?: return
    val scope = rememberCoroutineScope()
    val db = Firebase.firestore
    val roomRef = remember { db.collection("rooms").document(roomCode) }

    val session = remember { FirestoreSession(roomCode, BattleshipsCodec) }
    val state by session.stateFlow.collectAsState(initial = GameSession.empty(roomCode))
    val isMyTurn = state.currentTurn == uid

    var placingMine by remember { mutableStateOf(false) }
    var selectedPowerUp by remember { mutableStateOf<PowerUp?>(null) }
    // --- LASER ORIENTATION STATE ---
    var laserOrientation by remember { mutableStateOf(Orientation.Horizontal) }

    val opponentId = listOf(state.player1Id, state.player2Id)
        .filterNotNull()
        .firstOrNull { it != uid } ?: ""

    val myShips: List<UiShip> = (state.ships[uid] ?: emptyList()).map { ds ->
        UiShip(ds.startRow, ds.startCol, ds.size, ds.orientation)
    }
    val oppShips: List<UiShip> = (state.ships[opponentId] ?: emptyList()).map { ds ->
        UiShip(ds.startRow, ds.startCol, ds.size, ds.orientation)
    }


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

    var rematchVotes by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var lobbyStatus by remember { mutableStateOf<String?>(null) }
    var surrenderedUserId by remember { mutableStateOf<String?>(null) }
    var showSurrenderDialog by remember { mutableStateOf(false) }

    var animatingMove by remember { mutableStateOf<Cell?>(null) }
    var animIsHit by remember { mutableStateOf<Boolean?>(null) }
    var pendingMove by remember { mutableStateOf<Triple<Int, Int, (() -> Unit)>?>(null) }

    val havePlacedShips = myShips.isNotEmpty() && oppShips.isNotEmpty()
    val myMoves = state.moves.filter { it.playerId == uid }
    val oppMoves = state.moves.filter { it.playerId == opponentId }
    val iSunkOpponent = havePlacedShips && areAllShipsSunk(oppShips, myMoves)
    val opponentSunkMe = havePlacedShips && areAllShipsSunk(myShips, oppMoves)

    // Find the last move by opponent (on your board)
    val lastOppMove = oppMoves.lastOrNull()
    val wasHit = remember(lastOppMove) {
        lastOppMove?.let { move ->
            myShips.any { it.covers(move.y, move.x) }
        } ?: false
    }

    fun vibrateDevice(context: Context, duration: Long = 500) { // Try 500-800ms
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(duration, 255) // 255 = max amplitude
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }

    // Vibrate if opponent hit you (when new move appears)
    LaunchedEffect(lastOppMove) {
        if (wasHit && lastOppMove != null) {
            vibrateDevice(context)
        }
    }

    val gameResult = state.gameResult
    val isWinner = gameResult == uid
    val isBackendGameOver = gameResult != null && gameResult != ""
    val isSurrenderedGameOver = surrenderedUserId != null
    val isLocallyGameOver = (iSunkOpponent || opponentSunkMe || isBackendGameOver || isSurrenderedGameOver) && havePlacedShips


    // --- MINE CELLS ---
    val myMines = state.placedMines[uid] ?: emptyList()
    val enemyMines = state.placedMines[opponentId] ?: emptyList()

    // Triggered mines = any enemy mine that was attacked by YOU (for enemy board)
    // For your own board, any of your mines that were hit by the opponent
    val myTriggeredMines = state.triggeredMines[uid] ?: emptyList()
    val enemyTriggeredMines = state.triggeredMines[opponentId] ?: emptyList()

    var minesAtTurnStart by remember(state.currentTurn) {
        mutableStateOf(myMines.size)
    }
    val hasPlacedMineThisTurn = myMines.size > minesAtTurnStart

    var boardOffset by remember { mutableStateOf(Offset.Zero) }
    var cellSizePx by remember { mutableStateOf(0f) }

    suspend fun spendEnergy(roomRef: com.google.firebase.firestore.DocumentReference, uid: String, amount: Int) {
        roomRef.update(
            "gameState.battleships.energy.$uid",
            FieldValue.increment(-amount.toLong())
        ).await()
    }

    DisposableEffect(roomCode) {
        val reg = roomRef.addSnapshotListener { snap, _ ->
            val bs = ((snap?.get("gameState") as? Map<*, *>)?.get("battleships") as? Map<*, *>) ?: return@addSnapshotListener
            val votes = (bs["rematchVotes"] as? Map<*, *>)?.mapKeys { it.key.toString() }?.mapValues { it.value == true } ?: emptyMap()
            rematchVotes = votes
            lobbyStatus = snap.get("status") as? String
            surrenderedUserId = bs["surrendered"] as? String
        }
        onDispose { reg.remove() }
    }

    LaunchedEffect(lobbyStatus) {
        if (lobbyStatus == "ended") {
            navController.navigate(
                NavRoutes.LOBBY_MENU.replace("{gameId}", "battleships")
            ) {
                popUpTo(0)
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(rematchVotes) {
        if (rematchVotes.size == 2 && rematchVotes.values.all { it }) {
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

    LaunchedEffect(animIsHit) {
        if (animIsHit == true) {
            vibrateDevice(context, 500) // 500ms for a stronger vibration!
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // --- FULLSCREEN BACKGROUND IMAGE ---
        Image(
            painter = painterResource(com.example.gamehub.R.drawable.bg_battleships), // your background
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("🐋 Battleships", color = Color.White) },
                    actions = {
                        IconButton(
                            onClick = { showSurrenderDialog = true }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Surrender", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
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
                    color = Color.White // White for top text
                )
                Spacer(Modifier.height(16.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (placingMine) {
                        BoardGrid(
                            gridSize = 10,
                            cellSize = 32.dp,
                            ships = myShips,
                            attacks = buildAttackMap(myShips, oppMoves),
                            enabled = true,
                            destroyedShips = myDestroyedShips,
                            mineCells = myMines,
                            triggeredMines = myTriggeredMines,
                            onCellClick = { row, col ->
                                val isMineHere = myMines.any { it.row == row && it.col == col }
                                val alreadyHit = oppMoves.any { it.x == col && it.y == row }
                                if (!isMineHere && !alreadyHit && (state.energy[uid] ?: 0) >= PowerUp.Mine.cost) {
                                    scope.launch {
                                        try {
                                            spendEnergy(roomRef, uid, PowerUp.Mine.cost)
                                            val updatedMines = myMines + Cell(row, col)
                                            roomRef.update(
                                                "gameState.battleships.placedMines.$uid",
                                                updatedMines.map { mapOf("row" to it.row, "col" to it.col) }
                                            ).await()
                                            placingMine = false
                                            selectedPowerUp = null
                                        } catch (e: Exception) {
                                            println("ERROR PLACING MINE: ${e.message}")
                                        }
                                    }
                                }
                            }
                        )
                    }
                    else {
                        if (isMyTurn) {
                            val myHitCells = myMoves.map { Cell(it.y, it.x) }.toSet()
                            BoardGrid(
                                gridSize = 10,
                                cellSize = 32.dp,
                                ships = emptyList(),
                                attacks = buildAttackMap(oppShips, myMoves),
                                enabled = !isLocallyGameOver,
                                destroyedShips = oppDestroyedShips,
                                mineCells = emptyList(),            // ← Hide untriggered mines and don't double-show triggered
                                triggeredMines = enemyTriggeredMines,
                                onCellClick = { row, col ->
                                    if (!isMyTurn || isLocallyGameOver) return@BoardGrid
                                    val cell = Cell(row, col)
                                    val myHitCells = myMoves.map { Cell(it.y, it.x) }.toSet()

                                    when (selectedPowerUp) {
                                        PowerUp.Bomb2x2 -> {
                                            if (row in 0 until 9 && col in 0 until 9 && (state.energy[uid] ?: 0) >= PowerUp.Bomb2x2.cost) {
                                                scope.launch {
                                                    spendEnergy(roomRef, uid, PowerUp.Bomb2x2.cost)
                                                    val targets = PowerUp.Bomb2x2.expand(Cell(row, col))
                                                    targets.forEach { targetCell ->
                                                        session.submitMove(targetCell.col, targetCell.row, uid)
                                                    }
                                                    selectedPowerUp = null
                                                }
                                            }
                                        }
                                        PowerUp.Laser -> {
                                            if ((state.energy[uid] ?: 0) >= PowerUp.Laser.cost) {
                                                scope.launch {
                                                    // 2. Spend energy
                                                    spendEnergy(roomRef, uid, PowerUp.Laser.cost)

                                                    // 3. Calculate Laser targets
                                                    val targets = if (laserOrientation == Orientation.Horizontal) {
                                                        (0 until 10).map { c -> Cell(row, c) }
                                                    } else {
                                                        (0 until 10).map { r -> Cell(r, col) }
                                                    }

                                                    // 4. Fire all laser shots (could also do in parallel)
                                                    targets.forEach { targetCell ->
                                                        session.submitMove(targetCell.col, targetCell.row, uid)
                                                    }
                                                    selectedPowerUp = null

                                                }
                                            }
                                        }
                                        else -> {
                                            if (cell !in myHitCells && animatingMove == null && cellSizePx > 0f) {
                                                println("DEBUG: boardOffset=$boardOffset, cellSizePx=$cellSizePx, cell=$cell")

                                                // 1. Mark animation as in progress and what type
                                                val isHit = oppShips.any { it.covers(row, col) }
                                                animatingMove = cell
                                                animIsHit = isHit
                                                // 2. Store the move to send after animation
                                                pendingMove = Triple(col, row) {
                                                    scope.launch {
                                                        session.submitMove(col, row, uid)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                                onBoardPositioned = { offset, cellPx ->
                                    boardOffset = offset
                                    cellSizePx = cellPx
                                }
                            )
                        } else {
                            BoardGrid(
                                gridSize = 10,
                                cellSize = 32.dp,
                                ships = myShips,
                                attacks = buildAttackMap(myShips, oppMoves),
                                enabled = false,
                                destroyedShips = myDestroyedShips,
                                mineCells = myMines,
                                triggeredMines = myTriggeredMines,
                                onCellClick = { _, _ -> }
                            )
                        }
                    }
                }

                if (placingMine) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Select a square on your board to place the mine.",
                            color = Color.White,
                            modifier = Modifier
                                .background(Color(0xAA222222), shape = MaterialTheme.shapes.medium)
                                .padding(horizontal = 24.dp, vertical = 10.dp)
                        )
                    }
                }
                else if (selectedPowerUp is PowerUp.Bomb2x2) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Select a 2x2 zone on your opponent's board to drop the bomb. The cell you click will be the top-left corner of the bomb.",
                            color = Color.White,
                            modifier = Modifier
                                .background(Color(0xAA222222), shape = MaterialTheme.shapes.medium)
                                .padding(horizontal = 24.dp, vertical = 10.dp)
                        )
                    }
                }
                else if (selectedPowerUp is PowerUp.Laser) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Select a row or column to fire the laser. The laser will hit all cells in that row/column. Press the button below to toggle between row and column.",
                            color = Color.White,
                            modifier = Modifier
                                .background(Color(0xAA222222), shape = MaterialTheme.shapes.medium)
                                .padding(horizontal = 24.dp, vertical = 10.dp)
                        )
                    }
                }

                if (selectedPowerUp != null || placingMine) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Cancel PowerUp button (unchanged)
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

                            // Laser orientation button (if needed)
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

                Spacer(Modifier.height(8.dp))
                ShipsStatusPanel(
                    opponentShips = oppShips,
                    yourMoves = myMoves,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                // Power-up panel: grey box for contrast
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
                        // --- LASER TOGGLE BUTTON ---
                        if (selectedPowerUp == PowerUp.Laser) {
                            Button(
                                onClick = {
                                    laserOrientation =
                                        if (laserOrientation == Orientation.Horizontal)
                                            Orientation.Vertical
                                        else
                                            Orientation.Horizontal
                                },
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text("Laser: ${if (laserOrientation == Orientation.Horizontal) "Row" else "Column"}")
                            }
                        }
                    }
                }
            }

            // --- SURRENDER DIALOG ---
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
                                    roomRef.update("gameState.battleships.surrendered", uid).await()
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

            // --- GAME OVER DIALOG ---
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
                    },
                    containerColor = Color(0xFF222222)
                )
            }
        }
        if (animatingMove != null) {
            CannonAttackAnimation(
                boardOffset = boardOffset,
                cell = animatingMove!!,
                cellSizePx = cellSizePx,
                isHit = animIsHit,
                onFinished = {
                    // After animation is done, submit the move and clear state
                    val (x, y, submitFn) = pendingMove!!
                    submitFn()
                    animatingMove = null
                    animIsHit = null
                    pendingMove = null
                }
            )
        }
    }
}


// BoardGrid & helpers
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
    mineCells: List<Cell> = emptyList(),
    triggeredMines: List<Cell> = emptyList(),
    onCellClick: (row: Int, col: Int) -> Unit,
    onBoardPositioned: ((Offset, Float) -> Unit)? = null
) {
    val density = LocalDensity.current

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
            .border(4.dp, Color.Black, RectangleShape)
            .onGloballyPositioned { coordinates ->
                // Give the parent the board offset and the cell size in px
                val position = coordinates.positionInRoot()
                val offset = Offset(position.x, position.y)
                val cellPx = with(density) { cellSize.toPx() }
                onBoardPositioned?.invoke(offset, cellPx)
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            for (row in 0 until gridSize) {
                Row {
                    for (col in 0 until gridSize) {
                        val cell = Cell(row, col)
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
                            // --- MINES AND HITS/MISSES ---
                            // Show (in order of priority!):
                            // 1. Triggered mine (this is a mine that has been hit -- always show the mine/explosion PNG, never the hit dot!)
                            // 2. Untriggered mine (your own board only)
                            // 3. Hit (only if not a mine cell)
                            // 4. Miss (only if not a mine cell)
                            when {
                                triggeredMines.any { it.row == cell.row && it.col == cell.col } -> {
                                    Image(
                                        painter = painterResource(com.example.gamehub.R.drawable.mine), // Or an explosion drawable if you want
                                        contentDescription = "Triggered Mine",
                                        modifier = Modifier.size(cellSize * 2f)
                                    )
                                }
                                mineCells.any { it.row == cell.row && it.col == cell.col } -> {
                                    Image(
                                        painter = painterResource(com.example.gamehub.R.drawable.mine),
                                        contentDescription = "Mine",
                                        modifier = Modifier.size(cellSize * 2f)
                                    )
                                }
                                attacks[cell] == AttackResult.Hit -> {
                                    Image(
                                        painter = painterResource(com.example.gamehub.R.drawable.hit_circle),
                                        contentDescription = null,
                                        modifier = Modifier.size(cellSize / 2)
                                    )
                                }
                                attacks[cell] == AttackResult.Miss -> {
                                    Image(
                                        painter = painterResource(com.example.gamehub.R.drawable.miss_circle),
                                        contentDescription = null,
                                        modifier = Modifier.size(cellSize / 2)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Ship outlines and grid lines, as before ---
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

        Box(modifier = Modifier.matchParentSize()) {
            for (i in 1 until gridSize) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .absoluteOffset(x = cellSize * i, y = 0.dp)
                        .background(Color.Gray)
                )
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
