package com.example.gamehub.features.ScreamOSaur.model

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gamehub.R
import com.example.gamehub.audio.SoundManager
import com.example.gamehub.features.ScreamOSaur.backend.AudioProcessor
import com.example.gamehub.features.ScreamOSaur.backend.GameLoop
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ScreamOSaurViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ScreamOSaurUiState())
    val uiState: StateFlow<ScreamOSaurUiState> = _uiState.asStateFlow()

    private val audioProcessor = AudioProcessor(getApplication(), viewModelScope)

    private var animationJob: Job? = null
    private var timeAtPause = 0L

    private val gameLoop = GameLoop(
        scope = viewModelScope,
        onUpdate = { newState ->
            _uiState.update {
                it.copy(
                    obstacles = newState.obstacles,
                    score = newState.score,
                    gameSpeed = newState.gameSpeed
                )
            }
        },
        onGameOver = {
            stopAudio()
            _uiState.update { it.copy(gameState = GameState.GAME_OVER) }
            Log.d(TAG, "Game over triggered")
        },
        getState = { _uiState.value }
    )

    companion object {
        const val JUMP_AMPLITUDE_THRESHOLD = AudioProcessor.MIN_AMPLITUDE_THRESHOLD
        private const val TAG = "ScreamOSaurDebug"
    }

    init {
        Log.d(TAG, "ScreamOSaurViewModel initialized")

        viewModelScope.launch {
            audioProcessor.amplitude.collect { amplitude ->
                _uiState.update { it.copy(currentAmplitude = amplitude) }

                if (amplitude > JUMP_AMPLITUDE_THRESHOLD * 0.8) {
                    Log.d(
                        TAG,
                        "Audio amplitude: $amplitude, threshold: $JUMP_AMPLITUDE_THRESHOLD, isJumping: ${_uiState.value.isJumping}"
                    )
                }

                if (amplitude >= JUMP_AMPLITUDE_THRESHOLD && !_uiState.value.isJumping) {
                    Log.d(TAG, "Jump condition met! Amplitude: $amplitude >= $JUMP_AMPLITUDE_THRESHOLD")
                    triggerJump()
                }
            }
        }
    }

    private fun playJumpSound() {
        SoundManager.playEffect(getApplication(), R.raw.dino_jump_sound)
    }

    fun setGameDimensions(
        gameHeightPx: Float,
        groundHeightPx: Float,
        dinosaurSizePx: Float,
        dinosaurVisualXPositionPx: Float,
        jumpMagnitudePx: Float
    ) {
        _uiState.update {
            it.copy(
                gameHeightPx = gameHeightPx,
                groundHeightPx = groundHeightPx,
                dinosaurSizePx = dinosaurSizePx,
                dinosaurVisualXPositionPx = dinosaurVisualXPositionPx,
                dinoTopYOnGroundPx = gameHeightPx - groundHeightPx - dinosaurSizePx,
                jumpMagnitudePx = jumpMagnitudePx
            )
        }
    }

    fun updateAudioPermissionState(hasPermission: Boolean) {
        _uiState.update { it.copy(hasAudioPermission = hasPermission) }
        if (hasPermission && _uiState.value.gameState == GameState.PLAYING) {
            audioProcessor.start()
        } else if (!hasPermission) {
            stopAudio()
        }
    }

    fun startGame() {
        _uiState.update {
            it.copy(
                gameState = GameState.PLAYING,
                score = 0,
                obstacles = emptyList(),
                gameSpeed = GameLoop.INITIAL_GAME_SPEED,
                isJumping = false,
                runningAnimState = 0
            )
        }

        gameLoop.reset()
        stopAllJobs()
        gameLoop.start()

        if (_uiState.value.hasAudioPermission == true) {
            viewModelScope.launch {
                delay(200)
                audioProcessor.start()
            }
        }

        startRunningAnimation()
    }

    fun pauseGame() {
        if (_uiState.value.gameState == GameState.PLAYING) {
            timeAtPause = System.currentTimeMillis()
            _uiState.update { it.copy(gameState = GameState.PAUSED) }
            stopAllJobs()
        }
    }

    fun resumeGame() {
        if (_uiState.value.gameState == GameState.PAUSED) {
            _uiState.update { it.copy(gameState = GameState.PLAYING) }
            gameLoop.start()
            if (_uiState.value.hasAudioPermission == true) {
                audioProcessor.start()
            }
            startRunningAnimation()
        }
    }

    private fun triggerJump() {
        if (_uiState.value.isJumping) {
            Log.d(TAG, "Jump already in progress, ignoring trigger")
            return
        }
        Log.d(TAG, "JUMP TRIGGERED! Requesting jump animation.")
        _uiState.update { it.copy(isJumping = true) }
        playJumpSound()
    }

    fun setJumpAnimValue(value: Float) {
        _uiState.update { it.copy(jumpAnimValue = value) }
    }

    fun onJumpAnimationFinished() {
        _uiState.update { it.copy(isJumping = false, jumpAnimValue = 0f) }
        Log.d(TAG, "Jump animation complete, reset jumping state")
    }

    private fun startRunningAnimation() {
        animationJob?.cancel()
        animationJob = viewModelScope.launch {
            while (isActive && _uiState.value.gameState == GameState.PLAYING) {
                _uiState.update { it.copy(runningAnimState = (it.runningAnimState + 1) % 4) }
                delay(100)
            }
        }
    }

    private fun stopAudio() {
        audioProcessor.stop()
        _uiState.update { it.copy(currentAmplitude = 0) }
    }

    private fun stopAllJobs() {
        gameLoop.stop()
        animationJob?.cancel()
        stopAudio()
    }

    override fun onCleared() {
        super.onCleared()
        stopAllJobs()
    }
}
