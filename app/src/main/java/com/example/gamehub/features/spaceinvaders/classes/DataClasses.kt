package com.example.gamehub.features.spaceinvaders.classes

data class Player(var x: Float, val y: Float)

data class Enemy(var x: Int, var y: Int, var isAlive: Boolean = true, var type: EnemyType)

enum class EnemyType { SHOOTER, BOTTOM, MIDDLE }
