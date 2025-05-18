package com.example.gamehub.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow

private val Context.dataStore by preferencesDataStore(name = "settings")

object VolumeKeys {
    val MUSIC_VOLUME = floatPreferencesKey("music_volume")
    val SOUND_VOLUME = floatPreferencesKey("sound_volume")
}

class VolumePreferences(private val context: Context) {
    val musicVolumeFlow: Flow<Float> = context.dataStore.data
        .map { it[VolumeKeys.MUSIC_VOLUME] ?: 0.5f }

    val soundVolumeFlow: Flow<Float> = context.dataStore.data
        .map { it[VolumeKeys.SOUND_VOLUME] ?: 0.5f }

    suspend fun setMusicVolume(value: Float) {
        context.dataStore.edit { it[VolumeKeys.MUSIC_VOLUME] = value }
    }

    suspend fun setSoundVolume(value: Float) {
        context.dataStore.edit { it[VolumeKeys.SOUND_VOLUME] = value }
    }
}
