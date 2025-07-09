// SHARED SOUND MANAGER SINGLETON FOR THE WHOLE APP
package com.example.gamehub.audio

import android.content.Context
import android.media.MediaPlayer

object SoundManager {
    var soundVolume: Float = 1.0f

    fun playEffect(context: Context, soundResId: Int) {
        val player = MediaPlayer.create(context, soundResId)
        player.setVolume(soundVolume, soundVolume)
        player.setOnCompletionListener { it.release() }
        player.start()
    }
}