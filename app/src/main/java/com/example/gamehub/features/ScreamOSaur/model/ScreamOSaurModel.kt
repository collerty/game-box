package com.example.gamehub.features.ScreamOSaur.model

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.snapshotFlow // Added import for snapshotFlow
import androidx.compose.ui.geometry.Rect
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gamehub.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.random.Random

// Game state enum
enum class GameState { READY, PLAYING, PAUSED, GAME_OVER }

// Obstacle data class
data class Obstacle(
    var xPosition: Float, // Pixel value
    val height: Float,    // Pixel value
    val width: Float,     // Pixel value
    var passed: Boolean = false,
    val id: Long = Random.nextLong() // Unique ID for animations/keys
)

// UI State data class
data class ScreamOSaurUiState(
    val gameState: GameState = GameState.READY,
    val score: Int = 0,
    val obstacles: List<Obstacle> = emptyList(),
    val currentAmplitude: Int = 0,
    val jumpAnimValue: Float = 0f,
    val isJumping: Boolean = false,
    val runningAnimState: Int = 0,
    // Game dimensions - will be set by the UI
    val dinosaurVisualXPositionPx: Float = 0f,
    val dinoTopYOnGroundPx: Float = 0f,
    val dinosaurSizePx: Float = 0f,
    val gameHeightPx: Float = 0f,
    val groundHeightPx: Float = 0f,
    val jumpMagnitudePx: Float = 0f,
    val gameSpeed: Float = ScreamOSaurViewModel.INITIAL_GAME_SPEED,
    val hasAudioPermission: Boolean? = null // Null initially, set by UI
)

class ScreamOSaurViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ScreamOSaurUiState())
    val uiState: StateFlow<ScreamOSaurUiState> = _uiState.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var recordJob: Job? = null
    private var gameLoopJob: Job? = null
    private var animationJob: Job? = null

    private val jumpAnim = Animatable(0f)

    private var gameStartTime = 0L
    private var timeAtPause = 0L
    private var initialDelayHasOccurred = false

    companion object {
        const val MIN_AMPLITUDE_THRESHOLD = 1800
        const val JUMP_AMPLITUDE_THRESHOLD = 12000
        const val INITIAL_GAME_SPEED = 5f
        const val MAX_GAME_SPEED = 20f
    }

    private var jumpSoundPlayer: MediaPlayer? = null

    init {
        initializeSoundPlayers()
        // Observe jumpAnim changes and update uiState
        viewModelScope.launch {
            snapshotFlow { jumpAnim.value }.collect { value -> // Changed to snapshotFlow
                _uiState.update { it.copy(jumpAnimValue = value) }
            }
        }
    }

    private fun initializeSoundPlayers() {
        jumpSoundPlayer = MediaPlayer.create(getApplication(), R.raw.dino_jump_sound)?.apply {
            setVolume(0.5f, 0.5f)
        }
    }

    private fun playJumpSound() {
        jumpSoundPlayer?.let {
            if (it.isPlaying) {
                it.seekTo(0)
            } else {
                try {
                    it.start()
                } catch (_: IllegalStateException) { /* Handle or log */ }
            }
        }
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
            startAudioProcessing() // Start if game was already playing and permission granted
        } else if (!hasPermission) {
            recordJob?.cancel() // Stop audio processing if permission revoked
            audioRecord?.release()
            audioRecord = null
            _uiState.update { it.copy(currentAmplitude = 0) }
        }
    }

    fun startGame() {
        viewModelScope.launch { jumpAnim.snapTo(0f) }
        _uiState.update {
            it.copy(
                gameState = GameState.PLAYING,
                score = 0,
                obstacles = emptyList(),
                gameSpeed = INITIAL_GAME_SPEED,
                isJumping = false,
                runningAnimState = 0
            )
        }
        initialDelayHasOccurred = false
        stopAllJobs(releaseAudio = true) // Ensure everything is reset
        startGameLoop()
        if (_uiState.value.hasAudioPermission == true) {
            startAudioProcessing()
        }
        startRunningAnimation()
    }

    fun pauseGame() {
        if (_uiState.value.gameState == GameState.PLAYING) {
            timeAtPause = System.currentTimeMillis()
            _uiState.update { it.copy(gameState = GameState.PAUSED) }
            stopAllJobs(releaseAudio = true) // Release mic when paused
        }
    }

    fun resumeGame() {
        if (_uiState.value.gameState == GameState.PAUSED) {
            val pausedDuration = System.currentTimeMillis() - timeAtPause
            gameStartTime += pausedDuration
            _uiState.update { it.copy(gameState = GameState.PLAYING) }
            startGameLoop()
            if (_uiState.value.hasAudioPermission == true) {
                startAudioProcessing()
            }
            startRunningAnimation()
        }
    }

    private fun gameOver() {
        stopAllJobs(releaseAudio = true)
        _uiState.update { it.copy(gameState = GameState.GAME_OVER, currentAmplitude = 0) }
    }

    private fun startGameLoop() {
        gameLoopJob = viewModelScope.launch(Dispatchers.Default) {
            if (!initialDelayHasOccurred) {
                delay(1500)
                gameStartTime = System.currentTimeMillis()
                initialDelayHasOccurred = true
            }

            while (isActive && _uiState.value.gameState == GameState.PLAYING) {
                val currentState = _uiState.value
                val currentTime = System.currentTimeMillis()
                val elapsedGameTime = currentTime - gameStartTime

                // Update obstacles
                val updatedObstacles = currentState.obstacles.map {
                    it.copy(xPosition = it.xPosition - currentState.gameSpeed)
                }.filter { it.xPosition + it.width > 0 }

                // Spawn new obstacles
                val canvasWidthPx = currentState.gameHeightPx * (16f / 9f)
                val spawnTriggerX = canvasWidthPx * 0.5f
                var newObstaclesCurrent = updatedObstacles

                if (newObstaclesCurrent.isEmpty() && elapsedGameTime > 500) { // Wait a bit before first obstacle
                    newObstaclesCurrent = listOf(createNewObstacle(canvasWidthPx, currentState))
                } else if (newObstaclesCurrent.isNotEmpty() && newObstaclesCurrent.last().xPosition < spawnTriggerX && newObstaclesCurrent.size < 5) {
                    newObstaclesCurrent = newObstaclesCurrent + createNewObstacle(canvasWidthPx, currentState)
                }

                // Update score and game speed
                var newScore = currentState.score
                var newSpeed = currentState.gameSpeed
                val scoredObstacles = newObstaclesCurrent.map { obstacle ->
                    if (!obstacle.passed && (obstacle.xPosition + obstacle.width) < currentState.dinosaurVisualXPositionPx) {
                        newScore++
                        newSpeed = (INITIAL_GAME_SPEED + (newScore / 5f)).coerceAtMost(MAX_GAME_SPEED)
                        obstacle.copy(passed = true)
                    } else {
                        obstacle
                    }
                }

                _uiState.update {
                    it.copy(
                        obstacles = scoredObstacles,
                        score = newScore,
                        gameSpeed = newSpeed
                    )
                }

                // Collision check
                if (elapsedGameTime > 2000) { // Delay collision check
                    val dinoTopY = currentState.dinoTopYOnGroundPx - (jumpAnim.value * currentState.jumpMagnitudePx)
                    val dinoHitbox = Rect(
                        left = currentState.dinosaurVisualXPositionPx + currentState.dinosaurSizePx * 0.20f,
                        top = dinoTopY + currentState.dinosaurSizePx * 0.20f,
                        right = currentState.dinosaurVisualXPositionPx + currentState.dinosaurSizePx * 0.80f,
                        bottom = dinoTopY + currentState.dinosaurSizePx * 0.90f // Adjusted for feet
                    )

                    val collision = scoredObstacles.any { obs ->
                        val obsRect = Rect(
                            left = obs.xPosition,
                            top = currentState.gameHeightPx - currentState.groundHeightPx - obs.height,
                            right = obs.xPosition + obs.width,
                            bottom = currentState.gameHeightPx - currentState.groundHeightPx
                        )
                        dinoHitbox.overlaps(obsRect)
                    }

                    if (collision) {
                        gameOver()
                        break
                    }
                }
                delay(16) // ~60fps
            }
        }
    }

    private fun createNewObstacle(canvasWidthPx: Float, state: ScreamOSaurUiState): Obstacle {
        val baseHeight = state.gameHeightPx
        return Obstacle(
            xPosition = canvasWidthPx + Random.nextFloat() * (canvasWidthPx * 0.2f),
            height = Random.nextFloat() * (baseHeight * 0.275f - baseHeight * 0.125f) + baseHeight * 0.125f,
            width = Random.nextFloat() * (baseHeight * 0.15f - baseHeight * 0.075f) + baseHeight * 0.075f
        )
    }

    private fun startAudioProcessing() {
        if (ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            _uiState.update { it.copy(hasAudioPermission = false, currentAmplitude = 0) }
            return
        }
        _uiState.update { it.copy(hasAudioPermission = true) }

        val bufferSize = AudioRecord.getMinBufferSize(8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (bufferSize <= 0) { return }

        audioRecord?.release() // Release previous instance if any
        audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, 8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord?.release()
            audioRecord = null
            return
        }

        recordJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                audioRecord?.startRecording()
                val buffer = ShortArray(bufferSize)
                while (isActive && _uiState.value.gameState == GameState.PLAYING && _uiState.value.hasAudioPermission == true) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        val maxAmplitudeRaw = buffer.take(read).maxOfOrNull { it.toInt().absoluteValue } ?: 0
                        val amp = if (maxAmplitudeRaw >= MIN_AMPLITUDE_THRESHOLD) maxAmplitudeRaw else 0
                        _uiState.update { it.copy(currentAmplitude = amp) }

                        if (amp > JUMP_AMPLITUDE_THRESHOLD && !_uiState.value.isJumping) {
                            triggerJump()
                        }
                    }
                    delay(50)
                }
            } catch (e: Exception) {
                // Log or handle exception
                 _uiState.update { it.copy(currentAmplitude = 0) }
            } finally {
                if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord?.stop()
                }
                // Don't release here if we might resume, release in onCleared or when explicitly stopped
                if (_uiState.value.gameState != GameState.PLAYING || _uiState.value.hasAudioPermission != true) {
                    _uiState.update { it.copy(currentAmplitude = 0) }
                    audioRecord?.release()
                    audioRecord = null
                }
            }
        }
    }

    private fun triggerJump() {
        if (_uiState.value.isJumping) return
        _uiState.update { it.copy(isJumping = true) }
        playJumpSound()
        viewModelScope.launch {
            jumpAnim.snapTo(0f)
            jumpAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing)
            )
            jumpAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
            )
            _uiState.update { it.copy(isJumping = false, jumpAnimValue = 0f) } // Ensure jumpAnimValue is reset after animation
        }
    }

    private fun startRunningAnimation() {
        animationJob = viewModelScope.launch {
            while (isActive && _uiState.value.gameState == GameState.PLAYING) {
                _uiState.update { it.copy(runningAnimState = (it.runningAnimState + 1) % 4) }
                delay(100)
            }
        }
    }

    private fun stopAllJobs(releaseAudio: Boolean) {
        gameLoopJob?.cancel()
        animationJob?.cancel()
        recordJob?.cancel()
        if (releaseAudio) {
            audioRecord?.release()
            audioRecord = null
            _uiState.update { it.copy(currentAmplitude = 0) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAllJobs(releaseAudio = true)
        jumpSoundPlayer?.release()
        jumpSoundPlayer = null
    }
}
