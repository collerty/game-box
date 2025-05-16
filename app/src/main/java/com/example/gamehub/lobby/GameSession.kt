package com.example.gamehub.lobby

import kotlinx.coroutines.flow.Flow

interface GameSession<MOVE, STATE> {
    val stateFlow: Flow<STATE>
    suspend fun sendMove(move: MOVE)
    suspend fun close()
}
