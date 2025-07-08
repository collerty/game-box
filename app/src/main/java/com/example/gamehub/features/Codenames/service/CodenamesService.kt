package com.example.gamehub.features.codenames.service

import com.example.gamehub.features.codenames.model.CardColor
import com.example.gamehub.features.codenames.model.CodenamesCard
import com.example.gamehub.features.codenames.model.CodenamesGameState
import com.example.gamehub.features.codenames.model.Clue
import com.example.gamehub.repository.ICodenamesRepository
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

    // Add business logic methods as needed, using the strongly-typed model
} 