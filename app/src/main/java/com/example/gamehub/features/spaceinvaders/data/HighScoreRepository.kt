package com.example.gamehub.features.spaceinvaders.data

import android.util.Log
import com.example.gamehub.features.spaceinvaders.models.PlayerScore
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface HighScoreRepository {
    val highScores: StateFlow<List<PlayerScore>>
    val playerName: StateFlow<String>

    fun onPlayerNameChanged(newName: String)
    fun fetchHighScores()
    fun submitScore(playerName: String, newScore: Int)
}

class FirestoreHighScoreRepository : HighScoreRepository {
    private val db = FirebaseFirestore.getInstance()

    private val _highScores = MutableStateFlow<List<PlayerScore>>(emptyList())
    override val highScores: StateFlow<List<PlayerScore>> = _highScores

    private val _playerName = MutableStateFlow("")
    override val playerName: StateFlow<String> = _playerName

    override fun onPlayerNameChanged(newName: String) {
        _playerName.value = newName
    }

    override fun fetchHighScores() {
        db.collection("space-invaders")
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
        val docRef = db.collection("space-invaders")
            .document(playerName.lowercase())

        docRef.get()
            .addOnSuccessListener { document ->
                val currentScore = document.getLong("score")?.toInt() ?: 0
                if (newScore > currentScore) {
                    docRef.set(mapOf(
                        "player" to playerName,
                        "score" to newScore
                    ))
                    Log.d("Firestore", "New high score saved.")
                } else {
                    Log.d("Firestore", "Score not updated (not higher).")
                }
            }
            .addOnFailureListener {
                Log.e("Firestore", "Failed to read existing score", it)
            }
    }
}

