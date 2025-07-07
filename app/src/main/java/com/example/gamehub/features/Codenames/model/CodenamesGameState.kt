package com.example.gamehub.features.codenames.model

data class CodenamesGameState(
    val cards: List<CodenamesCard> = emptyList(),
    val currentTurn: String = "RED",
    val redWordsRemaining: Int = 9,
    val blueWordsRemaining: Int = 8,
    val currentTeam: String = "RED",
    val isMasterPhase: Boolean = true,
    val currentClue: String? = null,
    val currentClueNumber: Int? = null,
    val winner: String? = null,
    val clues: List<Clue> = emptyList(),
    val currentGuardingWordCount: Int? = null,
    val guessesRemaining: Int? = null
) 