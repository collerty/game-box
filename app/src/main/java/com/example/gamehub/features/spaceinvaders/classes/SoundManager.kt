package com.example.gamehub.features.spaceinvaders.classes

import android.content.Context
import android.media.SoundPool
import androidx.annotation.RawRes
import com.example.gamehub.R

class SoundManager(context: Context) {

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(5)
        .build()

    private val soundMap = mutableMapOf<String, Int>()

    init {
        // Preload your sounds here
        soundMap["shoot"] = soundPool.load(context, R.raw.shoot, 1)
        soundMap["take_damage"] = soundPool.load(context, R.raw.take_damage, 1)
        soundMap["explode"] = soundPool.load(context, R.raw.explode, 1)
        soundMap["ufo"] = soundPool.load(context, R.raw.ufo, 1)
        // Add more sounds as needed
    }

    fun playSound(name: String) {
        soundMap[name]?.let { soundId ->
            soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }

    fun release() {
        soundPool.release()
    }
}