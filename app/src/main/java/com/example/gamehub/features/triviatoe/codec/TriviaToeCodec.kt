package com.example.gamehub.features.triviatoe.codec

import com.example.gamehub.features.triviatoe.model.*
import com.example.gamehub.lobby.GameCodec

object TriviatoeCodec : GameCodec<TriviatoeMove, TriviatoeSession> {

    // Encode a move (for sending just one move to Firestore if you need)
    override fun encodeMove(move: TriviatoeMove): Map<String, Any> = mapOf(
        "playerId" to move.playerId,
        "row" to move.row,
        "col" to move.col,
        "symbol" to move.symbol,
        "round" to move.round
    )

    // Decode the full game session from Firestore
    override fun decodeState(snapshot: Map<String, Any?>): TriviatoeSession {
        return TriviatoeSession(
            roomCode = snapshot["roomCode"] as? String ?: "",
            players = (snapshot["players"] as? List<Map<String, Any?>>)?.map { playerMap ->
                TriviatoePlayer(
                    uid = playerMap["uid"] as? String ?: "",
                    name = playerMap["name"] as? String ?: "",
                    symbol = playerMap["symbol"] as? String ?: ""
                )
            } ?: emptyList(),
            board = (snapshot["board"] as? List<Map<String, Any?>>)?.map { cellMap ->
                TriviatoeCell(
                    row = (cellMap["row"] as? Long)?.toInt() ?: 0,
                    col = (cellMap["col"] as? Long)?.toInt() ?: 0,
                    symbol = cellMap["symbol"] as? String
                )
            } ?: emptyList(),
            moves = (snapshot["moves"] as? List<Map<String, Any?>>)?.map { moveMap ->
                TriviatoeMove(
                    playerId = moveMap["playerId"] as? String ?: "",
                    row = (moveMap["row"] as? Long)?.toInt() ?: 0,
                    col = (moveMap["col"] as? Long)?.toInt() ?: 0,
                    symbol = moveMap["symbol"] as? String ?: "",
                    round = (moveMap["round"] as? Long)?.toInt() ?: 0
                )
            } ?: emptyList(),
            currentRound = (snapshot["currentRound"] as? Long)?.toInt() ?: 0,
            quizQuestion = decodeQuestion(snapshot["quizQuestion"] as? Map<String, Any?>),
            answers = (snapshot["answers"] as? Map<String, Map<String, Any?>>)
                ?.mapValues { entry ->
                    decodeAnswer(entry.value)
                } ?: emptyMap(),
            firstToMove = snapshot["firstToMove"] as? String,
            currentTurn = snapshot["currentTurn"] as? String,
            winner = snapshot["winner"] as? String,
            state = (snapshot["state"] as? String)?.let { TriviatoeRoundState.valueOf(it) }
                ?: TriviatoeRoundState.QUESTION,
            randomized = snapshot["randomized"] as? Boolean, // <--- ADD THIS LINE HERE!
            lastQuestion = decodeQuestion(snapshot["lastQuestion"] as? Map<String, Any?>),
            lastCorrectIndex = (snapshot["lastCorrectIndex"] as? Long)?.toInt(),
            rematchVotes = (snapshot["rematchVotes"] as? Map<String, Boolean>) ?: emptyMap(),
            readyForQuestion = (snapshot["readyForQuestion"] as? Map<String, Boolean>) ?: emptyMap()
        )
    }

    // Utility for decoding a question
    private fun decodeQuestion(map: Map<String, Any?>?): TriviatoeQuestion? {
        if (map == null) return null
        return when (map["type"] as? String) {
            "multiple_choice" -> TriviatoeQuestion.MultipleChoice(
                question = map["question"] as? String ?: "",
                answers = (map["answers"] as? List<*>)?.map { it.toString() } ?: emptyList(),
                correctIndex = (map["correctIndex"] as? Long)?.toInt() ?: 0
            )
            "date_input" -> TriviatoeQuestion.DateInput(
                question = map["question"] as? String ?: "",
                correctMillis = map["correctMillis"] as? Long ?: 0L
            )
            else -> null
        }
    }

    // Utility for decoding an answer
//    private fun decodeAnswer(map: Map<String, Any?>?): PlayerAnswer? {
//        if (map == null) return null
//        return when (map["type"] as? String) {
//            "multiple_choice" -> {
//                val idx = (map["answerIndex"] as? Long)?.toInt() ?: 0
//                val timestampAny = map["timestamp"]
//                val millis = when (timestampAny) {
//                    is com.google.firebase.Timestamp -> timestampAny.seconds * 1000 + timestampAny.nanoseconds / 1_000_000
//                    is Long -> timestampAny
//                    is Double -> timestampAny.toLong()
//                    else -> null
//                }
//                PlayerAnswer(
//                    answerIndex = idx,
//                    timestamp = millis
//                )
//            }
//            // Optionally: handle "date_input"
//            else -> null
//        }
//    }

    private fun decodeAnswer(map: Map<String, Any?>?): PlayerAnswer? {
        if (map == null) return null
        val idx = (map["answerIndex"] as? Long)?.toInt() ?: 0
        val timestampAny = map["timestamp"]
        val millis = when (timestampAny) {
            is com.google.firebase.Timestamp -> timestampAny.seconds * 1000 + timestampAny.nanoseconds / 1_000_000
            is Long -> timestampAny
            is Double -> timestampAny.toLong()
            else -> null
        }
        return PlayerAnswer(
            answerIndex = idx,
            timestamp = millis
        )
    }


}
