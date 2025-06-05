package com.example.gamehub.features.triviatoe.model

sealed class TriviatoeQuestion {
    // Standard multiple choice
    data class MultipleChoice(
        val question: String,
        val answers: List<String>, // four options
        val correctIndex: Int
    ) : TriviatoeQuestion()

    // Example: date input type (closest wins)
    data class DateInput(
        val question: String,
        val correctMillis: Long // Could be epoch millis, or just year, etc
    ) : TriviatoeQuestion()

    // Future: Add new types here!
    // data class NumberInput(...)
}
