package com.example.gamehub.features.MemoryMatching.model

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf

// Data class to hold card information
data class MemoryCard(
    val id: Int, // Unique ID for the card instance
    val imageRes: Int, // Drawable resource ID for the image
    var isFlipped: Boolean = false,
    var isMatched: Boolean = false
)

// Enum for managing screen states
enum class GameScreen {
    DIFFICULTY_SELECTION,
    PLAYING
}

// Data class for defining difficulty levels
data class GameDifficulty(
    val pairs: Int,
    val columns: Int,
    val displayName: String,
    val cardBackResId: Int, // Resource ID for the card back image
    val timeLimitSeconds: Int, // Time limit for this difficulty
    val maxAttempts: Int, // Maximum incorrect attempts allowed
    val totalCards: Int = pairs * 2
)

// Game State data class
data class MemoryMatchingState(
    val cards: SnapshotStateList<MemoryCard> = mutableStateListOf(),
    val flippedCardIndices: List<Int> = emptyList(),
    val processingMatch: Boolean = false,
    val attemptCount: Int = 0,
    val currentTurnIncorrectAttempts: Int = 0,
    val allPairsMatched: Boolean = false,
    val timeLeftInSeconds: Int = 0,
    val isTimerRunning: Boolean = false,
    val showLoseScreen: Boolean = false,
    val loseReason: String? = null,
    val currentDifficulty: GameDifficulty? = null,
    val currentScreen: GameScreen = GameScreen.DIFFICULTY_SELECTION
)
