package com.example.gamehub.ui

import android.app.Application
import androidx.lifecycle.*
import com.example.gamehub.data.VolumePreferences
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = VolumePreferences(application)

    val musicVolume = preferences.musicVolumeFlow.asLiveData()
    val soundVolume = preferences.soundVolumeFlow.asLiveData()

    fun setMusicVolume(value: Float) {
        viewModelScope.launch { preferences.setMusicVolume(value) }
    }

    fun setSoundVolume(value: Float) {
        viewModelScope.launch { preferences.setSoundVolume(value) }
    }
}
