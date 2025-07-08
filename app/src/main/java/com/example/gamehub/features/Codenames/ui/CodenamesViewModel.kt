package com.example.gamehub.features.codenames.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gamehub.features.codenames.model.CodenamesGameState
import com.example.gamehub.features.codenames.model.Clue
import com.example.gamehub.repository.interfaces.ICodenamesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class CodenamesViewModel(
    private val repository: ICodenamesRepository
) : ViewModel() {
    private val _gameState = MutableStateFlow<CodenamesGameState?>(null)
    val gameState: StateFlow<CodenamesGameState?> = _gameState

    private val _currentTurn = MutableStateFlow("RED")
    val currentTurn: StateFlow<String> = _currentTurn

    private val _redWordsRemaining = MutableStateFlow(9)
    val redWordsRemaining: StateFlow<Int> = _redWordsRemaining

    private val _blueWordsRemaining = MutableStateFlow(8)
    val blueWordsRemaining: StateFlow<Int> = _blueWordsRemaining

    private val _winner = MutableStateFlow<String?>(null)
    val winner: StateFlow<String?> = _winner

    private val _isMasterPhase = MutableStateFlow(true)
    val isMasterPhase: StateFlow<Boolean> = _isMasterPhase

    private val _currentTeam = MutableStateFlow("RED")
    val currentTeam: StateFlow<String> = _currentTeam

    private val _redClues = MutableStateFlow<List<Clue>>(emptyList())
    val redClues: StateFlow<List<Clue>> = _redClues

    private val _blueClues = MutableStateFlow<List<Clue>>(emptyList())
    val blueClues: StateFlow<List<Clue>> = _blueClues

    private val _timerSeconds = MutableStateFlow(0)
    val timerSeconds: StateFlow<Int> = _timerSeconds

    private var timerJob: Job? = null

    fun startListening(roomId: String) {
        repository.listenToGameState(
            roomId,
            onDataChange = { state ->
                _gameState.value = state
                _currentTurn.value = state?.currentTurn ?: "RED"
                _redWordsRemaining.value = state?.redWordsRemaining ?: 9
                _blueWordsRemaining.value = state?.blueWordsRemaining ?: 8
                _currentTeam.value = state?.currentTeam ?: "RED"
                _isMasterPhase.value = state?.isMasterPhase ?: true
                _winner.value = state?.winner
                val clues = state?.clues ?: emptyList()
                _redClues.value = clues.filter { it.team == "RED" }
                _blueClues.value = clues.filter { it.team == "BLUE" }
            },
            onError = { /* handle error, e.g. log or show message */ }
        )
    }

    fun startTimer(isMasterPhase: Boolean, currentTeam: String, roomId: String) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            _timerSeconds.value = 60
            while (_timerSeconds.value > 0 && _winner.value == null) {
                delay(1000)
                _timerSeconds.value = _timerSeconds.value - 1
            }
            if (_winner.value == null) {
                if (!isMasterPhase) {
                    val nextTeam = if (currentTeam == "RED") "BLUE" else "RED"
                    repository.updateGameState(
                        roomId,
                        CodenamesGameState(
                            currentTurn = nextTeam,
                            currentTeam = nextTeam,
                            isMasterPhase = true,
                            redWordsRemaining = _redWordsRemaining.value,
                            blueWordsRemaining = _blueWordsRemaining.value,
                            winner = _winner.value,
                            clues = _redClues.value + _blueClues.value,
                            cards = _gameState.value?.cards ?: emptyList(),
                            currentGuardingWordCount = 0,
                            guessesRemaining = 0
                        ),
                        onSuccess = {},
                        onError = { }
                    )
                } else {
                    repository.updateGameState(
                        roomId,
                        CodenamesGameState(
                            currentTurn = _currentTurn.value,
                            currentTeam = _currentTeam.value,
                            isMasterPhase = false,
                            redWordsRemaining = _redWordsRemaining.value,
                            blueWordsRemaining = _blueWordsRemaining.value,
                            winner = _winner.value,
                            clues = _redClues.value + _blueClues.value,
                            cards = _gameState.value?.cards ?: emptyList(),
                            currentGuardingWordCount = 0,
                            guessesRemaining = 0
                        ),
                        onSuccess = {},
                        onError = { }
                    )
                }
            }
        }
    }

    fun submitClue(roomId: String, newState: CodenamesGameState) {
        viewModelScope.launch {
            repository.updateGameState(
                roomId,
                newState,
                onSuccess = { /* handle success, e.g. show toast */ },
                onError = { /* handle error */ }
            )
        }
    }

    // Add more methods for card click, etc.
}

class CodenamesViewModelFactory(
    private val repository: ICodenamesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CodenamesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CodenamesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 