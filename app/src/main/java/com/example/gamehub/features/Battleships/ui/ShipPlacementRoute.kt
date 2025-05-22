package com.example.gamehub.features.battleships.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

/**
 * Route wrapper for the ShipPlacementScreen that takes the NavController,
 * code and userName from NavHost arguments.
 */
@Composable
fun ShipPlacementRoute(
    navController: NavHostController,
    code: String,
    userName: String,
    mapId: Int
) {
    // delegate directly to the screen, which now does its own Firestore writes
    ShipPlacementScreen(
        navController = navController,
        code          = code,
        userName      = userName
    )
}
