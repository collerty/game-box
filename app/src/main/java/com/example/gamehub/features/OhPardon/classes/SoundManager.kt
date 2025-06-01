package com.example.gamehub.features.OhPardon.classes

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
        soundMap["move_self"] = soundPool.load(context, R.raw.move_self, 1)
        soundMap["diceroll"] = soundPool.load(context, R.raw.diceroll, 1)
        soundMap["illegal"] = soundPool.load(context, R.raw.illegal, 1)
        soundMap["capture"] = soundPool.load(context, R.raw.capture, 1)
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
