package com.example.gamehub.features.spaceinvaders.controllers

import android.util.Log
import com.example.gamehub.features.spaceinvaders.models.Bunker

class BunkerController {
    private val bunkers = mutableListOf<Bunker>()
    private var initialized = false
    private val bunkerHealth = 15
    private val bunkerWidth = 200f
    private val bunkerHeight = 100f

    fun getBunkers(): List<Bunker> = bunkers

    fun initializeBunkers(screenWidth: Float, screenHeight: Float) {
        if (initialized) return

        bunkers.clear()
        val bunkerCount = 3
        val spacing = screenWidth / (bunkerCount + 1)
        val y = screenHeight - 400f

        for (i in 0 until bunkerCount) {
            val x = spacing * (i + 1) - 40f
            bunkers.add(
                Bunker(
                    id = i,
                    x = x,
                    y = y,
                    width = bunkerWidth,
                    height = bunkerHeight,
                    health = bunkerHealth
                )
            )
            Log.d("bunker id", "Bunker ID: ${i}, X: $x, Y: $y")
        }

        initialized = true
    }

    fun reset() {
        initialized = false
        bunkers.clear()
    }

    fun removeDestroyedBunkers() {
        bunkers.removeAll { it.isDestroyed() }
    }
}

