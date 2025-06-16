package com.example.gamehub.features.spaceinvaders.classes

import android.util.Log
import com.example.gamehub.features.spaceinvaders.classes.SpaceInvadersViewModel.EventBus
import com.example.gamehub.features.spaceinvaders.classes.SpaceInvadersViewModel.UiEvent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PlayerController(
    private var player: Player,
    private val playerWidth: Float,
    private val moveSpeed: Float
) {
    var isMovingLeft = false
    var isMovingRight = false
    var screenWidthPx: Float = 0f
    var screenHeightPx: Float = 0f
    val playerBullets = mutableListOf<Bullet>()
    private val screenPadding = 300f
    private var lastShotTime = 0L
    private val fireCooldown = 500L // milliseconds between shots


    fun shootBullet() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastShotTime >= fireCooldown) {
            val bulletX = player.x + 50f - 5f // center of player
            val bulletY = player.y - 10f
            playerBullets.add(Bullet(x = bulletX, y = bulletY))
            lastShotTime = currentTime
            GlobalScope.launch {
                EventBus.emit(UiEvent.PlayShootSound)
                EventBus.emit(UiEvent.Vibrate)
            }
        }
    }

    fun updateFromTilt(xTilt: Float) {
        when {
            xTilt > 2 -> moveLeft(moveSpeed)
            xTilt < -2 -> moveRight(moveSpeed)
        }
    }


    fun updateBullets(screenHeight: Float) {
        playerBullets.forEach { bullet ->
            bullet.y -= bullet.speed
            if (bullet.y < 0 || bullet.y > screenHeight) bullet.isActive = false
        }


        // Remove inactive bullets
        playerBullets.removeAll { !it.isActive }
    }


    fun updatePlayerMovement() {
        if (isMovingLeft) moveLeft(moveSpeed)
        if (isMovingRight) moveRight(moveSpeed)
    }

    fun getPlayer(): Player = player

    fun setPlayer(player: Player) {
        this.player = player
    }


    private fun moveLeft(step: Float) {
        player = player.copy(x = (player.x - step).coerceAtLeast(screenPadding))
    }

    private fun moveRight(step: Float) {
        player = player.copy(
            x = (player.x + step).coerceAtMost(screenWidthPx - playerWidth - screenPadding)
        )
    }

}
