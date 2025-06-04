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


class FirestoreTriviatoeSession(
    val roomCode: String
) {
    private val db = FirebaseFirestore.getInstance()
    private val room = db.collection("rooms").document(roomCode)

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
        room.update(
            mapOf("gameState.triviatoe.answers.$playerId" to mapOf(
                "answerIndex" to answer.answerIndex,
                "timestamp" to FieldValue.serverTimestamp() // This is the key change!
            ))
        ).await()
    }

    // Place an X or O on the grid
    suspend fun submitMove(playerId: String, row: Int, col: Int, symbol: String) {
        // Add a new move and update board state accordingly
        // You'll want to check turn, win conditions, etc before calling this!
        val move = mapOf(
            "playerId" to playerId,
            "row" to row,
            "col" to col,
            "symbol" to symbol,
            "round" to getCurrentRound() // Implement this helper
        )
        room.update(
            mapOf(
                "gameState.triviatoe.moves" to FieldValue.arrayUnion(move),
                "gameState.triviatoe.board" to FieldValue.arrayUnion(mapOf("row" to row, "col" to col, "symbol" to symbol))
            )
        ).await()
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

        // Indices of available questions
        val available = (TriviatoeQuestionBank.all.indices).filter { it !in usedQuestions }
        if (available.isEmpty()) {
            // No more questions left! End game or reset
            // Example: Set state to FINISHED
            room.update(mapOf(
                "gameState.triviatoe.state" to "FINISHED"
            )).await()
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

        // Save new round question and advance state
        room.update(
            mapOf(
                "gameState.triviatoe.quizQuestion" to questionMap,
                "gameState.triviatoe.state" to "QUESTION",
                "gameState.triviatoe.answers" to emptyMap<String, Any>(),
                "gameState.triviatoe.usedQuestions" to usedQuestions + randomIndex,
                "gameState.triviatoe.currentRound" to ((triviatoe?.get("currentRound") as? Long)?.toInt()?.plus(1) ?: 0)
            )
        ).await()
    }

    suspend fun advanceGameState() {
        val snap = room.get().await()
        val triviatoe = (snap.get("gameState") as? Map<*, *>)?.get("triviatoe") as? Map<*, *>
        val firstToMove = triviatoe?.get("firstToMove") as? String
        room.update(
            mapOf(
                "gameState.triviatoe.currentTurn" to firstToMove,
                "gameState.triviatoe.state" to "MOVE_1"
            )
        ).await()
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
        room.update(mapOf("gameState.triviatoe.players" to shuffled)).addOnSuccessListener {
            println("Firestore update successful!")
        }.addOnFailureListener { e ->
            println("Firestore update FAILED: ${e.message}")
        }.await()

    }

    suspend fun setFirstToMoveAndAdvance(winnerId: String) {
        // Clear answers, set firstToMove, advance to REVEAL (or directly to MOVE_1)
        println("setFirstToMoveAndAdvance called, winnerId=$winnerId")
        room.update(
            mapOf(
                "gameState.triviatoe.firstToMove" to winnerId,
                "gameState.triviatoe.answers" to emptyMap<String, Any>(),
                "gameState.triviatoe.state" to "REVEAL" // or "MOVE_1" if you want to skip the REVEAL screen
            )
        ).await()
    }

    // Move from MOVE_1 to MOVE_2 (host only)
    suspend fun afterMove1(firstToMove: String, players: List<TriviatoePlayer>) {
        val other = players.firstOrNull { it.uid != firstToMove }?.uid ?: return
        room.update(
            mapOf(
                "gameState.triviatoe.currentTurn" to other,
                "gameState.triviatoe.state" to "MOVE_2"
            )
        ).await()
    }

    // Move from MOVE_2 to CHECK_WIN (host only)
    suspend fun afterMove2() {
        room.update(
            mapOf("gameState.triviatoe.state" to "CHECK_WIN")
        ).await()
    }

    // Check for win and either finish or go to next question (host only)
    suspend fun finishMoveRound(session: TriviatoeSession) {
        println("finishMoveRound called for round ${session.currentRound}")
        val winnerUid = checkWinConditionGetWinnerUid(session)
        if (winnerUid != null) {
            room.update(
                mapOf(
                    "gameState.triviatoe.state" to "FINISHED",
                    "gameState.triviatoe.winner" to winnerUid
                )
            ).await()
        } else {
            startNextRound()
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

}
