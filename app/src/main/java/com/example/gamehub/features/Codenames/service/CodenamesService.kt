package com.example.gamehub.features.codenames.service

import com.example.gamehub.features.codenames.model.CardColor
import com.example.gamehub.features.codenames.model.CodenamesCard
import com.example.gamehub.features.codenames.model.CodenamesGameState
import com.example.gamehub.features.codenames.model.Clue
import com.example.gamehub.repository.interfaces.ICodenamesRepository
import kotlin.random.Random

class CodenamesService(private val repository: ICodenamesRepository) {
    companion object {
        private val WORD_LIST = listOf(
            "APPLE", "BANANA", "CAR", "DOOR", "ELEPHANT", "FISH", "GIRAFFE", "HOUSE",
            "IGLOO", "JELLY", "KANGAROO", "LION", "MONKEY", "NEST", "ORANGE", "PENGUIN",
            "QUEEN", "RABBIT", "SNAKE", "TIGER", "UMBRELLA", "VIOLIN", "WHALE", "X-RAY",
            "YACHT", "ZEBRA", "AIRPLANE", "BASKET", "CANDLE", "DOLPHIN", "EAGLE", "FLOWER",
            "GARDEN", "HAT", "ICE", "JUICE", "KITE", "LEMON", "MOUNTAIN", "NIGHT"
        )

        fun generateGameState(): CodenamesGameState {
            val shuffledWords = WORD_LIST.shuffled().take(25)
            val redCards = 9
            val blueCards = 8
            val neutralCards = 7
            val assassinCards = 1
            val colors = mutableListOf<CardColor>().apply {
                repeat(redCards) { add(CardColor.RED) }
                repeat(blueCards) { add(CardColor.BLUE) }
                repeat(neutralCards) { add(CardColor.NEUTRAL) }
                repeat(assassinCards) { add(CardColor.ASSASSIN) }
            }.shuffled()
            val cards = shuffledWords.zip(colors).map { (word, color) ->
                CodenamesCard(word, color)
            }
            return CodenamesGameState(
                cards = cards,
                currentTurn = "RED",
                redWordsRemaining = redCards,
                blueWordsRemaining = blueCards,
                currentTeam = "RED",
                isMasterPhase = true,
                currentClue = null,
                currentClueNumber = null
            )
        }
    }

    fun getGameState(
        roomId: String,
        onSuccess: (CodenamesGameState?) -> Unit,
        onError: (Exception) -> Unit
    ) = repository.getGameState(roomId, onSuccess, onError)

    fun updateGameState(
        roomId: String,
        gameState: CodenamesGameState,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) = repository.updateGameState(roomId, gameState, onSuccess, onError)

    fun listenToGameState(
        roomId: String,
        onDataChange: (CodenamesGameState?) -> Unit,
        onError: (Exception) -> Unit
    ) = repository.listenToGameState(roomId, onDataChange, onError)

    fun submitClue(
        roomId: String,
        clue: Clue,
        clueNumber: Int,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        repository.getGameState(roomId, onSuccess = { state ->
            if (state == null) {
                onError(Exception("Game state not found"))
                return@getGameState
            }
            // Only allow clue if it's master phase and no winner
            if (!state.isMasterPhase || state.winner != null) {
                onError(Exception("Not master phase or game already ended"))
                return@getGameState
            }
            val updatedState = state.copy(
                currentClue = clue.word,
                currentClueNumber = clueNumber,
                isMasterPhase = false,
                guessesRemaining = clueNumber + 1, // Standard Codenames rule
                clues = state.clues + clue
            )
            repository.updateGameState(roomId, updatedState, onSuccess, onError)
        }, onError = onError)
    }

    fun makeGuess(
        roomId: String,
        cardIndex: Int,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        repository.getGameState(roomId, onSuccess = { state ->
            if (state == null) {
                onError(Exception("Game state not found"))
                return@getGameState
            }
            if (state.isMasterPhase || state.winner != null) {
                onError(Exception("Not guessing phase or game already ended"))
                return@getGameState
            }
            val cards = state.cards.toMutableList()
            val card = cards[cardIndex]
            if (card.isRevealed) {
                onError(Exception("Card already revealed"))
                return@getGameState
            }
            cards[cardIndex] = card.copy(isRevealed = true)
            var redWords = state.redWordsRemaining
            var blueWords = state.blueWordsRemaining
            var winner: String? = null
            var guessesLeft = (state.guessesRemaining ?: 0) - 1
            var nextPhase = false
            // Handle guess logic
            when (card.color) {
                com.example.gamehub.features.codenames.model.CardColor.RED -> {
                    if (state.currentTeam == "RED") {
                        redWords--
                        if (redWords == 0) winner = "RED"
                    } else {
                        nextPhase = true // Wrong team guessed
                    }
                }
                com.example.gamehub.features.codenames.model.CardColor.BLUE -> {
                    if (state.currentTeam == "BLUE") {
                        blueWords--
                        if (blueWords == 0) winner = "BLUE"
                    } else {
                        nextPhase = true // Wrong team guessed
                    }
                }
                com.example.gamehub.features.codenames.model.CardColor.ASSASSIN -> {
                    winner = if (state.currentTeam == "RED") "BLUE" else "RED"
                }
                com.example.gamehub.features.codenames.model.CardColor.NEUTRAL -> {
                    nextPhase = true
                }
            }
            // End guessing phase if out of guesses or wrong/neutral/assassin
            if (guessesLeft <= 0 || nextPhase || winner != null) {
                val nextTeam = if (state.currentTeam == "RED") "BLUE" else "RED"
                val updatedState = state.copy(
                    cards = cards,
                    redWordsRemaining = redWords,
                    blueWordsRemaining = blueWords,
                    winner = winner,
                    isMasterPhase = true,
                    currentTeam = nextTeam,
                    currentTurn = nextTeam,
                    currentClue = null,
                    currentClueNumber = null,
                    guessesRemaining = null
                )
                repository.updateGameState(roomId, updatedState, onSuccess, onError)
            } else {
                // Continue guessing
                val updatedState = state.copy(
                    cards = cards,
                    redWordsRemaining = redWords,
                    blueWordsRemaining = blueWords,
                    winner = winner,
                    guessesRemaining = guessesLeft
                )
                repository.updateGameState(roomId, updatedState, onSuccess, onError)
            }
        }, onError = onError)
    }

    fun endGuessingPhase(
        roomId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        repository.getGameState(roomId, onSuccess = { state ->
            if (state == null) {
                onError(Exception("Game state not found"))
                return@getGameState
            }
            if (state.isMasterPhase || state.winner != null) {
                onError(Exception("Not guessing phase or game already ended"))
                return@getGameState
            }
            val nextTeam = if (state.currentTeam == "RED") "BLUE" else "RED"
            val updatedState = state.copy(
                isMasterPhase = true,
                currentTeam = nextTeam,
                currentTurn = nextTeam,
                currentClue = null,
                currentClueNumber = null,
                guessesRemaining = null
            )
            repository.updateGameState(roomId, updatedState, onSuccess, onError)
        }, onError = onError)
    }

    // Add business logic methods as needed, using the strongly-typed model
} 