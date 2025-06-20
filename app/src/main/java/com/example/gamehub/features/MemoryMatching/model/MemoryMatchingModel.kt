package com.example.gamehub.features.MemoryMatching.model

import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gamehub.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Data class to hold card information
data class MemoryCard(
    val id: Int, // Unique ID for the card instance
    val imageRes: Int, // Drawable resource ID for the image
    var isFlipped: Boolean = false,
    var isMatched: Boolean = false
)

// Enum for managing screen states
enum class GameScreen {
    DIFFICULTY_SELECTION,
    PLAYING
}

// Data class for defining difficulty levels
data class GameDifficulty(
    val pairs: Int,
    val columns: Int,
    val displayName: String,
    val cardBackResId: Int, // Resource ID for the card back image
    val timeLimitSeconds: Int, // Time limit for this difficulty
    val maxAttempts: Int, // Maximum incorrect attempts allowed
    val totalCards: Int = pairs * 2
)

// Game State data class
data class MemoryMatchingState(
    val cards: SnapshotStateList<MemoryCard> = SnapshotStateList(),
    val flippedCardIndices: List<Int> = emptyList(),
    val processingMatch: Boolean = false,
    val attemptCount: Int = 0,
    val currentTurnIncorrectAttempts: Int = 0,
    val allPairsMatched: Boolean = false,
    val timeLeftInSeconds: Int = 0,
    val isTimerRunning: Boolean = false,
    val showLoseScreen: Boolean = false,
    val loseReason: String? = null,
    val currentDifficulty: GameDifficulty? = null,
    val currentScreen: GameScreen = GameScreen.DIFFICULTY_SELECTION
)

class MemoryMatchingViewModel(application: Application) : AndroidViewModel(application) {

    private val _gameState = MutableStateFlow(MemoryMatchingState())
    val gameState: StateFlow<MemoryMatchingState> = _gameState.asStateFlow()

    // Sound Players
    private var cardFlipSoundPlayer: MediaPlayer? = null
    private var winSoundPlayer: MediaPlayer? = null
    private var matchSoundPlayer: MediaPlayer? = null
    private var noMatchSoundPlayer: MediaPlayer? = null
    private var loseGameSoundPlayer: MediaPlayer? = null

    private var timerJob: Job? = null

    val allImageResources: List<Int> = listOf(
        R.drawable.basketball, R.drawable.bee, R.drawable.dice, R.drawable.herosword,
        R.drawable.ladybug, R.drawable.ramen, R.drawable.taxi, R.drawable.zombie,
        R.drawable.zelda, R.drawable.spaceman, R.drawable.robot, R.drawable.island,
        R.drawable.gamingcontroller, R.drawable.dragon, R.drawable.browncar
    )

    val gameDifficulties: List<GameDifficulty> = listOf(
        GameDifficulty(pairs = 6, columns = 3, displayName = "Easy (3x4 - 12 Cards)", cardBackResId = R.drawable.old_card_back, timeLimitSeconds = 50, maxAttempts = 4),
        GameDifficulty(pairs = 8, columns = 4, displayName = "Medium (4x4 - 16 Cards)", cardBackResId = R.drawable.card_back_red, timeLimitSeconds = 60, maxAttempts = 5),
        GameDifficulty(pairs = 10, columns = 4, displayName = "Hard (4x5 - 20 Cards)", cardBackResId = R.drawable.cards_back_blue, timeLimitSeconds = 75, maxAttempts = 6),
        GameDifficulty(pairs = 12, columns = 4, displayName = "Expert (4x6 - 24 Cards)", cardBackResId = R.drawable.back_card_brown, timeLimitSeconds = 90, maxAttempts = 7)
    )

    init {
        initializeSoundPlayers(application.applicationContext)
        _gameState.update { it.copy(timeLeftInSeconds = gameDifficulties.first().timeLimitSeconds) }
    }

