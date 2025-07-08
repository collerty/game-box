package com.example.gamehub.features.spaceinvaders.classes

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gamehub.features.spaceinvaders.util.EventBusAudioManager
import com.example.gamehub.repository.implementations.SpaceInvadersRepository
import com.example.gamehub.repository.interfaces.ISpaceInvadersRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class SpaceInvadersViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {

    sealed class UiEvent {
        object PlayExplodeSound : UiEvent()
        object PlayShootSound: UiEvent()
        object PlayTakeDamageSound: UiEvent()
        object PlayUFOSound: UiEvent()
        object Vibrate : UiEvent()
    }

    object EventBus {
        private val _uiEvent = MutableSharedFlow<UiEvent>()
        val uiEvent = _uiEvent.asSharedFlow()

        suspend fun emit(event: UiEvent) {
            _uiEvent.emit(event)
        }
    }

    private val audioManager = EventBusAudioManager { event ->
        EventBus.emit(event)
    }

    // Replace FirestoreHighScoreRepository with our new repository
    private val highScoreRepository: ISpaceInvadersRepository = SpaceInvadersRepository()

    private var _gameEngine = SpaceInvadersGameEngine(
        audioManager = audioManager,
        coroutineScope = viewModelScope
    )
    val gameEngine: SpaceInvadersGameEngine get() = _gameEngine

    private val _tick = mutableStateOf(0)
    val tick: State<Int> = _tick

    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    var xTilt: Float by mutableStateOf(0f)

    var tiltControlEnabled by mutableStateOf(false)
        private set

    val playerName = highScoreRepository.playerName
    val highScores = highScoreRepository.highScores

    fun onPlayerNameChanged(newName: String) {
        highScoreRepository.onPlayerNameChanged(newName)
    }

    fun submitScore(playerName: String, newScore: Int) {
        highScoreRepository.submitScore(playerName, newScore)
    }

    private var screenSizeSet = false

    init {
        highScoreRepository.fetchHighScores()
    }

    fun setScreenSize(width: Float, height: Float) {
        if (!screenSizeSet) {
            Log.d("SpaceInvaders", "Setting screen size: width=$width, height=$height")
            //YES, this has to be like this, due to going from portrait to landscape, otherwise the bunkers wont work correctly
            _gameEngine.screenWidthPx = height
            _gameEngine.screenHeightPx = width
            // Set player Y position to bottom of screen
            _gameEngine.playerController.setPlayer(
                _gameEngine.player.copy(y = height - 100f)
            )
            _gameEngine.bunkerController.initializeBunkers(height, width)
            screenSizeSet = true
            startGameLoop()
        }
    }

    fun toggleTiltControl() {
        tiltControlEnabled = !tiltControlEnabled
        if (tiltControlEnabled) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        } else {
            sensorManager.unregisterListener(this)
        }
    }

    private fun startGameLoop() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(16L) // ~60 FPS
                if (tiltControlEnabled) {
                    gameEngine.playerController.updateFromTilt(xTilt)
                }
                gameEngine.updateGame()
                _tick.value++ // Trigger UI recomposition
            }
        }
    }

    fun restartGame(screenWidthPx: Float, screenHeightPx: Float) {
        if (tiltControlEnabled) {
            sensorManager.unregisterListener(this)
        }

        Log.d("SpaceInvaders", "Restarting game with screen size: $screenWidthPx x $screenHeightPx")

        // Create a new game engine with all dependencies
        _gameEngine = SpaceInvadersGameEngine(
            audioManager = audioManager,
            coroutineScope = viewModelScope
        )

        // Set screen dimensions in engine
        _gameEngine.screenWidthPx = screenWidthPx
        _gameEngine.screenHeightPx = screenHeightPx

        Log.d("SpaceInvaders", "Screen size set: ${_gameEngine.screenWidthPx} x ${_gameEngine.screenHeightPx}")

        _gameEngine.playerController.setPlayer(
            _gameEngine.player.copy(y = screenHeightPx - 100f)
        )

        xTilt = 0f
        if (tiltControlEnabled) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }

        _gameEngine.reset()
        _tick.value++
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            xTilt = -it.values[1] // left/right tilt
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onCleared() {
        sensorManager.unregisterListener(this)
        super.onCleared()
    }
}