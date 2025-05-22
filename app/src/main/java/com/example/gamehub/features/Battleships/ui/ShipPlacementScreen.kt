package com.example.gamehub.features.battleships.ui

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.gamehub.navigation.NavRoutes
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.gamehub.features.battleships.ui.Orientation
import com.example.gamehub.features.battleships.ui.Ship
/**
 * ShipPlacementScreen: let the user place (or remove) each ship in turn,
 * then proceed to the play screen once all ships are placed.
 *
 * @param navController    Nav controller for navigation
 * @param code             The game room code
 * @param userName         The current player’s UID
 * @param mapId            The map selected in the vote phase
 */
@Composable
fun ShipPlacementScreen(
    navController: NavHostController,
    code: String,
    userName: String,
    mapId: Int
) {
    // Firestore reference to this room
    val db      = Firebase.firestore
    val roomRef = remember { db.collection("rooms").document(code) }
    val scope   = rememberCoroutineScope()

    // Standard Battleships sizes
    val shipSizes = listOf(5, 4, 3, 3, 2)

    // UI state: placed ships & current orientation
    var placedShips by remember { mutableStateOf<List<Ship>>(emptyList()) }
    var orientation by remember { mutableStateOf(Orientation.Horizontal) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // 1) Interactive map
        BattleshipMap(
            gridSize    = 10,
            cellSize    = 32.dp,
            ships       = placedShips,
            onCellClick = { row, col ->
                // Remove or place ship if under the limit
                val existing = placedShips.firstOrNull { it.covers(row, col) }
                placedShips = if (existing != null) {
                    placedShips - existing
                } else {
                    val nextSize = shipSizes.getOrNull(placedShips.size) ?: return@BattleshipMap
                    placedShips + Ship(row, col, nextSize, orientation)
                }
            }
        )

        Spacer(Modifier.height(16.dp))

        // 2) Rotate & Done buttons
        Row {
            // Rotate is allowed until all ships placed
            Button(
                onClick = {
                    orientation = if (orientation == Orientation.Horizontal)
                        Orientation.Vertical else Orientation.Horizontal
                },
                enabled = placedShips.size < shipSizes.size
            ) {
                Text("Rotate")
            }

            Spacer(Modifier.width(16.dp))

            // Done only enabled once all ships are placed
            Button(
                onClick = {
                    // Persist placements to Firestore
                    scope.launch {
                        val payload = placedShips.map { ship ->
                            mapOf(
                                "startRow"    to ship.startRow,
                                "startCol"    to ship.startCol,
                                "size"        to ship.size,
                                "orientation" to ship.orientation.name
                            )
                        }
                        roomRef
                            .update("gameState.battleships.ships.$userName", payload)
                            .await()
                    }
                    // Navigate to play screen using NavRoutes.BATTLESHIPS_GAME
                    val route = NavRoutes.BATTLESHIPS_GAME
                        .replace("{code}", code)
                        .replace("{userName}", Uri.encode(userName))
                    navController.navigate(route)
                },
                enabled = placedShips.size == shipSizes.size
            ) {
                Text("Done")
            }
        }
    }
}

/** True if this ship covers the given (row,col) */
fun Ship.covers(row: Int, col: Int): Boolean =
    if (orientation == Orientation.Horizontal) {
        row == startRow && col in startCol until (startCol + size)
    } else {
        col == startCol && row in startRow until (startRow + size)
    }