    private fun initializeSoundPlayers(context: Context) {
        cardFlipSoundPlayer = MediaPlayer.create(context, R.raw.card_flip)?.apply { setVolume(0.3f, 0.3f) }
        winSoundPlayer = MediaPlayer.create(context, R.raw.card_flip__win)?.apply { setVolume(0.6f, 0.6f) }
        matchSoundPlayer = MediaPlayer.create(context, R.raw.dats_right)?.apply { setVolume(0.5f, 0.5f) }
        noMatchSoundPlayer = MediaPlayer.create(context, R.raw.dats_wrong)?.apply { setVolume(0.5f, 0.5f) }
        loseGameSoundPlayer = MediaPlayer.create(context, R.raw.matching_gamelose_sound)?.apply { setVolume(0.6f, 0.6f) }
    }

    private fun playCardFlipSound() { cardFlipSoundPlayer?.safePlay() }
    private fun playWinSound() { winSoundPlayer?.safePlay() }
    private fun playMatchSound() { matchSoundPlayer?.safePlay() }
    private fun playNoMatchSound() { noMatchSoundPlayer?.safePlay() }
    private fun playLoseSound() { loseGameSoundPlayer?.safePlay() }

    private fun MediaPlayer.safePlay() {
        try {
            if (isPlaying) {
                seekTo(0)
            } else {
                start()
            }
        } catch (e: IllegalStateException) {
            // Player might not be prepared or in an error state
            prepareAsync() // Try to prepare it for next time
        }
    }

    private fun generateCardsForDifficulty(difficulty: GameDifficulty): SnapshotStateList<MemoryCard> {
        val numPairs = difficulty.pairs
        val uniqueImagesToTake = kotlin.math.min(numPairs, allImageResources.size)
        val selectedImages = allImageResources.shuffled().take(uniqueImagesToTake)
        return (selectedImages + selectedImages)
            .mapIndexed { index, resId -> MemoryCard(id = index, imageRes = resId) }
            .shuffled()
            .toMutableStateList()
    }

