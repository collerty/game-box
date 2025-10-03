package com.example.gamehub.repository.interfaces

import com.example.gamehub.features.whereandwhe.model.WWPlayerGuess
import com.example.gamehub.features.whereandwhe.model.WWRoundResultsContainer
import com.google.firebase.firestore.ListenerRegistration

interface IWhereAndWhenRepository {
    
    // Room listening
    fun listenToRoomState(
        roomCode: String,
        onDataChange: (Map<String, Any?>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration
    
    // Player operations
    fun submitPlayerGuess(
        roomCode: String,
        playerId: String,
        guess: WWPlayerGuess,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    )
    
    // Host operations
    fun updateGameState(
        roomCode: String,
        updates: Map<String, Any?>,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    )
    
    fun updatePlayerScores(
        roomCode: String,
        playerScores: Map<String, Any>,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    )
    
    fun transitionToMapReveal(
        roomCode: String,
        resultsContainer: WWRoundResultsContainer,
        updatedPlayers: List<Map<String, Any>>,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    )
    
    fun transitionToLeaderboard(
        roomCode: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    )
    
    fun transitionToNextRound(
        roomCode: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    )
    
    fun markPlayerReadyForLeaderboard(
        roomCode: String,
        playerId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    )
    
    // Cleanup
    fun deleteRoom(
        roomCode: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    )
}
