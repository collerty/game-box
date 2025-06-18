package com.example.gamehub.features.ohpardon.models

import androidx.compose.ui.graphics.Color
import com.google.firebase.Timestamp

data class GameRoom(
    val gameId: String,
    val name: String,
    val hostUid: String,
    val hostName: String,
    val passwordHash: Int?,
    val maxPlayers: Int,
    val status: String = "waiting", // waiting, playing, finished
    val players: List<Player>,
    val gameState: GameState,
    val createdAt: Timestamp?
)

data class ColorConfig(
    val homePositions: List<Pair<Int, Int>>,
    val goalPath: List<Pair<Int, Int>>,
    val entryCell: Pair<Int, Int>
)

data class GameState(
    val currentTurnUid: String,
    val diceRoll: Int? = null,
    val gameResult: String = "Ongoing"
)

data class Player(
    val uid: String,
    val name: String,
    val color: Color,
    val pawns: List<Pawn>
)

data class Pawn(
    val id: String,
    var position: Int = -1 // -1 means at home
)

sealed class UiEvent {
    object PlayMoveSound : UiEvent()
    object PlayDiceRollSound: UiEvent()
    object PlayCaptureSound: UiEvent()
    object PlayIllegalMoveSound: UiEvent()
    object Vibrate : UiEvent()
}
