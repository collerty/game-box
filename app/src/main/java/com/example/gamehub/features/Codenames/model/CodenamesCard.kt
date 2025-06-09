package com.example.gamehub.features.codenames.model

data class CodenamesCard(
    val word: String,
    val color: CardColor,
    val isRevealed: Boolean = false
)

enum class CardColor {
    RED,
    BLUE,
    NEUTRAL,
    ASSASSIN
} 