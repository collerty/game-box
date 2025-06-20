package com.example.gamehub.features.MemoryMatching.logic

import com.example.gamehub.R
import com.example.gamehub.features.MemoryMatching.model.GameDifficulty
import com.example.gamehub.features.MemoryMatching.model.MemoryCard

class GameLogic {

    val allImageResources: List<Int> = listOf(
        R.drawable.basketball, R.drawable.bee, R.drawable.dice, R.drawable.herosword,
        R.drawable.ladybug, R.drawable.ramen, R.drawable.taxi, R.drawable.zombie,
        R.drawable.zelda, R.drawable.spaceman, R.drawable.robot, R.drawable.island,
        R.drawable.gamingcontroller, R.drawable.dragon, R.drawable.browncar
    )

    fun generateCardsForDifficulty(difficulty: GameDifficulty): List<MemoryCard> {
        val numPairs = difficulty.pairs
        val uniqueImagesToTake = kotlin.math.min(numPairs, allImageResources.size)
        val selectedImages = allImageResources.shuffled().take(uniqueImagesToTake)
        return (selectedImages + selectedImages)
            .mapIndexed { index, resId -> MemoryCard(id = index, imageRes = resId) }
            .shuffled()
    }
}

