// features/battleships/ui/Ship.kt
package com.example.gamehub.features.battleships.ui

data class Ship(
    val startRow: Int,
    val startCol: Int,
    val size: Int,
    val orientation: Orientation
)
