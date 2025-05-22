package com.example.gamehub.lobby

import com.example.gamehub.lobby.codec.BattleshipsCodec
import com.example.gamehub.lobby.model.Move
import com.example.gamehub.lobby.model.GameSession
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose

class FirestoreSession(
    private val gameId: String,
    private val codec: BattleshipsCodec
) {
    private val db     = FirebaseFirestore.getInstance()
    private val room   = db.collection("rooms").document(gameId)

    /** Live stream of full GameSession */
    val stateFlow = callbackFlow<GameSession> {
        val listener = room.addSnapshotListener { snap, _ ->
            snap?.data?.let {
                trySend(BattleshipsCodec.decode(it))
            }
        }
        awaitClose { listener.remove() }
    }.stateIn(
        scope = CoroutineScope(Dispatchers.IO),
        started = SharingStarted.Eagerly,
        initialValue = GameSession.empty(gameId)
    )

    /** Fire one shot at (row,col) and pass turn to opponent */
    suspend fun submitMove(x: Int, y: Int, playerId: String) {
        val snap    = room.get().await()
        val current = BattleshipsCodec.decode(snap.data ?: error("Session missing"))
        val opponent = if (playerId == current.player1Id) current.player2Id!! else current.player1Id
        val updated = current.copy(
            moves       = current.moves + Move(x, y, playerId),
            currentTurn = opponent
        )
        room.set(BattleshipsCodec.encode(updated)).await()
    }
}
