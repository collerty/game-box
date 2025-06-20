package com.example.gamehub.lobby.model

import com.example.gamehub.features.battleships.ui.Orientation

data class CellState(val hasShip: Boolean, val wasHit: Boolean, val wasMiss: Boolean)

object BoardBuilder {
    fun build(ships: List<Ship>, moves: List<Move>): List<CellState> {
        val grid = 10
        val cells = MutableList(grid * grid) { CellState(false, false, false) }

        // place ships
        ships.forEach { ship ->
            val coords = if (ship.orientation == Orientation.Horizontal) {
                (0 until ship.size).map { offset -> ship.startRow to (ship.startCol + offset) }
            } else {
                (0 until ship.size).map { offset -> (ship.startRow + offset) to ship.startCol }
            }
            coords.forEach { (r, c) ->
                cells[r * grid + c] = cells[r * grid + c].copy(hasShip = true)
            }
        }

        // apply moves
        moves.forEach { m ->
            val idx = m.y * grid + m.x
            cells[idx] = if (cells[idx].hasShip) {
                cells[idx].copy(wasHit = true)
            } else {
                cells[idx].copy(wasMiss = true)
            }
        }

        return cells
    }
}
