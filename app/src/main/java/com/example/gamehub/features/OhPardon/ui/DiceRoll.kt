package com.example.gamehub.features.OhPardon

import kotlin.random.Random

class DiceRoll(private val sides: Int = 6) {
    fun roll(): Int = Random.nextInt(1, sides + 1)
}