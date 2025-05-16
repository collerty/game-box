package com.example.gamehub.lobby.codec

import com.example.gamehub.lobby.GameCodec
import com.google.firebase.firestore.FieldValue

// Top-level data classes
data class BattleshipsMove(val position: String, val playerUid: String)

data class BattleshipsState(
    val currentTurn: String,
    val moves: List<String>,
    val gameResult: GameResult?
)

data class GameResult(val winner: String, val loser: String, val reason: String)

// Codec object for encoding/decoding only
object BattleshipsCodec : GameCodec<BattleshipsMove, BattleshipsState> {

    override fun encodeMove(move: BattleshipsMove): Map<String, Any> = mapOf(
        "gameState.battleships.moves" to FieldValue.arrayUnion(move.position),
        "gameState.battleships.currentTurn" to move.playerUid
    )

    override fun decodeState(snapshot: Map<String, Any?>): BattleshipsState {
        val gameData = snapshot["gameState"] as? Map<*, *>
        val bsData = gameData?.get("battleships") as? Map<*, *> ?: emptyMap<String, Any>()
        return BattleshipsState(
            currentTurn = bsData["currentTurn"] as? String ?: "",
            moves = bsData["moves"] as? List<String> ?: emptyList(),
            gameResult = (bsData["gameResult"] as? Map<*, *>)?.let {
                GameResult(
                    winner = it["winner"] as? String ?: "",
                    loser = it["loser"] as? String ?: "",
                    reason = it["reason"] as? String ?: ""
                )
            }
        )
    }
}
