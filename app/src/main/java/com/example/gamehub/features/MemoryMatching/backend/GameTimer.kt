package com.example.gamehub.features.MemoryMatching.backend

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameTimer(private val scope: CoroutineScope) {

    private val _timeLeftInSeconds = MutableStateFlow(0)
    val timeLeftInSeconds = _timeLeftInSeconds.asStateFlow()

    private var timerJob: Job? = null

    fun start(startTimeInSeconds: Int) {
        _timeLeftInSeconds.value = startTimeInSeconds
        timerJob?.cancel()
        timerJob = scope.launch {
            while (_timeLeftInSeconds.value > 0) {
                delay(1000)
                _timeLeftInSeconds.value--
            }
        }
    }

    fun stop() {
        timerJob?.cancel()
    }

    fun isRunning(): Boolean = timerJob?.isActive ?: false
}

