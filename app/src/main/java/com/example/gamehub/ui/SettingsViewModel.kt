package com.example.gamehub.ui

import android.app.Application
import androidx.lifecycle.*
import com.example.gamehub.data.VolumePreferences
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = VolumePreferences(application)

    val musicVolume = preferences.musicVolumeFlow.asLiveData()
    val soundVolume = preferences.soundVolumeFlow.asLiveData()

    private var previousMusicVolume = 0.5f
    private var previousSoundVolume = 0.5f

    fun setMusicVolume(value: Float) {
        if (value > 0f) previousMusicVolume = value
        viewModelScope.launch { preferences.setMusicVolume(value) }
    }

    fun setSoundVolume(value: Float) {
        if (value > 0f) previousSoundVolume = value
        viewModelScope.launch { preferences.setSoundVolume(value) }
    }

    fun toggleMuteMusic(currentVolume: Float) {
        if (currentVolume > 0f) {
            previousMusicVolume = currentVolume
            setMusicVolume(0f)
        } else {
            setMusicVolume(previousMusicVolume)
        }
    }

    fun toggleMuteSound(currentVolume: Float) {
        if (currentVolume > 0f) {
            previousSoundVolume = currentVolume
            setSoundVolume(0f)
        } else {
            setSoundVolume(previousSoundVolume)
        }
    }
}
