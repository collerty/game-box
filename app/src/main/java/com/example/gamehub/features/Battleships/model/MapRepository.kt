package com.example.gamehub.features.battleships.model

import com.example.gamehub.R

object MapRepository {
    private fun fullSquare(n: Int): Set<Cell> =
        (0 until n).flatMap { r -> (0 until n).map { c -> Cell(r, c) } }.toSet()

    private fun diamondMap(): Set<Cell> {
        val pattern = listOf(
            "....XX....",
            "...XXXX...",
            "..XXXXXX..",
            ".XXXXXXXX.",
            "XXXXXXXXXX",
            "XXXXXXXXXX",
            ".XXXXXXXX.",
            "..XXXXXX..",
            "...XXXX...",
            "....XX....",
        )
        // Optionally pad all lines to length 10 if not already
        val padded = pattern.map {
            if (it.length < 10) ".".repeat((10 - it.length) / 2) + it + ".".repeat((10 - it.length + 1) / 2) else it
        }
        val cells = mutableSetOf<Cell>()
        for ((row, line) in padded.withIndex()) {
            for ((col, char) in line.withIndex()) {
                if (char == 'X') {
                    cells.add(Cell(row, col))
                }
            }
        }
        return cells
    }

    private fun cutCornersMap(): Set<Cell> {
        val pattern = listOf(
            "..XXXXXX..",
            "..XXXXXX..",
            "XXXXXXXXXX",
            "XXXXXXXXXX",
            "XXXXXXXXXX",
            "XXXXXXXXXX",
            "XXXXXXXXXX",
            "XXXXXXXXXX",
            "..XXXXXX..",
            "..XXXXXX.."
        )
        val cells = mutableSetOf<Cell>()
        for ((row, line) in pattern.withIndex()) {
            for ((col, char) in line.withIndex()) {
                if (char == 'X') {
                    cells.add(Cell(row, col))
                }
            }
        }
        return cells
    }

    private fun crossroadsMap(): Set<Cell> {
        val pattern = listOf(
            "...XXXX...",
            ".XXXXXXXX.",
            ".X.XXXX.X.",
            "XXXXXXXXXX",
            "XXXXXXXXXX",
            "XXXXXXXXXX",
            "XXXXXXXXXX",
            ".X.XXXX.X.",
            ".XXXXXXXX.",
            "...XXXX..."
        )
        val cells = mutableSetOf<Cell>()
        for ((row, line) in pattern.withIndex()) {
            for ((col, char) in line.withIndex()) {
                if (char == 'X') {
                    cells.add(Cell(row, col))
                }
            }
        }
        return cells
    }

    private fun plusMap(): Set<Cell> {
        val pattern = listOf(
            "...XXXX...",
            "...XXXX...",
            "...XXXX...",
            "XXXXXXXXXX",
            "XXXXXXXXXX",
            "XXXXXXXXXX",
            "XXXXXXXXXX",
            "...XXXX...",
            "...XXXX...",
            "...XXXX..."
        )
        val cells = mutableSetOf<Cell>()
        for ((row, line) in pattern.withIndex()) {
            for ((col, char) in line.withIndex()) {
                if (char == 'X') {
                    cells.add(Cell(row, col))
                }
            }
        }
        return cells
    }

    private fun CircleMap(): Set<Cell> {
        val pattern = listOf(
            "..........",
            "..XXXXXX..",
            ".XXXXXXXX.",
            ".XXXXXXXX.",
            "XXXXXXXXXX",
            "XXXXXXXXXX",
            ".XXXXXXXX.",
            ".XXXXXXXX.",
            "..XXXXXX..",
            "..........."
        )
        val cells = mutableSetOf<Cell>()
        for ((row, line) in pattern.withIndex()) {
            for ((col, char) in line.withIndex()) {
                if (char == 'X') {
                    cells.add(Cell(row, col))
                }
            }
        }
        return cells
    }

    // --- All available maps in a 10×10 grid ---
    val allMaps: List<MapDefinition> = listOf(
        MapDefinition(
            id         = 0,
            name       = "Square Sea",
            previewRes = R.drawable.map_square,
            rows       = 10,
            cols       = 10,
            validCells = fullSquare(10)
        ),
        MapDefinition(
            id         = 1,
            name       = "Diamond Bay",
            previewRes = R.drawable.map_diamond,
            rows       = 10,
            cols       = 10,
            validCells = diamondMap()
        ),
        MapDefinition(
            id         = 2,
            name       = "Cut-Corner",
            previewRes = R.drawable.map_cutcorner,
            rows       = 10,
            cols       = 10,
            validCells = cutCornersMap()
        ),
        MapDefinition(
            id         = 3,
            name       = "Crossroads",
            previewRes = R.drawable.map_crossroads,
            rows       = 10,
            cols       = 10,
            validCells = crossroadsMap()
        ),
        MapDefinition(
            id         = 5,
            name       = "Plus",
            previewRes = R.drawable.map_plus,
            rows       = 10,
            cols       = 10,
            validCells = plusMap()
        ),
        MapDefinition(
            id         = 7,
            name       = "Circle",
            previewRes = R.drawable.map_circle, // use your thumb image
            rows       = 10,
            cols       = 10,
            validCells = CircleMap()
        ),
    )
}
