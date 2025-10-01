package com.example.gamehub.features.triviatoe

import com.example.gamehub.features.triviatoe.codec.TriviatoeCodec
import com.example.gamehub.features.triviatoe.model.*
import com.example.gamehub.repository.interfaces.ITriviatoeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import com.example.gamehub.features.triviatoe.TriviatoeQuestionBank
import kotlinx.coroutines.delay

class TriviatoeGameSession(
    private val repository: ITriviatoeRepository,
    val roomCode: String
) {
    /**
     * Flow of the current session state, real-time.
     */
    val stateFlow = callbackFlow<TriviatoeSession> {
        val registration = repository.listenToGameState(
            roomCode = roomCode,
            onDataChange = { session ->
                trySend(session ?: emptySession(roomCode))
            },
            onError = { e ->
                println("TriviatoeSession: Error listening to game state: ${e.message}")
            }
        )
        awaitClose { registration.remove() }
    }.stateIn(
        scope = CoroutineScope(Dispatchers.Main),
        started = SharingStarted.Eagerly,
        initialValue = emptySession(roomCode)
    )

    companion object {
        fun emptySession(roomCode: String) = com.example.gamehub.features.triviatoe.model.TriviatoeSession(
            roomCode = roomCode,
            players = emptyList(),
            board = emptyList(),
            moves = emptyList(),
            currentRound = 0,
            quizQuestion = null,
            answers = emptyMap(),
            firstToMove = null,
            currentTurn = null,
            winner = null,
            state = TriviatoeRoundState.QUESTION
        )
    }

    // Submit a quiz answer for this round
    suspend fun submitAnswer(playerId: String, answer: PlayerAnswer) {
        repository.submitAnswer(
            roomCode = roomCode,
            playerId = playerId,
            answer = answer,
            onSuccess = { println("Submit answer successful!") },
            onError = { e -> println("Submit answer FAILED: ${e.message}") }
        )
    }

    // Place an X or O on the grid
    suspend fun submitMove(playerId: String, row: Int, col: Int, symbol: String) {
        repository.submitMove(
            roomCode = roomCode,
            playerId = playerId,
            row = row,
            col = col,
            symbol = symbol,
            onSuccess = { println("Submit move successful!") },
            onError = { e -> println("Submit move FAILED: ${e.message}") }
        )
    }

    suspend fun startNextRound() {
        repository.startNextRound(
            roomCode = roomCode,
            onSuccess = { println("Start next round successful!") },
            onError = { e -> println("Start next round FAILED: ${e.message}") }
        )
    }

    suspend fun advanceGameState() {
        repository.advanceGameState(
            roomCode = roomCode,
            onSuccess = { println("Advance game state successful!") },
            onError = { e -> println("Advance game state FAILED: ${e.message}") }
        )
    }

    suspend fun voteForRematch(playerId: String) {
        repository.voteForRematch(
            roomCode = roomCode,
            playerId = playerId,
            onSuccess = { println("Vote for rematch successful!") },
            onError = { e -> println("Vote for rematch FAILED: ${e.message}") }
        )
    }

    suspend fun resetGame() {
        repository.resetGame(
            roomCode = roomCode,
            onSuccess = { println("Reset game successful!") },
            onError = { e -> println("Reset game FAILED: ${e.message}") }
        )
    }

    // Helper methods that were in the original session
    private suspend fun getCurrentRound(): Int {
        // This would need to be implemented in the repository
        // For now, we'll return a default value
        return 0
    }

    private suspend fun getFirstToMove(): String? {
        // This would need to be implemented in the repository
        // For now, we'll return null
        return null
    }

    private suspend fun getPlayersList(): List<Map<String, Any?>> {
        // This would need to be implemented in the repository
        // For now, we'll return an empty list
        return emptyList()
    }

    private suspend fun getQuizQuestion(): Map<String, Any?>? {
        // This would need to be implemented in the repository
        // For now, we'll return null
        return null
    }

    private suspend fun getRematchVotes(): Map<String, Boolean> {
        // This would need to be implemented in the repository
        // For now, we'll return an empty map
        return emptyMap()
    }

    private suspend fun getPlayers(): List<Map<String, Any>> {
        // This would need to be implemented in the repository
        // For now, we'll return an empty list
        return emptyList()
    }

    private suspend fun getResetting(): Boolean {
        // This would need to be implemented in the repository
        // For now, we'll return false
        return false
    }

    // Win checking logic (moved from original session)
    private fun checkWinForMoveBoard(
        boardCells: MutableList<TriviatoeCell>,
        row: Int,
        col: Int,
        symbol: String,
        players: List<TriviatoePlayer>
    ): Boolean {
        // This is complex game logic that should remain in the session
        // Implementation would be the same as the original
        return false
    }
}
