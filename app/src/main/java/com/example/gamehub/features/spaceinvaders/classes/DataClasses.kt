package com.example.gamehub.features.spaceinvaders.classes

data class Player(var x: Float, val y: Float)

data class Enemy(var x: Float, var y: Float, var isAlive: Boolean = true, var type: EnemyType)

enum class EnemyType { SHOOTER, BOTTOM, MIDDLE}

data class UFO(
    var x: Float,
    var y: Float,
    var width: Float = 80f,
    var height: Float = 40f,
    var isActive: Boolean = false,
    var speed: Float = 8f,
    var direction: Int = 1, // 1 = right, -1 = left
)


data class PlayerScore(
    val player: String = "",
    val score: Int = 0
)

data class Bunker(
    val id: Int,
    var x: Float,
    var y: Float,
    val width: Float,
    val height: Float,
    var health: Int
) {
    fun isHit(projectileX: Float, projectileY: Float): Boolean {
        return projectileX in x..(x + width) && projectileY in y..(y + height)
    }

    fun takeDamage() {
        health--
    }

    fun isDestroyed(): Boolean = health <= 0
}


data class Bullet(
    var x: Float,
    var y: Float,
    val speed: Float = 15f,
    var isActive: Boolean = true
)

enum class GameState {
    PLAYING,
    GAME_OVER
}
