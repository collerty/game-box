package com.example.gamehub.features.spy.ui

import kotlin.random.Random

class SpyGameState(private val settings: SpyGameSettings) {
    private var currentLocation: String = ""
    private var spyIndices: List<Int> = emptyList()
    private var currentPlayerIndex: Int = 0
    private var timeRemaining: Int = 0
    private var isGameActive: Boolean = false

    fun startGame() {
        // Select random location
        currentLocation = settings.selectedLocations.random()
        
        // Select random spies
        spyIndices = List(settings.numberOfSpies) {
            Random.nextInt(settings.numberOfPlayers)
        }.distinct()
        
        currentPlayerIndex = 0
        timeRemaining = settings.timerMinutes * 60
        isGameActive = true
    }

    fun getPlayerRole(playerIndex: Int): String {
        return if (spyIndices.contains(playerIndex)) {
            "You are the Spy"
        } else {
            "You are at $currentLocation"
        }
    }

    fun nextPlayer() {
        if (currentPlayerIndex < settings.numberOfPlayers - 1) {
            currentPlayerIndex++
        }
    }

    fun updateTimer(seconds: Int) {
        timeRemaining = seconds
    }

    fun isGameOver(): Boolean {
        return timeRemaining <= 0
    }

    fun getCurrentPlayerIndex(): Int = currentPlayerIndex
    fun getTimeRemaining(): Int = timeRemaining
    fun isGameActive(): Boolean = isGameActive
    fun getCurrentLocation(): String = currentLocation
} 