    fun startGame(difficulty: GameDifficulty) {
        timerJob?.cancel()
        val newCards = generateCardsForDifficulty(difficulty)
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
                isTimerRunning = true,
                showLoseScreen = false,
                loseReason = null,
                currentScreen = GameScreen.PLAYING
            )
        }
        stopAndPrepareSound(winSoundPlayer)
        stopAndPrepareSound(loseGameSoundPlayer)
        startTimer()
    }

    fun restartGame() {
        _gameState.value.currentDifficulty?.let { difficulty ->
            startGame(difficulty)
        } ?: run {
            // Should not happen if restart is called from playing screen, but as a fallback:
            _gameState.update { it.copy(currentScreen = GameScreen.DIFFICULTY_SELECTION) }
        }
    }

    fun selectDifficultyScreen() {
        timerJob?.cancel()
        _gameState.update { it.copy(currentScreen = GameScreen.DIFFICULTY_SELECTION, isTimerRunning = false, allPairsMatched = false, showLoseScreen = false) }
        stopAndPrepareSound(winSoundPlayer)
        stopAndPrepareSound(loseGameSoundPlayer)
    }

    private fun stopAndPrepareSound(mediaPlayer: MediaPlayer?) {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer.stop()
                mediaPlayer.prepareAsync()
            }
        } catch (e: IllegalStateException) {
            // Handle or log
        }
    }

    fun onCardClicked(cardIndex: Int) {
        val currentState = _gameState.value
        val card = currentState.cards.getOrNull(cardIndex)

        if (card == null || card.isFlipped || card.isMatched || currentState.flippedCardIndices.size >= 2 || currentState.processingMatch || currentState.allPairsMatched || currentState.showLoseScreen) {
            return
        }

        playCardFlipSound()

        val newCards = currentState.cards.toMutableStateList()
        newCards[cardIndex] = card.copy(isFlipped = true)
        val newFlippedIndices = currentState.flippedCardIndices + cardIndex

        _gameState.update {
            it.copy(
                cards = newCards,
                flippedCardIndices = newFlippedIndices
            )
        }

        if (newFlippedIndices.size == 2) {
            processCardMatches()
        }
    }

    private fun processCardMatches() {
        viewModelScope.launch {
            // Fetch the latest state again inside coroutine
            var currentState = _gameState.value
            if (currentState.flippedCardIndices.size != 2 || currentState.processingMatch) return@launch

            _gameState.update { it.copy(processingMatch = true, attemptCount = it.attemptCount + 1) }
            currentState = _gameState.value // refresh state after update

            val firstIndex = currentState.flippedCardIndices[0]
            val secondIndex = currentState.flippedCardIndices[1]
            // Ensure indices are valid before accessing cards
            if (firstIndex !in currentState.cards.indices || secondIndex !in currentState.cards.indices) {
                 _gameState.update { it.copy(processingMatch = false, flippedCardIndices = emptyList()) } // Reset
                return@launch
            }
            val card1 = currentState.cards[firstIndex]
            val card2 = currentState.cards[secondIndex]

            if (card1.imageRes == card2.imageRes) {
                playMatchSound()
                val newCards = currentState.cards.toMutableStateList()
                newCards[firstIndex] = card1.copy(isMatched = true, isFlipped = true)
                newCards[secondIndex] = card2.copy(isMatched = true, isFlipped = true)

                val allMatched = newCards.all { it.isMatched }
                if (allMatched) {
                    playWinSound()
                    timerJob?.cancel()
                }
                _gameState.update {
                    it.copy(
                        cards = newCards,
                        currentTurnIncorrectAttempts = 0,
                        allPairsMatched = allMatched,
                        isTimerRunning = if (allMatched) false else it.isTimerRunning,
                        flippedCardIndices = emptyList(),
                        processingMatch = false
                    )
                }
            } else {
                playNoMatchSound()
                val newIncorrectAttempts = currentState.currentTurnIncorrectAttempts + 1

                delay(1000)

                val stateAfterDelay = _gameState.value // Crucial: Re-fetch state
                if (!stateAfterDelay.allPairsMatched && !stateAfterDelay.showLoseScreen) {
                    // Only flip back if game hasn't ended during the delay
                    val newCards = stateAfterDelay.cards.toMutableStateList()
                    if (firstIndex in newCards.indices) newCards[firstIndex] = card1.copy(isFlipped = false)
                    if (secondIndex in newCards.indices) newCards[secondIndex] = card2.copy(isFlipped = false)
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
                    timerJob?.cancel()
                    playLoseSoundIfGameLost()
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
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) { // Loop managed by delay and checks
                val currentState = _gameState.value
                if (!currentState.isTimerRunning || currentState.timeLeftInSeconds <= 0 || currentState.allPairsMatched || currentState.showLoseScreen) {
                    break // Exit loop if timer shouldn't run or game ended
                }
                delay(1000)
                _gameState.update { prev ->
                    if (prev.isTimerRunning && !prev.allPairsMatched && !prev.showLoseScreen) { // Double check conditions before decrementing
                        prev.copy(timeLeftInSeconds = prev.timeLeftInSeconds - 1)
                    } else {
                        prev // No change if conditions no longer met
                    }
                }
            }

            // Check conditions for losing due to time up AFTER the loop
            val finalState = _gameState.value
            if (finalState.isTimerRunning && finalState.timeLeftInSeconds <= 0 && !finalState.allPairsMatched && !finalState.showLoseScreen) {
                _gameState.update {
                    it.copy(
                        isTimerRunning = false,
                        loseReason = "Time's Up!",
                        showLoseScreen = true
                    )
                }
                playLoseSoundIfGameLost()
            }
        }
    }

    fun playLoseSoundIfGameLost() { // Renamed for clarity
        viewModelScope.launch { // Ensure it runs on a coroutine scope if called from UI thread or other contexts
            val currentState = _gameState.value
            if (currentState.showLoseScreen && !currentState.allPairsMatched) {
                playLoseSound()
                stopAndPrepareSound(winSoundPlayer)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        cardFlipSoundPlayer?.release()
        winSoundPlayer?.release()
        matchSoundPlayer?.release()
        noMatchSoundPlayer?.release()
        loseGameSoundPlayer?.release()
        cardFlipSoundPlayer = null
        winSoundPlayer = null
        matchSoundPlayer = null
        noMatchSoundPlayer = null
        loseGameSoundPlayer = null
    }
}

