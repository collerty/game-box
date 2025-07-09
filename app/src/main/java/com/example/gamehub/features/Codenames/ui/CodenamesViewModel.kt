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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

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

    private val _currentPlayerRole = MutableStateFlow<String?>(null)
    val currentPlayerRole: StateFlow<String?> = _currentPlayerRole
    private val _currentPlayerTeam = MutableStateFlow<String?>(null)
    val currentPlayerTeam: StateFlow<String?> = _currentPlayerTeam
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    var redMasterClue by mutableStateOf("")
    var blueMasterClue by mutableStateOf("")

    private var timerJob: Job? = null
    private var playerListener: com.google.firebase.firestore.ListenerRegistration? = null

    private val codenamesService = com.example.gamehub.features.codenames.service.CodenamesService(repository)

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

                // Real-time listener for current player's role and team
                val uid = auth.currentUser?.uid
                android.util.Log.d("CodenamesDebug", "Current Firebase UID: $uid")
                playerListener?.remove() // Remove previous listener if any
                if (uid != null) {
                    playerListener = db.collection("rooms").document(roomId)
                        .addSnapshotListener { doc, e ->
                            if (e != null) {
                                android.util.Log.e("CodenamesDebug", "Player snapshot error", e)
                                return@addSnapshotListener
                            }
                            if (doc != null && doc.exists()) {
                                @Suppress("UNCHECKED_CAST")
                                val players = doc.get("players") as? List<Map<String, Any>> ?: emptyList()
                                val currentPlayer = players.find { it["uid"] == uid }
                                android.util.Log.d("CodenamesDebug", "Fetched player: $currentPlayer from players: $players")
                                _currentPlayerRole.value = currentPlayer?.get("role") as? String
                                _currentPlayerTeam.value = currentPlayer?.get("team") as? String
                            } else {
                                android.util.Log.d("CodenamesDebug", "Room doc missing or null")
                                _currentPlayerRole.value = null
                                _currentPlayerTeam.value = null
                            }
                        }
                } else {
                    _currentPlayerRole.value = null
                    _currentPlayerTeam.value = null
                }
            },
            onError = { e -> android.util.Log.e("CodenamesDebug", "Game state error", e) }
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

    fun submitClue(roomId: String, clue: com.example.gamehub.features.codenames.model.Clue, clueNumber: Int) {
        viewModelScope.launch {
            codenamesService.submitClue(
                roomId,
                clue,
                clueNumber,
                onSuccess = { /* handle success */ },
                onError = { /* handle error */ }
            )
        }
    }

    fun makeGuess(roomId: String, cardIndex: Int) {
        viewModelScope.launch {
            codenamesService.makeGuess(
                roomId,
                cardIndex,
                onSuccess = { /* handle success */ },
                onError = { /* handle error */ }
            )
        }
    }

    fun endGuessingPhase(roomId: String) {
        viewModelScope.launch {
            codenamesService.endGuessingPhase(
                roomId,
                onSuccess = { /* handle success */ },
                onError = { /* handle error */ }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerListener?.remove()
    }

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