package com.example.gamehub.features.spaceinvaders.controllers

import com.example.gamehub.features.spaceinvaders.util.AudioManager
import com.example.gamehub.features.spaceinvaders.models.UFO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.random.Random

class UFOController(
    private val audioManager: AudioManager,
    private val coroutineScope: CoroutineScope
) {
    var ufo: UFO = UFO(x = -100f, y = 50f, isActive = false)
    private var ufoSpawnCooldown = 0

    fun updateUFO(screenWidth: Float) {
        if (ufo.isActive) {
            ufo.x += ufo.speed * ufo.direction

            // Deactivate when off-screen
            if ((ufo.direction == 1 && ufo.x > screenWidth) ||
                (ufo.direction == -1 && ufo.x + ufo.width < 0)) {
                ufo.isActive = false
            }
        } else {
            // Maybe spawn UFO randomly
            if (ufoSpawnCooldown <= 0 && Random.nextFloat() < 0.005f) { // 0.5% chance per frame
                ufo.direction = if (Random.nextBoolean()) 1 else -1
                ufo.x = if (ufo.direction == 1) -ufo.width else screenWidth
                ufo.y = 50f
                ufo.isActive = true
                ufoSpawnCooldown = 600 // ~10 seconds if 60 FPS
                coroutineScope.launch {
                    audioManager.playUFOSound()
                    audioManager.vibrate()
                }
            } else {
                ufoSpawnCooldown--
            }
        }
    }
}

