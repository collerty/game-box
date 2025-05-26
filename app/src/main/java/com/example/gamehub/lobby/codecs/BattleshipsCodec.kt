package com.example.gamehub.lobby.codec

import com.example.gamehub.features.battleships.model.Cell
import com.example.gamehub.lobby.model.GameSession
import com.example.gamehub.lobby.model.Move
import com.example.gamehub.lobby.model.Ship
import com.example.gamehub.lobby.model.PowerUp
import com.google.firebase.firestore.FieldValue

object BattleshipsCodec {
    /** Encode the full GameSession into Firestore data */
    fun encode(session: GameSession): Map<String, Any?> = mapOf(
        "gameId"           to session.gameId,
        "player1Id"        to session.player1Id,
        "player2Id"        to session.player2Id,
        "currentTurn"      to session.currentTurn,
        "moves"            to session.moves.map { it.toMap() },
        "ships"            to session.ships.mapValues { (_, ships) -> ships.map { it.toMap() } },
        "powerUps"         to session.availablePowerUps.mapValues { (_, ups) -> ups.map { it.name } },

        // ← NEW: vote state
        "mapVotes"         to session.mapVotes,          // Map<String,Int>
        "chosenMap"        to session.chosenMap,         // Int?

        // ← NEW: energy pool
        "energy"           to session.energy,            // Map<String,Int>

        // ← OPTIONAL: record of power-up moves (list of "PU_NAME:x,y" strings)
        "powerUpMoves"     to session.powerUpMoves,      // List<String>
        "placedMines"      to session.placedMines.mapValues { it.value.map { cell -> mapOf("row" to cell.row, "col" to cell.col) } }
        )

    /** Decode Firestore data back into your GameSession model */
    fun decode(data: Map<String, Any?>): GameSession {
        val gameId      = data["gameId"]      as? String        ?: ""
        val p1          = data["player1Id"]   as? String        ?: ""
        val p2          = data["player2Id"]   as? String?
        val current     = data["currentTurn"] as? String        ?: ""

        val rawMoves    = data["moves"]       as? List<Map<String,Any?>> ?: emptyList()
        val moves       = rawMoves.map { Move.fromMap(it) }

        val rawShips    = data["ships"]       as? Map<String, List<Map<String,Any?>>>
            ?: emptyMap()
        val ships       = rawShips.mapValues { it.value.map { Ship.fromMap(it) } }

        val rawPUs      = data["powerUps"]    as? Map<String, List<String>>
            ?: emptyMap()
        val powerUps    = rawPUs.mapValues { it.value.map { PowerUp.valueOf(it) } }

        // ← NEW: vote state
        @Suppress("UNCHECKED_CAST")
        val mapVotes    = (data["mapVotes"] as? Map<String, Number>)
            ?.mapValues { it.value.toInt() } ?: emptyMap()

        val chosenMap   = (data["chosenMap"] as? Number)?.toInt()

        // ← NEW: energy pool
        @Suppress("UNCHECKED_CAST")
        val energy      = (data["energy"] as? Map<String, Number>)
            ?.mapValues { it.value.toInt() } ?: emptyMap()

        // ← OPTIONAL: power-up moves
        val rawPUMoves  = data["powerUpMoves"] as? List<String> ?: emptyList()

        @Suppress("UNCHECKED_CAST")
        val rawMines = data["placedMines"] as? Map<String, List<Map<String, Any?>>>
        val placedMines = rawMines?.mapValues { entry ->
            entry.value.map { cellMap ->
                val row = when (val r = cellMap["row"]) {
                    is Int -> r
                    is Long -> r.toInt()
                    else -> 0 // Or handle error
                }
                val col = when (val c = cellMap["col"]) {
                    is Int -> c
                    is Long -> c.toInt()
                    else -> 0 // Or handle error
                }
                Cell(row, col)
            }
        } ?: emptyMap()

        return GameSession(
            gameId            = gameId,
            player1Id         = p1,
            player2Id         = p2,
            currentTurn       = current,
            moves             = moves,
            ships             = ships,
            availablePowerUps = powerUps,

            mapVotes          = mapVotes,
            chosenMap         = chosenMap,
            energy            = energy,
            powerUpMoves      = rawPUMoves,
            placedMines      = placedMines
        )
    }
}
