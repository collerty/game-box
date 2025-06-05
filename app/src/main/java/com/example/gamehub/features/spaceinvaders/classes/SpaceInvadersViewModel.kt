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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class SpaceInvadersViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {

    private var _gameEngine = SpaceInvadersGameEngine()
    val gameEngine: SpaceInvadersGameEngine get() = _gameEngine

    private val _tick = mutableStateOf(0)
    val tick: State<Int> = _tick

    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    var xTilt: Float by mutableStateOf(0f)

    var tiltControlEnabled by mutableStateOf(false)
        private set

    private val db = FirebaseFirestore.getInstance()

    var playerName = MutableStateFlow("")
        private set

    val highScores = MutableStateFlow<List<PlayerScore>>(emptyList())

    fun onPlayerNameChanged(newName: String) {
        playerName.value = newName
    }

    private fun fetchHighScores() {
        db.collection("space-invaders")
            .orderBy("score", Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener

                val scores = snapshot.documents.mapNotNull {
                    it.toObject(PlayerScore::class.java)
                }
                highScores.value = scores
            }
    }

    fun submitScore(playerName: String, newScore: Int) {
        val docRef = db.collection("space-invaders")
            .document(playerName.lowercase())

        docRef.get()
            .addOnSuccessListener { document ->
                val currentScore = document.getLong("score")?.toInt() ?: 0
                if (newScore > currentScore) {
                    docRef.set(mapOf(
                        "player" to playerName,
                        "score" to newScore
                    ))
                    Log.d("Firestore", "New high score saved.")
                } else {
                    Log.d("Firestore", "Score not updated (not higher).")
                }
            }
            .addOnFailureListener {
                Log.e("Firestore", "Failed to read existing score", it)
            }
    }

    private var screenSizeSet = false

    init {
        fetchHighScores()
        // Do not start game loop here
    }

    fun setScreenSize(width: Float, height: Float) {
        if (!screenSizeSet) {
            _gameEngine.screenWidthPx = width
            _gameEngine.screenHeightPx = height
            // Set player Y position to bottom of screen
            _gameEngine.playerController.setPlayer(
                _gameEngine.player.copy(y = height - 100f)
            )
            _gameEngine.initializeBunkers()
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
                delay(16L) // ~60 FPS
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
        _gameEngine = SpaceInvadersGameEngine()
        // Set screen dimensions in engine
        _gameEngine.screenWidthPx = screenWidthPx
        _gameEngine.screenHeightPx = screenHeightPx
        _gameEngine.playerController.setPlayer(
            _gameEngine.player.copy(y = screenHeightPx - 100f)
        )
        xTilt = 0f
        if (tiltControlEnabled) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }
        _gameEngine.gameState = GameState.PLAYING
        _gameEngine.initializeBunkers()
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