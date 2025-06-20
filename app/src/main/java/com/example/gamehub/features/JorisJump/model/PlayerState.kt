package com.example.gamehub.features.JorisJump.model

// Data class for player state (to be implemented)
data class PlayerState(
    var xScreenDp: Float = 0f,
    var yWorldDp: Float = 0f,
    var velocityY: Float = 0f,
    var isFallingOffScreen: Boolean = false
) 