package com.example.gamehub.features.spaceinvaders.util

import com.example.gamehub.features.spaceinvaders.classes.SpaceInvadersViewModel

interface AudioManager {
    suspend fun playExplodeSound()
    suspend fun playShootSound()
    suspend fun playTakeDamageSound()
    suspend fun playUFOSound()
    suspend fun vibrate()
}

class EventBusAudioManager(
    private val eventEmitter: suspend (SpaceInvadersViewModel.UiEvent) -> Unit
) : AudioManager {
    override suspend fun playExplodeSound() {
        eventEmitter(SpaceInvadersViewModel.UiEvent.PlayExplodeSound)
    }

    override suspend fun playShootSound() {
        eventEmitter(SpaceInvadersViewModel.UiEvent.PlayShootSound)
    }

    override suspend fun playTakeDamageSound() {
        eventEmitter(SpaceInvadersViewModel.UiEvent.PlayTakeDamageSound)
    }

    override suspend fun playUFOSound() {
        eventEmitter(SpaceInvadersViewModel.UiEvent.PlayUFOSound)
    }

    override suspend fun vibrate() {
        eventEmitter(SpaceInvadersViewModel.UiEvent.Vibrate)
    }
}

