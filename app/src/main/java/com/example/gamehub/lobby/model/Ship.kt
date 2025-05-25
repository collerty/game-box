package com.example.gamehub.lobby.model

import com.example.gamehub.features.battleships.ui.Orientation

data class Ship(
    val startRow: Int,
    val startCol: Int,
    val size: Int,
    val orientation: Orientation
) {
    fun toMap(): Map<String, Any> = mapOf(
        "startRow" to startRow,
        "startCol" to startCol,
        "size" to size,
        "orientation" to orientation.name
    )

    companion object {
        fun fromMap(map: Map<String, Any?>) = Ship(
            startRow = (map["startRow"] as Number).toInt(),
            startCol = (map["startCol"] as Number).toInt(),
            size = (map["size"] as Number).toInt(),
            orientation = Orientation.valueOf(map["orientation"] as String)
        )
    }
}
