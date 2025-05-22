package com.example.gamehub.features.battleships.model

data class MapDefinition(
    val id: Int,
    val name: String,
    val thumbnailRes: Int,
    val rows: Int,
    val cols: Int,
    val validCells: Set<Cell>
)
