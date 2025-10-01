package com.example.gamehub.repository.implementations

import com.example.gamehub.features.triviatoe.model.TriviatoeSession
import com.example.gamehub.features.triviatoe.model.PlayerAnswer
import com.example.gamehub.features.triviatoe.codec.TriviatoeCodec
import com.example.gamehub.repository.interfaces.ITriviatoeRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

class TriviatoeRepository : BaseRepository("rooms"), ITriviatoeRepository {

    override fun getGameState(
        roomCode: String,
        onSuccess: (TriviatoeSession?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        collection.document(roomCode).get()
            .addOnSuccessListener { doc ->
                val gs = doc.get("gameState") as? Map<*, *>
                val triviatoeState = (gs?.get("triviatoe") as? Map<*, *>)?.mapKeys { it.key as String }
                onSuccess(triviatoeState?.let { TriviatoeCodec.decodeState(it as Map<String, Any?>) })
            }
            .addOnFailureListener(onError)
    }

    override fun updateGameState(
        roomCode: String,
        updates: Map<String, Any?>,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val firestoreUpdates = updates.mapKeys { "gameState.triviatoe.${it.key}" }
        collection.document(roomCode).update(firestoreUpdates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener(onError)
    }

    override fun listenToGameState(
        roomCode: String,
        onDataChange: (TriviatoeSession?) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return collection.document(roomCode)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    onError(e)
                } else {
                    val gs = snapshot?.get("gameState") as? Map<*, *>
                    val triviatoeState = (gs?.get("triviatoe") as? Map<*, *>)?.mapKeys { it.key as String }
                    onDataChange(triviatoeState?.let { TriviatoeCodec.decodeState(it as Map<String, Any?>) })
                }
            }
    }

    override fun submitAnswer(
        roomCode: String,
        playerId: String,
        answer: PlayerAnswer,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val updates = mapOf(
            "answers.$playerId" to mapOf(
                "answerIndex" to answer.answerIndex,
                "timestamp" to FieldValue.serverTimestamp()
            )
        )
        updateGameState(roomCode, updates, onSuccess, onError)
    }

    override fun submitMove(
        roomCode: String,
        playerId: String,
        row: Int,
        col: Int,
        symbol: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        // Get current game state first
        collection.document(roomCode).get()
            .addOnSuccessListener { doc ->
                val gs = doc.get("gameState") as? Map<*, *>
                val triviatoeState = (gs?.get("triviatoe") as? Map<*, *>)?.mapKeys { it.key as String }
                val session = triviatoeState?.let { TriviatoeCodec.decodeState(it as Map<String, Any?>) }
                
                if (session == null) {
                    onError(Exception("Game state not found"))
                    return@addOnSuccessListener
                }

                // Validate move
                if (session.state != com.example.gamehub.features.triviatoe.model.TriviatoeRoundState.MOVE_1 && 
                    session.state != com.example.gamehub.features.triviatoe.model.TriviatoeRoundState.MOVE_2) {
                    onError(Exception("Not in a move state"))
                    return@addOnSuccessListener
                }
                if (session.currentTurn != playerId) {
                    onError(Exception("Not this player's turn"))
                    return@addOnSuccessListener
                }
                if (session.moves.any { it.playerId == playerId && it.round == session.currentRound }) {
                    onError(Exception("Player already moved this round"))
                    return@addOnSuccessListener
                }

                val move = mapOf(
                    "playerId" to playerId,
                    "row" to row,
                    "col" to col,
                    "symbol" to symbol,
                    "round" to session.currentRound
                )

                val updates = mapOf(
                    "moves" to FieldValue.arrayUnion(move),
                    "board" to FieldValue.arrayUnion(mapOf("row" to row, "col" to col, "symbol" to symbol)),
                    "lastMoveTimestamp" to FieldValue.serverTimestamp()
                )

                updateGameState(roomCode, updates, onSuccess, onError)
            }
            .addOnFailureListener(onError)
    }

    override fun startNextRound(
        roomCode: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        // This is a complex operation that needs to be handled in the session class
        // For now, we'll just call the update method
        updateGameState(roomCode, mapOf("state" to "QUESTION"), onSuccess, onError)
    }

    override fun advanceGameState(
        roomCode: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        updateGameState(roomCode, mapOf("state" to "MOVE_1"), onSuccess, onError)
    }

    override fun voteForRematch(
        roomCode: String,
        playerId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val updates = mapOf(
            "rematchVotes.$playerId" to true
        )
        updateGameState(roomCode, updates, onSuccess, onError)
    }

    override fun resetGame(
        roomCode: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val updates = mapOf(
            "state" to "QUESTION",
            "currentRound" to 0,
            "quizQuestion" to null,
            "answers" to emptyMap<String, Any>(),
            "moves" to emptyList<Any>(),
            "board" to emptyList<Any>(),
            "winner" to null,
            "rematchVotes" to emptyMap<String, Any>(),
            "resetting" to true
        )
        updateGameState(roomCode, updates, onSuccess, onError)
    }
}
