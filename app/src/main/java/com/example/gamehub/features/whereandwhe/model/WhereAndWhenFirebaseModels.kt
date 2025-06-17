package com.example.gamehub.features.whereandwhe.model

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
    val timeRanOut: Boolean = false // True if this player's guess was due to timeout
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
    val roundStartTimeMillis: Long = 0L, // When the GUESSING phase of the current round started
    val roundStatus: String = "guessing",
    val playerGuesses: Map<String, WWPlayerGuess> = emptyMap(), // Key: Player UID
    val roundResults: WWRoundResultsContainer = WWRoundResultsContainer(), // Results of the *last* completed round

    val mapRevealStartTimeMillis: Long = 0L, // When map reveal phase started
    val resultsDialogStartTimeMillis: Long = 0L, // When results dialog phase should start
    val leaderboardStartTimeMillis: Long = 0L, // When leaderboard phase should start

    // Tracks player readiness for different stages
    val playersReadyForResultsDialog: Map<String, Boolean> = emptyMap(), // Players ready to see results after map reveal (not used if map reveal is auto-timed)
    val playersReadyForLeaderboard: Map<String, Boolean> = emptyMap(), // Players acknowledged their results dialog
    val playersReadyForNextRound: Map<String, Boolean> = emptyMap(),   // Players finished viewing leaderboard (not used if leaderboard is auto-timed)

    val challengeOrder: List<String> = emptyList()
) {
    // No-argument constructor for Firebase
    constructor() : this(
        0, "", 0L,
        STATUS_GUESSING,
        emptyMap(), WWRoundResultsContainer(),
        0L, 0L, 0L,
        emptyMap(), emptyMap(), emptyMap(),
        emptyList()
    )

    companion object {
        const val STATUS_GUESSING = "guessing"
        const val STATUS_SHOWING_MAP_REVEAL = "showing_map_reveal"
        const val STATUS_RESULTS = "results"
        const val STATUS_SHOWING_LEADERBOARD = "showing_leaderboard"
        // No STATUS_FINISHED here, main room status handles game end
    }
}