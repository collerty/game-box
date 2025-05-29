package com.example.gamehub.features.spaceinvaders.classes

import android.util.Log
import androidx.compose.runtime.*

class SpaceInvadersGameEngine {
    var player by mutableStateOf(Player(x = 0f, y = 0f))

    val playerWidth = 100f
    var isMovingLeft = false
    var isMovingRight = false

    private val moveSpeed = 10f
    var screenWidthPx: Float = 0f // Set this from Composable

    private fun moveLeft(step: Float) {
        player = player.copy(x = (player.x - step).coerceAtLeast(0f))
    }

    private fun moveRight(step: Float) {
        player = player.copy(x = (player.x + step).coerceAtMost(screenWidthPx - playerWidth))
    }

    fun updateGame() {
        if (isMovingLeft) moveLeft(moveSpeed)
        if (isMovingRight) moveRight(moveSpeed)
    }

}
