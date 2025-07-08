package com.example.gamehub.repository.implementations

import com.example.gamehub.features.codenames.model.CodenamesGameState
import com.example.gamehub.features.codenames.model.CodenamesCard
import com.example.gamehub.features.codenames.model.CardColor
import com.example.gamehub.features.codenames.model.Clue
import com.example.gamehub.repository.interfaces.ICodenamesRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class CodenamesRepository : ICodenamesRepository {
    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("rooms")

    override fun getGameState(
        roomId: String,
        onSuccess: (CodenamesGameState?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        collection.document(roomId).get()
            .addOnSuccessListener { doc ->
                val state = doc.get("gameState.codenames") as? Map<String, Any?>
                onSuccess(state?.let { mapToGameState(it) })
            }
            .addOnFailureListener(onError)
    }

    override fun updateGameState(
        roomId: String,
        gameState: CodenamesGameState,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val data = gameStateToMap(gameState)
        collection.document(roomId)
            .update(mapOf("gameState.codenames" to data))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener(onError)
    }

    override fun listenToGameState(
        roomId: String,
        onDataChange: (CodenamesGameState?) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return collection.document(roomId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    onError(e)
                } else {
                    val state = snapshot?.get("gameState.codenames") as? Map<String, Any?>
                    onDataChange(state?.let { mapToGameState(it) })
                }
            }
    }

    // --- Mapping helpers ---
    private fun mapToGameState(map: Map<String, Any?>): CodenamesGameState {
        val cards = (map["cards"] as? List<Map<String, Any?>>)?.map {
            CodenamesCard(
                word = it["word"] as? String ?: "",
                color = CardColor.valueOf(it["color"] as? String ?: "NEUTRAL"),
                isRevealed = it["isRevealed"] as? Boolean ?: false
            )
        } ?: emptyList()
        val clues = (map["clues"] as? List<Map<String, Any?>>)?.map {
            Clue(
                word = it["word"] as? String ?: "",
                team = it["team"] as? String ?: ""
            )
        } ?: emptyList()
        return CodenamesGameState(
            cards = cards,
            currentTurn = map["currentTurn"] as? String ?: "RED",
            redWordsRemaining = (map["redWordsRemaining"] as? Number)?.toInt() ?: 9,
            blueWordsRemaining = (map["blueWordsRemaining"] as? Number)?.toInt() ?: 8,
            currentTeam = map["currentTeam"] as? String ?: "RED",
            isMasterPhase = map["isMasterPhase"] as? Boolean ?: true,
            currentClue = map["currentClue"] as? String,
            currentClueNumber = (map["currentClueNumber"] as? Number)?.toInt(),
            winner = map["winner"] as? String,
            clues = clues,
            currentGuardingWordCount = (map["currentGuardingWordCount"] as? Number)?.toInt(),
            guessesRemaining = (map["guessesRemaining"] as? Number)?.toInt()
        )
    }

    private fun gameStateToMap(state: CodenamesGameState): Map<String, Any?> {
        return mapOf(
            "cards" to state.cards.map {
                mapOf(
                    "word" to it.word,
                    "color" to it.color.name,
                    "isRevealed" to it.isRevealed
                )
            },
            "currentTurn" to state.currentTurn,
            "redWordsRemaining" to state.redWordsRemaining,
            "blueWordsRemaining" to state.blueWordsRemaining,
            "currentTeam" to state.currentTeam,
            "isMasterPhase" to state.isMasterPhase,
            "currentClue" to state.currentClue,
            "currentClueNumber" to state.currentClueNumber,
            "winner" to state.winner,
            "clues" to state.clues.map {
                mapOf(
                    "word" to it.word,
                    "team" to it.team
                )
            },
            "currentGuardingWordCount" to state.currentGuardingWordCount,
            "guessesRemaining" to state.guessesRemaining
        )
    }
} 