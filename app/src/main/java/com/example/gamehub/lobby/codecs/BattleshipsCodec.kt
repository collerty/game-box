package com.example.gamehub.lobby.codec

import com.example.gamehub.lobby.GameCodec
import com.google.firebase.firestore.FieldValue

object BattleshipsCodec : GameCodec<BattleshipsMove, BattleshipsState> {

    override fun encodeMove(move: BattleshipsMove): Map<String, Any> = mapOf(
        "gameState.battleships.moves" to FieldValue.arrayUnion(move.position),
        "gameState.battleships.currentTurn" to move.playerUid
    )

    override fun decodeState(snapshot: Map<String, Any?>): BattleshipsState {
        val gameData = snapshot["gameState"] as? Map<*, *> ?: emptyMap<Any,Any>()
        val bsData   = (gameData["battleships"] as? Map<*, *>) ?: emptyMap<Any,Any>()

        val currentTurn = bsData["currentTurn"] as? String ?: ""
        val moves       = bsData["moves"]       as? List<String> ?: emptyList()
        val gameResult  = (bsData["gameResult"] as? Map<*, *>)?.let {
            GameResult(
                winner = it["winner"] as? String ?: "",
                loser  = it["loser"]  as? String ?: "",
                reason = it["reason"] as? String ?: ""
            )
        }

        // Parse mapVotes
        val rawVotes = bsData["mapVotes"] as? Map<*, *>
        val mapVotes = rawVotes
            ?.mapNotNull { (k, v) ->
                val key    = k as? String
                val intVal = (v as? Number)?.toInt()
                if (key != null && intVal != null) key to intVal else null
            }
            ?.toMap()
            ?: emptyMap()

        // Parse chosenMap
        val chosenMap = (bsData["chosenMap"] as? Number)?.toInt()

        return BattleshipsState(
            currentTurn = currentTurn,
            moves       = moves,
            gameResult  = gameResult,
            mapVotes    = mapVotes,
            chosenMap   = chosenMap
        )
    }
}
