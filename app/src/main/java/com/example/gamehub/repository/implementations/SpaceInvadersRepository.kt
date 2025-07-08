package com.example.gamehub.repository.implementations

import android.util.Log
import com.example.gamehub.features.spaceinvaders.models.PlayerScore
import com.example.gamehub.repository.interfaces.ISpaceInvadersRepository
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SpaceInvadersRepository : BaseRepository("space-invaders"), ISpaceInvadersRepository {
    private val _highScores = MutableStateFlow<List<PlayerScore>>(emptyList())
    override val highScores: StateFlow<List<PlayerScore>> = _highScores

    private val _playerName = MutableStateFlow("")
    override val playerName: StateFlow<String> = _playerName

    override fun onPlayerNameChanged(newName: String) {
        _playerName.value = newName
    }

    override fun fetchHighScores() {
        collection
            .orderBy("score", Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener

                val scores = snapshot.documents.mapNotNull {
                    it.toObject(PlayerScore::class.java)
                }
                _highScores.value = scores
            }
    }

    override fun submitScore(playerName: String, newScore: Int) {
        val docRef = collection.document(playerName.lowercase())

        docRef.get()
            .addOnSuccessListener { document ->
                val currentScore = document.getLong("score")?.toInt() ?: 0
                if (newScore > currentScore) {
                    set(
                        playerName.lowercase(),
                        mapOf(
                            "player" to playerName,
                            "score" to newScore
                        ),
                        { Log.d("Firestore", "New high score saved.") },
                        { e -> Log.e("Firestore", "Failed to save high score", e) }
                    )
                } else {
                    Log.d("Firestore", "Score not updated (not higher).")
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Failed to read existing score", e)
            }
    }
}

