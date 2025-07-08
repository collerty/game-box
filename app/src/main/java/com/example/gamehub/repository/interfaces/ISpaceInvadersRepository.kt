package com.example.gamehub.repository.interfaces

import com.example.gamehub.features.spaceinvaders.models.PlayerScore
import kotlinx.coroutines.flow.StateFlow

interface ISpaceInvadersRepository {
    val highScores: StateFlow<List<PlayerScore>>
    val playerName: StateFlow<String>

    fun onPlayerNameChanged(newName: String)
    fun fetchHighScores()
    fun submitScore(playerName: String, newScore: Int)
}

