package com.example.gamehub.features.codenames.service

import com.example.gamehub.features.codenames.model.CardColor
import com.example.gamehub.features.codenames.model.CodenamesCard
import kotlin.random.Random

class CodenamesService {
    companion object {
        private val WORD_LIST = listOf(
            "APPLE", "BANANA", "CAR", "DOOR", "ELEPHANT", "FISH", "GIRAFFE", "HOUSE",
            "IGLOO", "JELLY", "KANGAROO", "LION", "MONKEY", "NEST", "ORANGE", "PENGUIN",
            "QUEEN", "RABBIT", "SNAKE", "TIGER", "UMBRELLA", "VIOLIN", "WHALE", "X-RAY",
            "YACHT", "ZEBRA", "AIRPLANE", "BASKET", "CANDLE", "DOLPHIN", "EAGLE", "FLOWER",
            "GARDEN", "HAT", "ICE", "JUICE", "KITE", "LEMON", "MOUNTAIN", "NIGHT"
        )

        fun generateGameState(): Map<String, Any> {
            // Shuffle the word list and take 25 words
            val shuffledWords = WORD_LIST.shuffled().take(25)
            
            // Determine the number of cards for each color
            // First team gets 9 cards, second team gets 8, 7 neutral, 1 assassin
            val redCards = 9
            val blueCards = 8
            val neutralCards = 7
            val assassinCards = 1

            // Create a list of colors in the correct order
            val colors = mutableListOf<CardColor>().apply {
                repeat(redCards) { add(CardColor.RED) }
                repeat(blueCards) { add(CardColor.BLUE) }
                repeat(neutralCards) { add(CardColor.NEUTRAL) }
                repeat(assassinCards) { add(CardColor.ASSASSIN) }
            }.shuffled()

            // Create the cards
            val cards = shuffledWords.zip(colors).map { (word, color) ->
                CodenamesCard(word, color)
            }

            // Convert cards to a format suitable for Firebase
            val cardsForFirebase = cards.map { card ->
                mapOf(
                    "word" to card.word,
                    "color" to card.color.name,
                    "isRevealed" to card.isRevealed
                )
            }

            return mapOf(
                "cards" to cardsForFirebase,
                "currentTurn" to "RED", // Red team starts
                "redWordsRemaining" to redCards,
                "blueWordsRemaining" to blueCards
            )
        }
    }
} 