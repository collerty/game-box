package com.example.gamehub

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder

class MusicService : Service() {

    private var mediaPlayer: MediaPlayer? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Replace with your own music file in res/raw/background_music.mp3
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.triviatoe_background_music)
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
