package com.example.gamehub.features.battleships.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

@Composable
fun ShipPlacementRoute(
    navController: NavHostController,
    code: String,
    userName: String,
    mapId: Int
) {
    ShipPlacementScreen(
        navController = navController,
        code          = code,
        userName      = userName,
        mapId         = mapId
    )
}
