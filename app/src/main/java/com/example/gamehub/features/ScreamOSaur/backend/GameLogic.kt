package com.example.gamehub.features.ScreamOSaur.backend

import com.example.gamehub.features.ScreamOSaur.model.Obstacle
import com.example.gamehub.features.ScreamOSaur.model.ScreamOSaurUiState
import kotlin.random.Random

class GameLogic {

    fun createNewObstacle(canvasWidthPx: Float, state: ScreamOSaurUiState): Obstacle {
        // The gap starts large and decreases as the score increases
        val scoreFactor = (state.score / 40f).coerceIn(0f, 1f) // Normalize score effect, max effect at score 40
        val gapReduction = (canvasWidthPx * 0.4f) * (1 - scoreFactor)
        val randomGap = Random.nextFloat() * (canvasWidthPx * 0.15f)

        val baseHeight = state.gameHeightPx
        return Obstacle(
            xPosition = canvasWidthPx + gapReduction + randomGap,
            height = Random.nextFloat() * (baseHeight * 0.275f - baseHeight * 0.125f) + baseHeight * 0.125f,
            width = Random.nextFloat() * (baseHeight * 0.15f - baseHeight * 0.075f) + baseHeight * 0.075f
        )
    }
}

