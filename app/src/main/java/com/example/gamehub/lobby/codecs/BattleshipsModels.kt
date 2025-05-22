package com.example.gamehub.lobby.codec

/** A shot or action in the game. */
data class BattleshipsMove(val position: String, val playerUid: String)

/** The final result once the game ends. */
data class GameResult(val winner: String, val loser: String, val reason: String)

/**
 * The shared state for Battleships, extended with voting.
 */
data class BattleshipsState(
    val currentTurn: String,
    val moves: List<String>,
    val gameResult: GameResult?,

    // NEW: who voted for which map
    val mapVotes: Map<String, Int> = emptyMap(),
    // NEW: the chosen map ID once votes complete
    val chosenMap: Int? = null
)
