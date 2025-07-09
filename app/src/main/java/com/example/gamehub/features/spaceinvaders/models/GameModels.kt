package com.example.gamehub.features.spaceinvaders.models

// Base interface for game entities
interface GameEntity {
    val x: Float
    val y: Float
    val width: Float
    val height: Float
    val isActive: Boolean
}

// Player entity
data class Player(
    override var x: Float,
    override val y: Float,
    override val width: Float = 100f,
    override val height: Float = 30f,
    override val isActive: Boolean = true
) : GameEntity

// Enemy entity
data class Enemy(
    override var x: Float,
    override var y: Float,
    var isAlive: Boolean = true,
    var type: EnemyType,
    override val width: Float = 60f,
    override val height: Float = 40f
) : GameEntity {
    override val isActive: Boolean
        get() = isAlive
}

enum class EnemyType { SHOOTER, BOTTOM, MIDDLE }

enum class EnemyDirection {
    LEFT,
    RIGHT
}


// UFO entity
data class UFO(
    override var x: Float,
    override var y: Float,
    override val width: Float = 80f,
    override val height: Float = 40f,
    override var isActive: Boolean = false,
    var speed: Float = 8f,
    var direction: EnemyDirection = EnemyDirection.RIGHT
) : GameEntity

// Score data class
data class PlayerScore(
    val player: String = "",
    val score: Int = 0
)

// Bunker entity
data class Bunker(
    val id: Int,
    override var x: Float,
    override var y: Float,
    override val width: Float,
    override val height: Float,
    var health: Int
) : GameEntity {
    override val isActive: Boolean
        get() = health > 0

    fun isHit(projectileX: Float, projectileY: Float): Boolean {
        return projectileX in x..(x + width) && projectileY in y..(y + height)
    }

    fun takeDamage() {
        health--
    }

    fun isDestroyed(): Boolean = health <= 0
}

// Bullet entity
data class Bullet(
    override var x: Float,
    override var y: Float,
    override val width: Float = 10f,
    override val height: Float = 20f,
    val speed: Float = 15f,
    override var isActive: Boolean = true
) : GameEntity

enum class GameState {
    PLAYING,
    GAME_OVER
}

