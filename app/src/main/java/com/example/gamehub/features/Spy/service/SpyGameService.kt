package com.example.gamehub.features.spy.service

import android.os.CountDownTimer
import com.example.gamehub.features.spy.model.LocationManager
import com.example.gamehub.features.spy.model.SpyGameSettings
import com.example.gamehub.features.spy.model.SpyGameState
import com.example.gamehub.features.spy.model.DEFAULT_NUMBER_OF_PLAYERS
import com.example.gamehub.features.spy.model.DEFAULT_NUMBER_OF_SPIES
import com.example.gamehub.features.spy.model.DEFAULT_TIMER_MINUTES

const val TIMER_TICK_MILLIS = 1000L

class SpyGameService(private val locationManager: LocationManager) {
    var gameSettings: SpyGameSettings = SpyGameSettings()
    var gameState: SpyGameState = SpyGameState(gameSettings)
    private var timer: CountDownTimer? = null
    private var timerListener: ((Int) -> Unit)? = null
    private var timerFinishListener: (() -> Unit)? = null

    fun setupGameSettings() {
        val locations = locationManager.getLocations().map { it.name }
        gameSettings = SpyGameSettings(
            numberOfPlayers = DEFAULT_NUMBER_OF_PLAYERS,
            numberOfSpies = DEFAULT_NUMBER_OF_SPIES,
            timerMinutes = DEFAULT_TIMER_MINUTES,
            selectedLocations = locations
        )
        gameState = SpyGameState(gameSettings)
    }

    fun startGame() {
        gameState = SpyGameState(gameSettings)
        gameState.startGame()
    }

    fun getPlayerRole(playerIndex: Int): String {
        return gameState.getPlayerRole(playerIndex)
    }

    fun nextPlayer() {
        gameState.nextPlayer()
    }

    fun startTimer(onTick: (Int) -> Unit, onFinish: () -> Unit) {
        timerListener = onTick
        timerFinishListener = onFinish
        timer = object : CountDownTimer(gameSettings.timerMinutes * 60 * TIMER_TICK_MILLIS, TIMER_TICK_MILLIS) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000).toInt()
                gameState.updateTimer(seconds)
                timerListener?.invoke(seconds)
            }
            override fun onFinish() {
                gameState.updateTimer(0)
                timerFinishListener?.invoke()
            }
        }.start()
    }

    fun cancelTimer() {
        timer?.cancel()
        timer = null
    }

    fun getTimeRemaining(): Int = gameState.getTimeRemaining()
    fun isGameOver(): Boolean = gameState.isGameOver()
    fun isGameActive(): Boolean = gameState.isGameActive()
    fun getCurrentLocation(): String = gameState.getCurrentLocation()

    // --- Logic moved from Activity ---
    var currentPlayerIndex = 0
    var isRoleRevealed = false
    var allRolesRevealed = false

    fun canStartGame(): Boolean {
        return gameSettings.selectedLocations.isNotEmpty() &&
                gameSettings.numberOfSpies < gameSettings.numberOfPlayers
    }

    fun resetGame() {
        cancelTimer()
        setupGameSettings()
        currentPlayerIndex = 0
        isRoleRevealed = false
        allRolesRevealed = false
    }

    fun revealRole(): String {
        isRoleRevealed = true
        return getPlayerRole(currentPlayerIndex)
    }

    fun advancePlayer(): Boolean {
        if (currentPlayerIndex < gameSettings.numberOfPlayers - 1) {
            currentPlayerIndex++
            isRoleRevealed = false
            return true
        } else {
            allRolesRevealed = true
            return false
        }
    }

    fun getPlayerCardInfo(): PlayerCardInfo {
        return PlayerCardInfo(
            playerNumber = currentPlayerIndex + 1,
            isRoleRevealed = isRoleRevealed,
            role = if (isRoleRevealed) getPlayerRole(currentPlayerIndex) else null
        )
    }

    data class PlayerCardInfo(val playerNumber: Int, val isRoleRevealed: Boolean, val role: String?)

    // Settings update helpers
    fun updateNumberOfPlayers(newValue: Int) {
        gameSettings.numberOfPlayers = newValue
    }
    fun updateNumberOfSpies(newValue: Int) {
        gameSettings.numberOfSpies = newValue
    }
    fun updateTimerMinutes(newValue: Int) {
        gameSettings.timerMinutes = newValue
    }
    fun updateLocations(newLocations: List<String>) {
        gameSettings.selectedLocations = newLocations
    }
    fun getSettingsSummary(): SettingsSummary {
        return SettingsSummary(
            players = gameSettings.numberOfPlayers,
            spies = gameSettings.numberOfSpies,
            timer = gameSettings.timerMinutes,
            locations = gameSettings.selectedLocations.size
        )
    }
    data class SettingsSummary(val players: Int, val spies: Int, val timer: Int, val locations: Int)

    // Location management helpers
    fun addLocation(name: String, description: String) {
        locationManager.addLocation(com.example.gamehub.features.spy.model.Location(name, description))
        updateLocations(locationManager.getLocations().map { it.name })
    }
    fun updateLocation(old: com.example.gamehub.features.spy.model.Location, new: com.example.gamehub.features.spy.model.Location) {
        locationManager.updateLocation(old, new)
        updateLocations(locationManager.getLocations().map { it.name })
    }
    fun removeLocation(location: com.example.gamehub.features.spy.model.Location) {
        locationManager.removeLocation(location)
        updateLocations(locationManager.getLocations().map { it.name })
    }
    fun getLocations() = locationManager.getLocations()
} 