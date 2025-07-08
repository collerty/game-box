package com.example.gamehub.features.spaceinvaders.controllers

import android.util.Log
import com.example.gamehub.features.spaceinvaders.models.Bunker

class BunkerController {
    private val bunkers = mutableListOf<Bunker>()
    private var initialized = false

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
                    width = 200f,
                    height = 100f,
                    health = 15
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

