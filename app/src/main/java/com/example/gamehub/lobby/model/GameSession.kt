package com.example.gamehub.lobby.model

data class GameSession(
    val gameId: String,
    val player1Id: String,
    val player2Id: String?,
    val currentTurn: String,
    val moves: List<Move>,
    val ships: Map<String, List<Ship>>,
    val availablePowerUps: Map<String, List<PowerUp>>,
    val energy: Map<String, Int> = emptyMap(),
    val powerUpMoves: List<String> = emptyList(),
    val mapVotes: Map<String, Int> = emptyMap(),
    val chosenMap: Int? = null,
    val gameResult: String? = null
) {
    companion object {
        fun empty(id: String) = GameSession(
            gameId = id,
            player1Id = "",
            player2Id = null,
            currentTurn = "",
            moves = emptyList(),
            ships = emptyMap(),
            availablePowerUps = emptyMap()

        )
    }

    /** Show your own ships + all hits by your opponent */
    fun getPlayerView(playerId: String): List<CellState> =
        BoardBuilder.build(
            ships[playerId] ?: emptyList(),
            moves.filter { it.playerId != playerId }
        )

    /** Show only the hits *you* made on your opponent */
    fun getOpponentView(playerId: String): List<CellState> =
        BoardBuilder.build(
            emptyList(),
            moves.filter { it.playerId == playerId }
        )
}