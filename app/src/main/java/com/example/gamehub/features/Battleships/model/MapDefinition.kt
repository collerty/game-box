package com.example.gamehub.features.battleships.model

data class MapDefinition(
    val id: Int,
    val name: String,
    val previewRes: Int,     // The big PNG for the vote screen
    val rows: Int,
    val cols: Int,
    val validCells: Set<Cell>
)
