package com.example.gamehub.features.triviatoe.model

data class TriviatoeSession(
    val roomCode: String,
    val players: List<TriviatoePlayer>,
    val board: List<TriviatoeCell>,
    val moves: List<TriviatoeMove>,
    val currentRound: Int,
    val quizQuestion: TriviatoeQuestion?,
    val answers: Map<String, PlayerAnswer?>, // key: playerId
    val firstToMove: String?,   // playerId who moves first this round
    val currentTurn: String?,   // playerId whose move it is now
    val winner: String?,        // playerId or null
    val state: TriviatoeRoundState,
    val randomized: Boolean? = null,
    val lastQuestion: TriviatoeQuestion? = null,
    val lastCorrectIndex: Int? = null,
    val rematchVotes: Map<String, Boolean> = emptyMap(),
    val readyForQuestion: Map<String, Boolean>? = null
)

data class TriviatoePlayer(
    val uid: String,
    val name: String,
    val symbol: String // "X" or "O"
)

data class TriviatoeCell(
    val row: Int,
    val col: Int,
    val symbol: String? // "X", "O", or null
)

data class TriviatoeMove(
    val playerId: String,
    val row: Int,
    val col: Int,
    val symbol: String,
    val round: Int
)

sealed class TriviatoeAnswer {
    data class MultipleChoice(val answerIndex: Int) : TriviatoeAnswer()
    data class DateInput(val millis: Long) : TriviatoeAnswer()
    // More answer types as needed
}

data class PlayerAnswer(
    val answerIndex: Int,
    val timestamp: Long? // in millis
)

enum class TriviatoeRoundState {
    QUESTION,    // Waiting for answers
    REVEAL,      // Show result/first-to-move
    MOVE_1,      // First player move
    MOVE_2,      // Second player move
    CHECK_WIN,   // Check win state
    FINISHED,
    XO_ASSIGN,
    WAITING_FOR_READY
}
