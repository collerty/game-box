package com.example.gamehub.features.spy.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.gamehub.features.spy.model.Location
import com.example.gamehub.features.spy.model.LocationManager
import com.example.gamehub.features.spy.service.SpyGameService
import com.example.gamehub.features.spy.service.SpyGameService.PlayerCardInfo
import com.example.gamehub.features.spy.service.SpyGameService.SettingsSummary

enum class GamePhase { SETTINGS, REVEAL, TIMER, GAME_OVER }

class SpyGameViewModel(private val locationManager: LocationManager) : ViewModel() {
    private val service = SpyGameService(locationManager)

    private val _gamePhase = MutableLiveData(GamePhase.SETTINGS)
    val gamePhase: LiveData<GamePhase> = _gamePhase

    private val _playerCardInfo = MutableLiveData<PlayerCardInfo>()
    val playerCardInfo: LiveData<PlayerCardInfo> = _playerCardInfo

    private val _settingsSummary = MutableLiveData<SettingsSummary>()
    val settingsSummary: LiveData<SettingsSummary> = _settingsSummary

    private val _timer = MutableLiveData<Int>()
    val timer: LiveData<Int> = _timer

    private val _gameOver = MutableLiveData<Boolean>()
    val gameOver: LiveData<Boolean> = _gameOver

    private val _locations = MutableLiveData<List<Location>>()
    val locations: LiveData<List<Location>> = _locations

    val currentPlayerIndex: Int get() = service.currentPlayerIndex
    val numberOfPlayers: Int get() = service.gameSettings.numberOfPlayers

    init {
        service.setupGameSettings()
        updateAll()
    }

    fun startGame() {
        if (!service.canStartGame()) return
        service.startGame()
        _gamePhase.value = GamePhase.REVEAL
        updatePlayerCard()
    }

    fun revealRole() {
        service.revealRole()
        updatePlayerCard()
    }

    fun advancePlayer() {
        if (service.advancePlayer()) {
            updatePlayerCard()
        } else {
            _gamePhase.value = GamePhase.TIMER
            startTimer()
        }
    }

    fun startTimer() {
        service.startTimer(
            onTick = { seconds -> _timer.postValue(seconds) },
            onFinish = { _timer.postValue(0); _gameOver.postValue(true); _gamePhase.postValue(GamePhase.GAME_OVER) }
        )
    }

    fun resetGame() {
        service.resetGame()
        _gamePhase.value = GamePhase.SETTINGS
        updateAll()
    }

    fun updateNumberOfPlayers(newValue: Int) {
        service.updateNumberOfPlayers(newValue)
        updateSettingsSummary()
    }
    fun updateNumberOfSpies(newValue: Int) {
        service.updateNumberOfSpies(newValue)
        updateSettingsSummary()
    }
    fun updateTimerMinutes(newValue: Int) {
        service.updateTimerMinutes(newValue)
        updateSettingsSummary()
    }
    fun addLocation(name: String, description: String) {
        service.addLocation(name, description)
        updateLocations()
        updateSettingsSummary()
    }
    fun updateLocation(old: Location, new: Location) {
        service.updateLocation(old, new)
        updateLocations()
        updateSettingsSummary()
    }
    fun removeLocation(location: Location) {
        service.removeLocation(location)
        updateLocations()
        updateSettingsSummary()
    }

    private fun updateAll() {
        updatePlayerCard()
        updateSettingsSummary()
        updateLocations()
    }
    private fun updatePlayerCard() {
        _playerCardInfo.postValue(service.getPlayerCardInfo())
    }
    private fun updateSettingsSummary() {
        _settingsSummary.postValue(service.getSettingsSummary())
    }
    private fun updateLocations() {
        _locations.postValue(service.getLocations())
    }

    override fun onCleared() {
        super.onCleared()
        service.cancelTimer()
    }
}

class SpyGameViewModelFactory(private val locationManager: LocationManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SpyGameViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SpyGameViewModel(locationManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 