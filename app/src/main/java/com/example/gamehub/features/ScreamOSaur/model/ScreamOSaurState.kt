package com.example.gamehub.features.ScreamOSaur.model

import kotlin.random.Random

// Game state enum
enum class GameState { READY, PLAYING, PAUSED, GAME_OVER }

// Obstacle data class
data class Obstacle(
    var xPosition: Float,
    val height: Float,
    val width: Float,
    var passed: Boolean = false,
    val id: Long = Random.nextLong()
)

// UI State data class
data class ScreamOSaurUiState(
    val gameState: GameState = GameState.READY,
    val score: Int = 0,
    val obstacles: List<Obstacle> = emptyList(),
    val currentAmplitude: Int = 0,
    val jumpAnimValue: Float = 0f,
    val isJumping: Boolean = false,
    val runningAnimState: Int = 0,
    val dinosaurVisualXPositionPx: Float = 0f,
    val dinoTopYOnGroundPx: Float = 0f,
    val dinosaurSizePx: Float = 0f,
    val gameHeightPx: Float = 0f,
    val groundHeightPx: Float = 0f,
    val jumpMagnitudePx: Float = 0f,
    val gameSpeed: Float = 5f, // Corresponds to INITIAL_GAME_SPEED
    val hasAudioPermission: Boolean? = null
)

