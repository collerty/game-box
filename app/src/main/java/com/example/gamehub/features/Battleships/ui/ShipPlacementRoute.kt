package com.example.gamehub.features.battleships.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.example.gamehub.repository.interfaces.IBattleShipsRepository // NEW IMPORT

@Composable
fun ShipPlacementRoute(
    navController: NavHostController,
    code: String,
    userName: String,
    mapId: Int,
    // ACCEPT THE REPOSITORY HERE
    battleshipsRepository: IBattleShipsRepository
) {
    ShipPlacementScreen(
        navController = navController,
        code          = code,
        userName      = userName,
        mapId         = mapId,
        // PASS THE REPOSITORY HERE
        battleshipsRepository = battleshipsRepository
    )
}