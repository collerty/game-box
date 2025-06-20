package com.example.gamehub.features.ohpardon

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gamehub.features.ohpardon.data.OhPardonDatabase
import com.example.gamehub.features.ohpardon.logic.OhPardonGameLogic
import com.example.gamehub.features.ohpardon.models.GameRoom
import com.example.gamehub.features.ohpardon.models.Player
import com.example.gamehub.features.ohpardon.models.UiEvent
import com.example.gamehub.features.ohpardon.ui.BoardCell
import com.example.gamehub.features.ohpardon.ui.mappers.BoardMapper
import com.example.gamehub.features.ohpardon.util.ShakeDetector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OhPardonViewModel(
    application: Application,
    private val roomCode: String,
    private val currentUserName: String
) : AndroidViewModel(application) {

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val _gameRoom = MutableStateFlow<GameRoom?>(null)
    val gameRoom: StateFlow<GameRoom?> = _gameRoom.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val repository = OhPardonDatabase(roomCode)
    private val gameLogic = OhPardonGameLogic()
    private val boardMapper = BoardMapper()
    private val shakeDetector = ShakeDetector(application) { attemptRollDice(currentUserName) }

    init {
        listenToRoomChanges()
    }

    private fun listenToRoomChanges() {
        repository.listenToRoomChanges { gameRoom ->
            _gameRoom.value = gameRoom
        }
    }

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    fun registerShakeListener() {
        shakeDetector.register()
    }

    fun unregisterShakeListener() {
        shakeDetector.unregister()
    }

    fun attemptRollDice(currentUserName: String) {
        val currentGame = _gameRoom.value ?: return
        val currentPlayer = currentGame.players.find { it.name == currentUserName }

        if (!gameLogic.canRollDice(currentGame, currentPlayer?.uid)) {
            return
        }

        val diceRoll = rollDice()
        repository.updateDiceRoll(diceRoll)
    }

    private fun rollDice(): Int {
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.PlayDiceRollSound)
            _uiEvent.emit(UiEvent.Vibrate)
        }
        return (1..6).random()
    }

    fun skipTurn(currentUserName: String) {
        val currentGame = _gameRoom.value ?: return
        val currentPlayer = currentGame.players.find { it.name == currentUserName }

        if (currentPlayer?.uid == currentGame.gameState.currentTurnUid) {
            val nextPlayerUid = gameLogic.getNextPlayerUid(currentGame, currentPlayer.uid)
            repository.skipTurn(nextPlayerUid)
        } else {
            _toastMessage.value = "Not allowed to skip another player's turn!"
        }
    }

    fun attemptMovePawn(currentUserUid: String, pawnId: String) {
        val currentGame = _gameRoom.value ?: return
        
        val moveResult = gameLogic.calculateMove(currentGame, currentUserUid, pawnId)
        
        if (!moveResult.isValid) {
            _toastMessage.value = moveResult.message
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.PlayIllegalMoveSound)
                _uiEvent.emit(UiEvent.Vibrate)
            }
            return
        }
        
        if (moveResult.isCapture) {
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.PlayCaptureSound)
                _uiEvent.emit(UiEvent.Vibrate)
            }
        } else {
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.PlayMoveSound)
                _uiEvent.emit(UiEvent.Vibrate)
            }
        }
        
        repository.updateGameState(moveResult.updatedPlayers, moveResult.nextPlayerUid, moveResult.isGameOver)
        
        if (moveResult.isGameOver) {
            repository.endGame(moveResult.winner?.name ?: "")
        }
    }

    fun getBoardForUI(players: List<Player>): List<List<BoardCell>> {
        return boardMapper.createBoard(players)
    }

    override fun onCleared() {
        super.onCleared()
        repository.removeListeners()
    }
}
