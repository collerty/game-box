package com.example.gamehub.features.whereandwhen.model

// Represents a player's guess for a round in Firestore
data class WWPlayerGuess(
    val year: Int? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val submitted: Boolean = false,
    val timeTakenMs: Long? = null // Optional
) {
    // No-argument constructor for Firebase
    constructor() : this(null, null, null, false, null)
}

// Represents a single player's result for a round in Firestore
data class WWPlayerRoundResult(
    val guessedYear: Int = 0,
    val yearScore: Int = 0,
    val guessedLat: Double? = null,
    val guessedLng: Double? = null,
    val distanceKm: Double? = null,
    val locationScore: Int = 0,
    val roundScore: Int = 0,
    val timeRanOut: Boolean = false
) {
    // No-argument constructor for Firebase
    constructor() : this(0, 0, null, null, null, 0, 0, false)
}

// Represents the results for all players for a single completed round
data class WWRoundResultsContainer(
    val challengeId: String = "",
    val results: Map<String, WWPlayerRoundResult> = emptyMap() // Key: Player UID
) {
    // No-argument constructor for Firebase
    constructor() : this("", emptyMap())
}

// The main game state for "Where & When" stored under gameState.whereandwhen
data class WhereAndWhenGameState(
    val currentRoundIndex: Int = 0,
    val currentChallengeId: String = "",
    val roundStartTimeMillis: Long = 0L,
    val roundStatus: String = "guessing", // "guessing", "results"
    val playerGuesses: Map<String, WWPlayerGuess> = emptyMap(), // Key: Player UID
    val roundResults: WWRoundResultsContainer = WWRoundResultsContainer(), // Results of the *last* completed round
    val playersReadyForNextRound: Map<String, Boolean> = emptyMap(), // Key: Player UID
    val challengeOrder: List<String> = emptyList()
) {
    // No-argument constructor for Firebase
    constructor() : this(0, "", 0L, "guessing", emptyMap(), WWRoundResultsContainer(), emptyMap(), emptyList())

    companion object {
        const val STATUS_GUESSING = "guessing"
        const val STATUS_RESULTS = "results"
    }
}