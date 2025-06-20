package com.example.gamehub.features.codenames.service

import com.example.gamehub.features.codenames.model.CardColor
import com.example.gamehub.features.codenames.model.CodenamesCard
import kotlin.random.Random
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.FirebaseAuthException

class CodenamesService {
    companion object {
        private val WORD_LIST = listOf(
            "APPLE", "BANANA", "CAR", "DOOR", "ELEPHANT", "FISH", "GIRAFFE", "HOUSE",
            "IGLOO", "JELLY", "KANGAROO", "LION", "MONKEY", "NEST", "ORANGE", "PENGUIN",
            "QUEEN", "RABBIT", "SNAKE", "TIGER", "UMBRELLA", "VIOLIN", "WHALE", "X-RAY",
            "YACHT", "ZEBRA", "AIRPLANE", "BASKET", "CANDLE", "DOLPHIN", "EAGLE", "FLOWER",
            "GARDEN", "HAT", "ICE", "JUICE", "KITE", "LEMON", "MOUNTAIN", "NIGHT"
        )

        fun generateGameState(): Map<String, Any?> {
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
                "blueWordsRemaining" to blueCards,
                "currentTeam" to "RED",
                "isMasterPhase" to true,
                "currentClue" to null,
                "currentClueNumber" to null
            )
        }

        fun submitClue(
            roomId: String,
            clue: String,
            number: Int,
            team: String
        ) {
            val db = FirebaseFirestore.getInstance()
            db.collection("rooms").document(roomId)
                .update(
                    mapOf(
                        "gameState.codenames.currentClue" to clue,
                        "gameState.codenames.currentClueNumber" to number,
                        "gameState.codenames.isMasterPhase" to false
                    )
                )
        }

        fun revealCard(
            roomId: String,
            cardIndex: Int
        ) {
            val db = FirebaseFirestore.getInstance()
            db.collection("rooms").document(roomId)
                .get()
                .addOnSuccessListener { document ->
                    @Suppress("UNCHECKED_CAST")
                    val gameState = document.get("gameState.codenames") as? Map<String, Any>
                    val cards = gameState?.get("cards") as? List<Map<String, Any>> ?: return@addOnSuccessListener
                    
                    if (cardIndex >= cards.size) return@addOnSuccessListener
                    
                    val updatedCards = cards.toMutableList()
                    val card = updatedCards[cardIndex].toMutableMap()
                    card["isRevealed"] = true
                    updatedCards[cardIndex] = card
                    
                    val color = card["color"] as? String ?: "NEUTRAL"
                    val currentTeam = gameState["currentTeam"] as? String ?: "RED"
                    val isMasterPhase = gameState["isMasterPhase"] as? Boolean ?: true
                    
                    // Update game state based on revealed card
                    val updates = mutableMapOf<String, Any>()
                    updates["gameState.codenames.cards"] = updatedCards
                    
                    // If card is not the current team's color or is neutral, switch turns
                    if (color != currentTeam || color == "NEUTRAL" || color == "ASSASSIN") {
                        updates["gameState.codenames.currentTeam"] = if (currentTeam == "RED") "BLUE" else "RED"
                        updates["gameState.codenames.isMasterPhase"] = true
                    }
                    
                    // Update remaining words count
                    if (color == "RED") {
                        val redWordsRemaining = (gameState["redWordsRemaining"] as? Number)?.toInt() ?: 9
                        updates["gameState.codenames.redWordsRemaining"] = redWordsRemaining - 1
                    } else if (color == "BLUE") {
                        val blueWordsRemaining = (gameState["blueWordsRemaining"] as? Number)?.toInt() ?: 8
                        updates["gameState.codenames.blueWordsRemaining"] = blueWordsRemaining - 1
                    }
                    
                    db.collection("rooms").document(roomId).update(updates)
                }
        }
    }
} 