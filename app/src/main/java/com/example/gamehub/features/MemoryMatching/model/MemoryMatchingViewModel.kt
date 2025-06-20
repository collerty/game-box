package com.example.gamehub.features.MemoryMatching.model

import android.app.Application
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gamehub.R
import com.example.gamehub.audio.SoundManager
import com.example.gamehub.features.MemoryMatching.logic.GameLogic
import com.example.gamehub.features.MemoryMatching.logic.GameTimer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MemoryMatchingViewModel(application: Application) : AndroidViewModel(application) {

    private val _gameState = MutableStateFlow(MemoryMatchingState())
    val gameState: StateFlow<MemoryMatchingState> = _gameState.asStateFlow()

    private val gameLogic = GameLogic()
    private val timer = GameTimer(viewModelScope)

    val gameDifficulties: List<GameDifficulty> = listOf(
        GameDifficulty(pairs = 6, columns = 3, displayName = "Easy (3x4 - 12 Cards)", cardBackResId = R.drawable.old_card_back, timeLimitSeconds = 50, maxAttempts = 4),
        GameDifficulty(pairs = 8, columns = 4, displayName = "Medium (4x4 - 16 Cards)", cardBackResId = R.drawable.card_back_red, timeLimitSeconds = 60, maxAttempts = 5),
        GameDifficulty(pairs = 10, columns = 4, displayName = "Hard (4x5 - 20 Cards)", cardBackResId = R.drawable.cards_back_blue, timeLimitSeconds = 75, maxAttempts = 6),
        GameDifficulty(pairs = 12, columns = 4, displayName = "Expert (4x6 - 24 Cards)", cardBackResId = R.drawable.back_card_brown, timeLimitSeconds = 90, maxAttempts = 7)
    )

    init {
        _gameState.update { it.copy(timeLeftInSeconds = gameDifficulties.first().timeLimitSeconds) }
        observeTimer()
    }

    private fun observeTimer() {
        viewModelScope.launch {
            timer.timeLeftInSeconds.collect { timeLeft ->
                _gameState.update { it.copy(timeLeftInSeconds = timeLeft) }
                if (timeLeft == 0) {
                    handleTimeUp()
                }
            }
        }
    }

    private fun handleTimeUp() {
        val currentState = _gameState.value
        if (currentState.isTimerRunning && !currentState.allPairsMatched && !currentState.showLoseScreen) {
            _gameState.update {
                it.copy(
                    isTimerRunning = false,
                    loseReason = "Time's Up!",
                    showLoseScreen = true
                )
            }
            SoundManager.playEffect(getApplication(), R.raw.matching_gamelose_sound)
        }
    }

    fun startGame(difficulty: GameDifficulty) {
        timer.stop()
        val newCards = gameLogic.generateCardsForDifficulty(difficulty).toMutableStateList()
        _gameState.update {
            it.copy(
                currentDifficulty = difficulty,
                cards = newCards,
                flippedCardIndices = emptyList(),
                attemptCount = 0,
                currentTurnIncorrectAttempts = 0,
                allPairsMatched = false,
                processingMatch = false,
                timeLeftInSeconds = difficulty.timeLimitSeconds,
                isTimerRunning = false,
                showLoseScreen = false,
                loseReason = null,
                currentScreen = GameScreen.PLAYING
            )
        }
    }

    fun restartGame() {
        _gameState.value.currentDifficulty?.let { difficulty ->
            startGame(difficulty)
        } ?: run {
            _gameState.update { it.copy(currentScreen = GameScreen.DIFFICULTY_SELECTION) }
        }
    }

    fun selectDifficultyScreen() {
        timer.stop()
        _gameState.update { it.copy(currentScreen = GameScreen.DIFFICULTY_SELECTION, isTimerRunning = false, allPairsMatched = false, showLoseScreen = false) }
    }

    fun onCardClicked(cardIndex: Int) {
        val currentState = _gameState.value
        val card = currentState.cards.getOrNull(cardIndex)

        if (card == null || card.isFlipped || card.isMatched || currentState.flippedCardIndices.size >= 2 || currentState.processingMatch || currentState.allPairsMatched || currentState.showLoseScreen) {
            return
        }

        if (!currentState.isTimerRunning) {
            currentState.currentDifficulty?.let { timer.start(it.timeLimitSeconds) }
        }

        SoundManager.playEffect(getApplication(), R.raw.card_flip)

        val newCards = currentState.cards.toMutableStateList()
        newCards[cardIndex] = card.copy(isFlipped = true)
        val newFlippedIndices = currentState.flippedCardIndices + cardIndex

        _gameState.update {
            it.copy(
                cards = newCards,
                flippedCardIndices = newFlippedIndices,
                isTimerRunning = true
            )
        }

        if (newFlippedIndices.size == 2) {
            processCardMatches()
        }
    }

    private fun processCardMatches() {
        viewModelScope.launch {
            var currentState = _gameState.value
            if (currentState.flippedCardIndices.size != 2 || currentState.processingMatch) return@launch

            _gameState.update { it.copy(processingMatch = true, attemptCount = it.attemptCount + 1) }
            currentState = _gameState.value

            val firstIndex = currentState.flippedCardIndices[0]
            val secondIndex = currentState.flippedCardIndices[1]
            if (firstIndex !in currentState.cards.indices || secondIndex !in currentState.cards.indices) {
                _gameState.update { it.copy(processingMatch = false, flippedCardIndices = emptyList()) }
                return@launch
            }
            val card1 = currentState.cards[firstIndex]
            val card2 = currentState.cards[secondIndex]

            if (card1.imageRes == card2.imageRes) {
                handleCorrectMatch(firstIndex, secondIndex)
            } else {
                handleIncorrectMatch(firstIndex, secondIndex)
            }
        }
    }

    private fun handleCorrectMatch(firstIndex: Int, secondIndex: Int) {
        SoundManager.playEffect(getApplication(), R.raw.dats_right)
        val newCards = _gameState.value.cards.toMutableStateList()
        newCards[firstIndex] = newCards[firstIndex].copy(isMatched = true, isFlipped = true)
        newCards[secondIndex] = newCards[secondIndex].copy(isMatched = true, isFlipped = true)

        val allMatched = newCards.all { it.isMatched }
        if (allMatched) {
            SoundManager.playEffect(getApplication(), R.raw.card_flip__win)
            timer.stop()
        }
        _gameState.update {
            it.copy(
                cards = newCards,
                currentTurnIncorrectAttempts = 0,
                allPairsMatched = allMatched,
                isTimerRunning = !allMatched,
                flippedCardIndices = emptyList(),
                processingMatch = false
            )
        }
    }

    private fun handleIncorrectMatch(firstIndex: Int, secondIndex: Int) {
        viewModelScope.launch {
            SoundManager.playEffect(getApplication(), R.raw.dats_wrong)
            val newIncorrectAttempts = _gameState.value.currentTurnIncorrectAttempts + 1

            delay(1000)

            val stateAfterDelay = _gameState.value
            if (!stateAfterDelay.allPairsMatched && !stateAfterDelay.showLoseScreen) {
                val newCards = stateAfterDelay.cards.toMutableStateList()
                if (firstIndex in newCards.indices) newCards[firstIndex] = newCards[firstIndex].copy(isFlipped = false)
                if (secondIndex in newCards.indices) newCards[secondIndex] = newCards[secondIndex].copy(isFlipped = false)
                _gameState.update { it.copy(cards = newCards) }
            }

            val difficulty = stateAfterDelay.currentDifficulty
            val maxAttemptsReached = difficulty != null && newIncorrectAttempts >= difficulty.maxAttempts

            if (maxAttemptsReached && !stateAfterDelay.allPairsMatched && !stateAfterDelay.showLoseScreen) {
                _gameState.update {
                    it.copy(
                        currentTurnIncorrectAttempts = newIncorrectAttempts,
                        loseReason = "Too many mistakes!",
                        showLoseScreen = true,
                        isTimerRunning = false,
                        flippedCardIndices = emptyList(),
                        processingMatch = false
                    )
                }
                timer.stop()
                SoundManager.playEffect(getApplication(), R.raw.matching_gamelose_sound)
            } else {
                _gameState.update {
                    it.copy(
                        currentTurnIncorrectAttempts = newIncorrectAttempts,
                        flippedCardIndices = emptyList(),
                        processingMatch = false
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timer.stop()
    }
}

