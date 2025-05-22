package com.example.gamehub.features.battleships.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.gamehub.lobby.FirestoreSession
import com.example.gamehub.lobby.LobbyService
import com.example.gamehub.lobby.codec.BattleshipsCodec
import com.example.gamehub.lobby.model.CellState
import com.example.gamehub.lobby.model.PowerUp
import com.example.gamehub.lobby.model.Ship
import com.example.gamehub.features.battleships.ui.PowerUpPanel  // ← import
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.annotation.DrawableRes


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BattleshipsPlayScreen(
    navController: NavHostController,
    roomCode: String,
    userName: String
) {
    val uid         = Firebase.auth.uid ?: return
    val db          = Firebase.firestore
    val roomRef     = remember { db.collection("rooms").document(roomCode) }
    val sessionRepo = remember { FirestoreSession(roomCode, BattleshipsCodec) }
    val session     by sessionRepo.stateFlow.collectAsState()
    val isMyTurn     = session.currentTurn == userName
    val scope       = rememberCoroutineScope()
    var showDialog  by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("🐋 Battleships") },
            actions = {
                IconButton(onClick = { showDialog = true }) {
                    Icon(Icons.Default.ExitToApp, contentDescription = "Surrender")
                }
                IconButton(onClick = { /* settings */ }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        )
    }) { padding ->
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

        Column(Modifier.padding(padding).padding(16.dp)) {
            Text(
                "Current turn: ${if (isMyTurn) "You" else "Opponent"}",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(16.dp))

            Row(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Opponent board
                BoardGrid(
                    gridSize = 10,
                    cellSize = 32.dp,
                    ships    = emptyList(),
                    attacks  = buildAttackMap(session.getOpponentView(userName)),
                    enabled  = isMyTurn
                ) { r, c ->
                    if (isMyTurn) scope.launch { sessionRepo.submitMove(r, c, userName) }
                }

                // Your board
                BoardGrid(
                    gridSize = 10,
                    cellSize = 32.dp,
                    ships    = session.ships[userName] ?: emptyList(),
                    attacks  = buildAttackMap(session.getPlayerView(userName)),
                    enabled  = false
                ) { _, _ -> }
            }

            Spacer(Modifier.height(16.dp))
            if (isMyTurn) {
                PowerUpPanel(
                    available = session.availablePowerUps[userName] ?: emptyList()
                ) { pu -> /* handle */ }
            }
        }
    }
}

// helper
private fun buildAttackMap(states: List<CellState>) =
    states.mapIndexedNotNull { idx, st ->
        val r = idx / 10; val c = idx % 10
        when {
            st.wasHit  -> Cell(r, c) to AttackResult.Hit
            st.wasMiss -> Cell(r, c) to AttackResult.Miss
            else       -> null
        }
    }.toMap()

// ... BoardGrid, Cell, AttackResult, covers(cell) etc. same as before ...


// ------------------------------------------------------------
// BoardGrid + supporting types
// ------------------------------------------------------------
@Composable
fun BoardGrid(
    gridSize: Int,
    cellSize: Dp,
    ships: List<Ship>,
    attacks: Map<Cell, AttackResult>,
    enabled: Boolean,
    onCellClick: (row: Int, col: Int) -> Unit
) {
    val frame = rememberWaterFrame(
        spriteRes = com.example.gamehub.R.drawable.ocean_spritesheet,
        framesPerRow = 8,
        totalFrames = 16,
        fps = 8
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(gridSize),
        modifier = Modifier.size(cellSize * gridSize)
    ) {
        items((0 until gridSize).flatMap { r -> (0 until gridSize).map { c -> Cell(r, c) } }) { cell ->
            Box(
                Modifier
                    .size(cellSize)
                    .clickable(enabled) { onCellClick(cell.row, cell.col) },
                contentAlignment = Alignment.Center
            ) {
                AnimatedWaterCell(
                    frame = frame,
                    spriteRes = com.example.gamehub.R.drawable.ocean_spritesheet,
                    framesPerRow = 8,
                    totalFrames = 16,
                    size = cellSize
                )
                if (ships.any { it.covers(cell) }) {
                    Box(
                        Modifier
                            .matchParentSize()
                            .background(Color(0xFF4B8B1D))
                    )
                }
                attacks[cell]?.let { res ->
                    @DrawableRes val img =
                        if (res == AttackResult.Hit) com.example.gamehub.R.drawable.hit_circle
                        else com.example.gamehub.R.drawable.miss_circle
                    Image(
                        painter = painterResource(img),
                        contentDescription = null,
                        modifier = Modifier.size(cellSize / 2)
                    )
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
