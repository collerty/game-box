package com.example.gamehub.repository.interfaces

import com.example.gamehub.features.battleships.model.BattleshipsGameRoom
import com.example.gamehub.features.battleships.model.Cell
import com.example.gamehub.features.battleships.model.Ship
import com.example.gamehub.lobby.model.Move
import kotlinx.coroutines.flow.StateFlow
import com.example.gamehub.lobby.model.PowerUp // Assuming this is needed for power-up logic

interface IBattleShipsRepository {
    // Flows for observing game state
    val gameRoom: StateFlow<BattleshipsGameRoom?>
    val rematchVotes: StateFlow<Map<String, Boolean>> // NEW: To replace direct listener on PlayScreen

    // Lifecycle/Room Management
    fun joinRoom(roomCode: String)
    fun leaveRoom()
    fun deleteRoom(roomCode: String, onSuccess: () -> Unit, onError: (Exception) -> Unit)

    // Game Actions (Core)
    fun submitMove(x: Int, y: Int, playerId: String)
    fun setPlayerReady(playerId: String, ships: List<Ship>)
    fun surrender(playerId: String)

    // Game Actions (Animation/Turn Management)
    fun updateCurrentAttack(x: Int, y: Int, playerId: String, startedAt: Long)
    fun clearCurrentAttack()

    // Game Actions (Power-ups/Energy)
    suspend fun updateEnergy(playerId: String, amount: Int) // amount is the delta (positive or negative)
    suspend fun placeMine(playerId: String, newMine: Cell) // For PowerUp.Mine

    // Game Actions (Post-game/Lobby)
    suspend fun voteForRematch(playerId: String)
    suspend fun resetGame(roomCode: String) // For complex rematch reset
    suspend fun endGame(roomCode: String) // For exiting/ending the room
}