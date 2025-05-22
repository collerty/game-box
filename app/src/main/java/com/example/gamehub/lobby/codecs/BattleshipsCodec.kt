package com.example.gamehub.lobby.codec

import com.example.gamehub.lobby.model.GameSession
import com.example.gamehub.lobby.model.Move
import com.example.gamehub.lobby.model.Ship
import com.example.gamehub.lobby.model.PowerUp

object BattleshipsCodec {
    fun encode(session: GameSession): Map<String, Any?> = mapOf(
        "gameId"           to session.gameId,
        "player1Id"        to session.player1Id,
        "player2Id"        to session.player2Id,
        "currentTurn"      to session.currentTurn,
        "moves"            to session.moves.map { it.toMap() },
        "ships"            to session.ships.mapValues { (_, ships) -> ships.map { it.toMap() } },
        "powerUps"         to session.availablePowerUps.mapValues { (_, ups) -> ups.map { it.name } }
    )

    fun decode(data: Map<String, Any?>): GameSession {
        val gameId      = data["gameId"]      as? String ?: ""
        val p1          = data["player1Id"]   as? String ?: ""
        val p2          = data["player2Id"]   as? String?
        val current     = data["currentTurn"] as? String ?: ""

        val rawMoves    = data["moves"]       as? List<Map<String, Any?>> ?: emptyList()
        val moves       = rawMoves.map { Move.fromMap(it) }

        val rawShips    = data["ships"]       as? Map<String, List<Map<String, Any?>>>
            ?: emptyMap()
        val ships       = rawShips.mapValues { it.value.map { Ship.fromMap(it) } }

        val rawPUs      = data["powerUps"]    as? Map<String, List<String>>
            ?: emptyMap()
        val powerUps    = rawPUs.mapValues { it.value.map { PowerUp.valueOf(it) } }

        return GameSession(
            gameId           = gameId,
            player1Id        = p1,
            player2Id        = p2,
            currentTurn      = current,
            moves            = moves,
            ships            = ships,
            availablePowerUps= powerUps
        )
    }
}
