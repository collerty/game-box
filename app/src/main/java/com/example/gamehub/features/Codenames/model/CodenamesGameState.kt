package com.example.gamehub.features.codenames.model

import com.example.gamehub.features.codenames.model.CodenamesConstants

data class CodenamesGameState(
    val cards: List<CodenamesCard> = emptyList(),
    val currentTurn: String = "RED",
    val redWordsRemaining: Int = CodenamesConstants.DEFAULT_RED_WORDS,
    val blueWordsRemaining: Int = CodenamesConstants.DEFAULT_BLUE_WORDS,
    val currentTeam: String = "RED",
    val isMasterPhase: Boolean = true,
    val currentClue: String? = null,
    val currentClueNumber: Int? = null,
    val winner: String? = null,
    val clues: List<Clue> = emptyList(),
    val currentGuardingWordCount: Int? = null,
    val guessesRemaining: Int? = null
) 