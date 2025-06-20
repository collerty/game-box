package com.example.gamehub.lobby.model

data class Move(val x: Int, val y: Int, val playerId: String) {
    fun toMap(): Map<String, Any> = mapOf(
        "x" to x,
        "y" to y,
        "playerId" to playerId
    )

    companion object {
        fun fromMap(map: Map<String, Any?>) = Move(
            x = (map["x"] as Number).toInt(),
            y = (map["y"] as Number).toInt(),
            playerId = map["playerId"] as String
        )
    }
}
