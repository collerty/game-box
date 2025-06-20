package com.example.gamehub.features.JorisJump.model

// Data class for platform state (to be implemented)
data class PlatformState(
    val id: Int,
    var x: Float,
    var y: Float,
    val isMoving: Boolean = false,
    var movementDirection: Int = 1,
    val movementSpeed: Float = 1.0f,
    val movementRange: Float = 50f,
    val originX: Float,
    val hasSpring: Boolean = false,
    val springJumpFactor: Float = 1.65f
) 