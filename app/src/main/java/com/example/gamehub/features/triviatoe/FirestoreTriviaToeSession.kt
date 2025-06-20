package com.example.gamehub.features.triviatoe

import com.example.gamehub.features.triviatoe.codec.TriviatoeCodec
import com.example.gamehub.features.triviatoe.model.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await
import com.example.gamehub.features.triviatoe.TriviatoeQuestionBank
import kotlinx.coroutines.delay

class FirestoreTriviatoeSession(
    val roomCode: String
) {
    private val db = FirebaseFirestore.getInstance()
    private val room = db.collection("rooms").document(
        requireNotNull(roomCode.takeIf { it.isNotBlank() }) { "roomCode must not be empty or blank" }
    )

    /**
     * Flow of the current session state, real-time.
     */
    val stateFlow = callbackFlow<TriviatoeSession> {
        val registration = room.addSnapshotListener { snap, _ ->
            val gs = snap?.get("gameState") as? Map<*, *> ?: return@addSnapshotListener
            val triviatoeState = (gs["triviatoe"] as? Map<*, *>)?.mapKeys { it.key as String } ?: return@addSnapshotListener
            trySend(TriviatoeCodec.decodeState(triviatoeState as Map<String, Any?>))
        }
        awaitClose { registration.remove() }
    }.stateIn(
        scope = CoroutineScope(Dispatchers.Main),
        started = SharingStarted.Eagerly,
        initialValue = emptySession(roomCode)
    )

    companion object {
        fun emptySession(roomCode: String) = TriviatoeSession(
            roomCode = roomCode,
            players = emptyList(),
            board = emptyList(),
            moves = emptyList(),
            currentRound = 0,
            quizQuestion = null,
            answers = emptyMap(),
            firstToMove = null,
            currentTurn = null,
            winner = null,
            state = TriviatoeRoundState.QUESTION
        )
    }

    private suspend fun safeUpdate(map: Map<String, Any?>, actionName: String) {
        try {
            room.update(map).await()
            println("Firestore update '$actionName' successful!")
        } catch (e: Exception) {
            println("Firestore update '$actionName' FAILED: ${e.message}")
            // Optionally: retry logic here!
        }
    }

    // Submit a quiz answer for this round

    suspend fun submitAnswer(playerId: String, answer: PlayerAnswer) {
        safeUpdate(
            mapOf("gameState.triviatoe.answers.$playerId" to mapOf(
                "answerIndex" to answer.answerIndex,
                "timestamp" to FieldValue.serverTimestamp()
            )),
            "submitAnswer"
        )
    }

    // Place an X or O on the grid
    suspend fun submitMove(playerId: String, row: Int, col: Int, symbol: String) {
        val snap = room.get().await()
        val gs = snap.get("gameState") as? Map<*, *> ?: return
        val triviatoeState = (gs["triviatoe"] as? Map<*, *>)?.mapKeys { it.key as String } ?: return
        val session = TriviatoeCodec.decodeState(triviatoeState as Map<String, Any?>)

        // Defensive checks: Only allow move if correct state/turn, and not moved already
        if (session.state != TriviatoeRoundState.MOVE_1 && session.state != TriviatoeRoundState.MOVE_2) {
            println("submitMove: Not in a move state, move blocked.")
            return
        }
        if (session.currentTurn != playerId) {
            println("submitMove: Not this player's turn, move blocked.")
            return
        }
        if (session.moves.any { it.playerId == playerId && it.round == session.currentRound }) {
            println("submitMove: Player already moved this round, move blocked.")
            return
        }

        if (session.state == TriviatoeRoundState.FINISHED) {
            println("submitMove: Game already finished, move blocked.")
            return
        }
        val move = mapOf(
            "playerId" to playerId,
            "row" to row,
            "col" to col,
            "symbol" to symbol,
            "round" to getCurrentRound(),
        )

        // Add the move and board update
        safeUpdate(
            mapOf(
                "gameState.triviatoe.moves" to FieldValue.arrayUnion(move),
                "gameState.triviatoe.board" to FieldValue.arrayUnion(mapOf("row" to row, "col" to col, "symbol" to symbol)),
                "gameState.triviatoe.lastMoveTimestamp" to FieldValue.serverTimestamp()
            ),
            "submitMove"
        )

        val boardCells = session.board.toMutableList()
        if (!boardCells.any { it.row == row && it.col == col }) {
            boardCells.add(TriviatoeCell(row, col, symbol))
        }

        if (checkWinForMoveBoard(boardCells, row, col, symbol, session.players)) {
            safeUpdate(
                mapOf(
                    "gameState.triviatoe.state" to "FINISHED",
                    "gameState.triviatoe.winner" to playerId
                ),
                "instantWin"
            )
            return // <--- **DO NOT PROCEED**: this "locks" the game
        }

        // Only advance state if still not finished (no win)
        val movesThisRound = session.moves.count { it.round == session.currentRound }
        val playerIds = session.players.map { it.uid }
        val otherPlayerId = playerIds.firstOrNull { it != playerId }

        if (movesThisRound == 1 && otherPlayerId != null) {
            safeUpdate(
                mapOf(
                    "gameState.triviatoe.currentTurn" to otherPlayerId,
                    "gameState.triviatoe.state" to "MOVE_2"
                ),
                "nextTurn"
            )
        } else if (movesThisRound == 2) {
            safeUpdate(
                mapOf(
                    "gameState.triviatoe.state" to "CHECK_WIN"
                ),
                "checkWin"
            )
        }
    }

    // Helper that takes an explicit board state:
    fun checkWinForMoveBoard(
        board: List<TriviatoeCell>,
        row: Int,
        col: Int,
        symbol: String,
        players: List<TriviatoePlayer>
    ): Boolean {
        val size = 10
        val arr = Array(size) { Array<String?>(size) { null } }
        for (cell in board) {
            arr[cell.row][cell.col] = cell.symbol
        }
        arr[row][col] = symbol // ensure this move is there!
        val directions = listOf(Pair(0, 1), Pair(1, 0), Pair(1, 1), Pair(1, -1))
        for ((dr, dc) in directions) {
            var count = 1
            var r = row + dr
            var c = col + dc
            while (r in 0 until size && c in 0 until size && arr[r][c] == symbol) {
                count++
                r += dr
                c += dc
            }
            r = row - dr
            c = col - dc
            while (r in 0 until size && c in 0 until size && arr[r][c] == symbol) {
                count++
                r -= dr
                c -= dc
            }
            if (count >= 4) return true
        }
        return false
    }




    // Helpers (implement as needed)
    private fun encodeAnswer(answer: TriviatoeAnswer): Map<String, Any> =
        when (answer) {
            is TriviatoeAnswer.MultipleChoice -> mapOf("type" to "multiple_choice", "answerIndex" to answer.answerIndex)
            is TriviatoeAnswer.DateInput      -> mapOf("type" to "date_input", "millis" to answer.millis)
        }

    private suspend fun getCurrentRound(): Int {
        val snap = room.get().await()
        val gs = snap.get("gameState") as? Map<*, *> ?: return 0
        val triviatoeState = (gs["triviatoe"] as? Map<*, *>)?.mapKeys { it.key as String } ?: return 0
        return (triviatoeState["currentRound"] as? Long)?.toInt() ?: 0
    }

    suspend fun startNextRound() {
        val snap = room.get().await()
        val triviatoe = (snap.get("gameState") as? Map<*, *>)?.get("triviatoe") as? Map<*, *>
        val usedQuestions = (triviatoe?.get("usedQuestions") as? List<Long>)?.map { it.toInt() } ?: emptyList()
        val gs = snap.get("gameState") as? Map<*, *> ?: return
        val triviatoeState = (gs["triviatoe"] as? Map<*, *>)?.mapKeys { it.key as String } ?: return
        val state = triviatoeState["state"] as? String
        val currentQuestion = triviatoeState["quizQuestion"]

        if (state == TriviatoeRoundState.FINISHED.name) {
            println("startNextRound: Already finished, skipping.")
            return
        }

        // Defensive: Only proceed if both players are present, assigned, and ready!
        val playersList = (triviatoeState["players"] as? List<Map<String, Any?>>)?.map { it.toMutableMap() } ?: emptyList()
        val readyMap = triviatoeState["readyForQuestion"] as? Map<String, Boolean> ?: emptyMap()
        val bothAssigned = playersList.size == 2 && playersList.all { !((it["symbol"] ?: "") as String).isNullOrEmpty() }
        val allReady = playersList.all { p -> (p["uid"] as? String)?.let { readyMap[it] == true } == true }

        if (!bothAssigned || !allReady) {
            println("startNextRound: Not all players are ready! Skipping...")
            return
        }

        // *** PATCH: Only write a new question if NOT already at QUESTION with a quizQuestion ***
        if (state == TriviatoeRoundState.QUESTION.name && currentQuestion != null) {
            println("startNextRound: Already at QUESTION with a question, doing nothing.")
            return
        }

        // Indices of available questions
        val available = (TriviatoeQuestionBank.all.indices).filter { it !in usedQuestions }
        if (available.isEmpty()) {
            safeUpdate(
                mapOf("gameState.triviatoe.state" to "FINISHED"),
                "startNextRound-FINISHED"
            )
            return
        }
        val randomIndex = available.random()
        val question = TriviatoeQuestionBank.all[randomIndex]
        val questionMap = mapOf(
            "type" to "multiple_choice",
            "question" to question.question,
            "answers" to question.answers,
            "correctIndex" to question.correctIndex
        )

        val newRound = when {
            (triviatoe?.get("quizQuestion") == null) -> 0 // First round
            else -> ((triviatoe?.get("currentRound") as? Long)?.toInt()?.plus(1) ?: 1)
        }

        println("startNextRound: Writing new question for round $newRound")

        safeUpdate(
            mapOf(
                "gameState.triviatoe.quizQuestion" to questionMap,
                "gameState.triviatoe.state" to "QUESTION",
                "gameState.triviatoe.answers" to emptyMap<String, Any>(),
                "gameState.triviatoe.usedQuestions" to usedQuestions + randomIndex,
                "gameState.triviatoe.currentRound" to newRound,
                "gameState.triviatoe.randomized" to null,
                "gameState.triviatoe.firstToMove" to null,
            ),
            "startNextRound"
        )
    }


    suspend fun advanceGameState() {
        val snap = room.get().await()
        val triviatoe = (snap.get("gameState") as? Map<*, *>)?.get("triviatoe") as? Map<*, *>
        val firstToMove = triviatoe?.get("firstToMove") as? String
        if (firstToMove == null) {
            println("advanceGameState: No firstToMove set, skipping.")
            return
        }
        safeUpdate(
            mapOf(
                "gameState.triviatoe.currentTurn" to firstToMove,
                "gameState.triviatoe.state" to "MOVE_1"
            ),
            "advanceGameState"
        )
    }

    suspend fun assignSymbolsRandomly() {
        println("assignSymbolsRandomly() CALLED!!")
        val snap = room.get().await()
        val triviatoe = (snap.get("gameState") as? Map<*, *>)?.get("triviatoe") as? Map<*, *>
        val playersList = (triviatoe?.get("players") as? List<Map<String, Any?>>)?.map { it.toMutableMap() } ?: return

        println("Players before assignment: $playersList")
        if (playersList.size < 2) {
            println("Not enough players yet!")
            return
        }

        // Shuffle and assign
        val shuffled = playersList.shuffled()
        shuffled[0]["symbol"] = "X"
        shuffled[1]["symbol"] = "O"
        println("Players after assignment: $shuffled")

        // Write to Firestore
        safeUpdate(
            mapOf("gameState.triviatoe.players" to shuffled),
            "assignSymbolsRandomly"
        )

    }

    // Example inside FirestoreTriviatoeSession:
    suspend fun setFirstToMoveAndAdvance(winnerId: String, randomized: Boolean) {
        println("setFirstToMoveAndAdvance called with winnerId=$winnerId, randomized=$randomized")
        val snap = room.get().await()
        val triviatoe = (snap.get("gameState") as? Map<*, *>)?.get("triviatoe") as? Map<*, *>
        val quizQuestion = triviatoe?.get("quizQuestion") as? Map<String, Any?>
        val quizType = quizQuestion?.get("type") as? String
        val quizQuestionWithType = if (quizQuestion != null && quizType == null) {
            quizQuestion + mapOf("type" to "multiple_choice")
        } else {
            quizQuestion
        }
        val correctIndex = (quizQuestionWithType?.get("correctIndex") as? Long)?.toInt()

        safeUpdate(
            mapOf(
                "gameState.triviatoe.firstToMove" to winnerId,
                "gameState.triviatoe.answers" to emptyMap<String, Any>(),
                "gameState.triviatoe.randomized" to randomized,
                "gameState.triviatoe.lastQuestion" to quizQuestionWithType,
                "gameState.triviatoe.lastCorrectIndex" to correctIndex,
                "gameState.triviatoe.state" to "REVEAL"
            ),
            "setFirstToMoveAndAdvance-winner"
        )
    }

    // Move from MOVE_1 to MOVE_2 (host only)
    suspend fun afterMove1(firstToMove: String, players: List<TriviatoePlayer>) {
        val snap = room.get().await()
        val gs = snap.get("gameState") as? Map<*, *> ?: return
        val triviatoeState = (gs["triviatoe"] as? Map<*, *>)?.mapKeys { it.key as String } ?: return
        val state = triviatoeState["state"] as? String
        if (state == TriviatoeRoundState.FINISHED.name) {
            println("FunctionName: Already finished, skipping.")
            return
        }


        val other = players.firstOrNull { it.uid != firstToMove }?.uid ?: return
        safeUpdate(
            mapOf(
                "gameState.triviatoe.currentTurn" to other,
                "gameState.triviatoe.state" to "MOVE_2"
            ),
            "afterMove1"
        )
    }

    // Move from MOVE_2 to CHECK_WIN (host only)
    suspend fun afterMove2() {
        val snap = room.get().await()
        val gs = snap.get("gameState") as? Map<*, *> ?: return
        val triviatoeState = (gs["triviatoe"] as? Map<*, *>)?.mapKeys { it.key as String } ?: return
        val state = triviatoeState["state"] as? String
        if (state == TriviatoeRoundState.FINISHED.name) {
            println("FunctionName: Already finished, skipping.")
            return
        }

        safeUpdate(
            mapOf("gameState.triviatoe.state" to "CHECK_WIN"),
            "afterMove2"
        )
    }

    // Check for win and either finish or go to next question (host only)
    suspend fun finishMoveRound(session: TriviatoeSession) {
        if (session.state == TriviatoeRoundState.FINISHED) {
            println("finishMoveRound: Game already finished, skipping.")
            return
        }

        val winnerUid = checkWinConditionGetWinnerUid(session)
        if (winnerUid != null) {
            safeUpdate(
                mapOf(
                    "gameState.triviatoe.state" to "FINISHED",
                    "gameState.triviatoe.winner" to winnerUid
                ),
                "finishMoveRound-FINISHED"
            )
        } else {
            // After a full move round with no win, wait for both to confirm ready for next round
            safeUpdate(
                mapOf(
                    "gameState.triviatoe.state" to "WAITING_FOR_READY",
                    "gameState.triviatoe.readyForQuestion" to mapOf<String, Boolean>() // clear ready
                ),
                "finishMoveRound-WAITING_FOR_READY"
            )
        }
    }



    // Utility: Returns uid if winner found, or null
    fun checkWinConditionGetWinnerUid(session: TriviatoeSession): String? {
        val size = 10
        val board = Array(size) { Array<String?>(size) { null } }
        for (cell in session.board) {
            board[cell.row][cell.col] = cell.symbol
        }
        // Helper to check four-in-a-row in any direction
        fun checkLine(startRow: Int, startCol: Int, dRow: Int, dCol: Int): String? {
            var r = startRow
            var c = startCol
            var last: String? = null
            var count = 0
            while (r in 0 until size && c in 0 until size) {
                val sym = board[r][c]
                if (sym != null && sym == last) {
                    count++
                    if (count == 4) return session.players.find { it.symbol == sym }?.uid
                } else {
                    count = 1
                    last = sym
                }
                r += dRow
                c += dCol
            }
            return null
        }
        // Horizontal and vertical
        for (row in 0 until size) {
            var winner = checkLine(row, 0, 0, 1)
            if (winner != null) return winner
            winner = checkLine(0, row, 1, 0)
            if (winner != null) return winner
        }
        // Diagonals
        for (i in 0..size-4) {
            var winner = checkLine(i, 0, 1, 1)
            if (winner != null) return winner
            winner = checkLine(0, i, 1, 1)
            if (winner != null) return winner
            winner = checkLine(i, size-1, 1, -1)
            if (winner != null) return winner
            winner = checkLine(0, size-1-i, 1, -1)
            if (winner != null) return winner
        }
        // No winner
        return null
    }

    // Called when a player taps Rematch
    // In FirestoreTriviatoeSession:
    suspend fun requestRematch(playerId: String) {
        safeUpdate(
            mapOf("gameState.triviatoe.rematchVotes.$playerId" to true),
            "requestRematch"
        )
    }

    suspend fun tryResetIfAllAgreed() {
        val snap = room.get().await()
        val triviatoe = (snap.get("gameState") as? Map<*, *>)?.get("triviatoe") as? Map<*, *>
        val rematchVotes = (triviatoe?.get("rematchVotes") as? Map<String, Boolean>) ?: emptyMap()
        val players = (triviatoe?.get("players") as? List<Map<String, Any>>) ?: emptyList()

        // Add this check (read resetting flag)
        val resetting = triviatoe?.get("resetting") as? Boolean ?: false
        if (resetting) return // Another reset is already happening!

        val allAgreed = players.all { p ->
            val uid = p["uid"]
            uid is String && rematchVotes[uid] == true
        }

        if (players.isNotEmpty() && allAgreed) {
            // Set the resetting flag *before* resetting everything else
            safeUpdate(
                mapOf("gameState.triviatoe.resetting" to true),
                "setResettingLock"
            )
            // Now do the reset as before
            safeUpdate(
                mapOf(
                    "gameState.triviatoe.state" to "XO_ASSIGN",
                    "gameState.triviatoe.board" to emptyList<Any>(),
                    "gameState.triviatoe.moves" to emptyList<Any>(),
                    "gameState.triviatoe.winner" to null,
                    "gameState.triviatoe.rematchVotes" to mapOf<String, Boolean>(),
                    "gameState.triviatoe.currentTurn" to null,
                    "gameState.triviatoe.firstToMove" to null,
                    "gameState.triviatoe.currentRound" to 0,
                    "gameState.triviatoe.answers" to emptyMap<String, Any>(),
                    "gameState.triviatoe.quizQuestion" to null,
                    "gameState.triviatoe.lastQuestion" to null,
                    "gameState.triviatoe.lastCorrectIndex" to null,
                    "gameState.triviatoe.randomized" to null,
                    "gameState.triviatoe.resetting" to false
                ),
                "resetForRematch"
            )
        }
    }

    suspend fun setReadyForQuestion(playerId: String) {
        safeUpdate(
            mapOf("gameState.triviatoe.readyForQuestion.$playerId" to true),
            "setReadyForQuestion"
        )
    }
}


