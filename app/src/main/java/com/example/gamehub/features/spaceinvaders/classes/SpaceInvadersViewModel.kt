package com.example.gamehub.features.spaceinvaders.classes

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SpaceInvadersViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {

    private val _gameEngine = SpaceInvadersGameEngine()
    val gameEngine: SpaceInvadersGameEngine get() = _gameEngine

    private val _tick = mutableStateOf(0)
    val tick: State<Int> = _tick

    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    var xTilt: Float by mutableStateOf(0f)

    var tiltControlEnabled by mutableStateOf(false)
        private set
    

    init {
        startGameLoop()
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
                delay(16L) // ~60 FPS
                if (tiltControlEnabled) {
                    gameEngine.playerController.updateFromTilt(xTilt)
                }
                gameEngine.updateGame()
                _tick.value++ // Trigger UI recomposition
            }
        }
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
