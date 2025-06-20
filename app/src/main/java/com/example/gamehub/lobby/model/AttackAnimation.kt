package com.example.gamehub.lobby.model

data class AttackAnimation(
    val x: Int,
    val y: Int,
    val playerId: String,
    val startedAt: Long // unix timestamp
)