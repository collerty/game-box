package com.example.gamehub.features.spaceinvaders.classes

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

    private var lastShotTime = 0L
    private val fireCooldown = 500L // milliseconds between shots


    fun shootBullet() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastShotTime >= fireCooldown) {
            val bulletX = player.x + 50f - 5f // center of player
            val bulletY = player.y - 10f
            playerBullets.add(Bullet(x = bulletX, y = bulletY))
            lastShotTime = currentTime
        }
    }


    fun updateBullets(screenHeight: Float) {
        playerBullets.forEach { bullet ->
            bullet.y -= bullet.speed
            if (bullet.y < 0) bullet.isActive = false
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
        player = player.copy(x = (player.x - step).coerceAtLeast(0f))
    }

    private fun moveRight(step: Float) {
        player = player.copy(x = (player.x + step).coerceAtMost(screenWidthPx - playerWidth))
    }
}
