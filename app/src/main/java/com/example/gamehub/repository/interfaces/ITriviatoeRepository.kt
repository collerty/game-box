package com.example.gamehub.repository.interfaces

import com.example.gamehub.features.triviatoe.model.TriviatoeSession
import com.example.gamehub.features.triviatoe.model.PlayerAnswer
import com.google.firebase.firestore.ListenerRegistration

interface ITriviatoeRepository {
    fun getGameState(
        roomCode: String,
        onSuccess: (TriviatoeSession?) -> Unit,
        onError: (Exception) -> Unit
    )

    fun updateGameState(
        roomCode: String,
        updates: Map<String, Any?>,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    )

    fun listenToGameState(
        roomCode: String,
        onDataChange: (TriviatoeSession?) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration

    fun submitAnswer(
        roomCode: String,
        playerId: String,
        answer: PlayerAnswer,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    )

    fun submitMove(
        roomCode: String,
        playerId: String,
        row: Int,
        col: Int,
        symbol: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    )

    fun startNextRound(
        roomCode: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    )

    fun advanceGameState(
        roomCode: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    )

    fun voteForRematch(
        roomCode: String,
        playerId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    )

    fun resetGame(
        roomCode: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    )
}
