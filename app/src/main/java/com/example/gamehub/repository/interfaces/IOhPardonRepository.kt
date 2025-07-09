package com.example.gamehub.repository.interfaces

import androidx.compose.ui.graphics.Color
import com.example.gamehub.features.ohpardon.models.GameRoom
import com.example.gamehub.features.ohpardon.models.Player
import kotlinx.coroutines.flow.StateFlow

interface IOhPardonRepository {
    val gameRoom: StateFlow<GameRoom?>

    fun joinRoom(roomCode: String)
    fun updateDiceRoll(diceRoll: Int)
    fun skipTurn(nextPlayerUid: String)
    fun updateGameState(updatedPlayers: List<Player>, nextPlayerUid: String, isGameOver: Boolean)
    fun endGame(winnerName: String)
    fun parseColor(colorStr: String): Color
    fun leaveRoom()
}

