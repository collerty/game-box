package com.example.gamehub.repository.interfaces

import androidx.compose.runtime.Composable
import com.example.gamehub.features.codenames.model.CodenamesGameState

interface ICodenamesRepository {
    fun getGameState(
        roomId: String,
        onSuccess: (CodenamesGameState?) -> Unit,
        onError: (Exception) -> Unit
    )

    fun updateGameState(
        roomId: String,
        gameState: CodenamesGameState,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    )

    fun listenToGameState(
        roomId: String,
        onDataChange: (CodenamesGameState?) -> Unit,
        onError: (Exception) -> Unit
    )
} 