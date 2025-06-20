package com.example.gamehub.features.JorisJump.model

// Data class for enemy state (to be implemented)
data class EnemyState(
    val id: Int,
    var x: Float, // World X Dp from left
    var y: Float, // World Y Dp from top
    var visualOffsetX: Float = 0f, // For twitching animation
    var visualOffsetY: Float = 0f  // For twitching animation
) 