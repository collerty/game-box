package com.example.gamehub.features.battleships.model

import com.example.gamehub.R

object MapRepository {
    private fun fullSquare(n: Int): Set<Cell> =
        (0 until n).flatMap { r -> (0 until n).map { c -> Cell(r, c) } }.toSet()

    private fun diamond(n: Int): Set<Cell> {
        val mid = (n - 1) / 2
        return (0 until n).flatMap { r ->
            (0 until n).mapNotNull { c ->
                if (kotlin.math.abs(r - mid) + kotlin.math.abs(c - mid) <= mid)
                    Cell(r, c)
                else null
            }
        }.toSet()
    }

    private fun cutCorners(n: Int): Set<Cell> {
        val corners = mutableSetOf<Cell>().apply {
            addAll((0 until 2).flatMap { r -> (0 until 2).map { c -> Cell(r, c) } })
            addAll((0 until 2).flatMap { r -> ((n - 2) until n).map { c -> Cell(r, c) } })
            addAll(((n - 2) until n).flatMap { r -> (0 until 2).map { c -> Cell(r, c) } })
            addAll(((n - 2) until n).flatMap { r -> ((n - 2) until n).map { c -> Cell(r, c) } })
        }
        return fullSquare(n) - corners
    }

    /** A “plus‐sign” through the center row and center column */
    private fun plusSign(n: Int): Set<Cell> {
        val mid = n / 2
        val horizontal = (0 until n).map { c -> Cell(mid, c) }
        val vertical   = (0 until n).map { r -> Cell(r, mid) }
        return (horizontal + vertical).toSet()
    }

    /** Only the outer border (a “ring”) */
    private fun ring(n: Int): Set<Cell> {
        val top    = (0 until n).map    { c -> Cell(0, c) }
        val bottom = (0 until n).map    { c -> Cell(n - 1, c) }
        val left   = (1 until n - 1).map{ r -> Cell(r, 0) }
        val right  = (1 until n - 1).map{ r -> Cell(r, n - 1) }
        return (top + bottom + left + right).toSet()
    }

    /** All available maps in a 10×10 grid */
    val allMaps: List<MapDefinition> = listOf(
        MapDefinition(
            id           = 0,
            name         = "Square Sea",
            thumbnailRes = R.drawable.map_square_thumb,
            rows         = 10,
            cols         = 10,
            validCells   = fullSquare(10)
        ),
        MapDefinition(
            id           = 1,
            name         = "Diamond Bay",
            thumbnailRes = R.drawable.map_diamond_thumb,
            rows         = 10,
            cols         = 10,
            validCells   = diamond(10)
        ),
        MapDefinition(
            id           = 2,
            name         = "Cut-Corner Cove",
            thumbnailRes = R.drawable.map_cutcorners_thumb,
            rows         = 10,
            cols         = 10,
            validCells   = cutCorners(10)
        ),
        MapDefinition(
            id           = 3,
            name         = "Crossroads",
            thumbnailRes = R.drawable.map_plus_thumb,
            rows         = 10,
            cols         = 10,
            validCells   = plusSign(10)
        ),
        MapDefinition(
            id           = 4,
            name         = "Border Run",
            thumbnailRes = R.drawable.map_ring_thumb,
            rows         = 10,
            cols         = 10,
            validCells   = ring(10)
        )
    )
}
