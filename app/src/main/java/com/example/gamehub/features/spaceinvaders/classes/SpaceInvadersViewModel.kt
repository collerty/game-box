package com.example.gamehub.features.spaceinvaders.classes

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SpaceInvadersViewModel : ViewModel() {
    private val _gameEngine = SpaceInvadersGameEngine()
    val gameEngine: SpaceInvadersGameEngine get() = _gameEngine

    private val _tick = mutableStateOf(0)
    val tick: State<Int> = _tick

    init {
        startGameLoop()
    }

    private fun startGameLoop() {
        viewModelScope.launch {
            while (true) {
                delay(16L) // ~60 FPS
                _gameEngine.updateGame()
                _tick.value++ // Trigger UI recomposition
            }
        }
    }
}
