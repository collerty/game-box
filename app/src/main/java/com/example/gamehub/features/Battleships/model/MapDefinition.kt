package com.example.gamehub.features.battleships.model

import androidx.annotation.DrawableRes

/**
 * Defines one playable map:
 *  - id/name for voting
 *  - thumbnailRes: small preview drawable
 *  - rows×cols grid dimensions
 *  - validCells: which cells are “in play”
 */
data class MapDefinition(
    val id: Int,
    val name: String,
    @DrawableRes val thumbnailRes: Int,
    val rows: Int,
    val cols: Int,
    val validCells: Set<Cell>
)
