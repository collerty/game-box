package com.example.gamehub.features.spaceinvaders.classes

data class Player(var x: Float, val y: Float)

data class Enemy(var x: Float, var y: Float, var isAlive: Boolean = true, var type: EnemyType)

enum class EnemyType { SHOOTER, BOTTOM, MIDDLE }

data class Bullet(
    var x: Float,
    var y: Float,
    val speed: Float = 15f,
    var isActive: Boolean = true
)
