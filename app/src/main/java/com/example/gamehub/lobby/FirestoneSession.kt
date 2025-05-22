package com.example.gamehub.lobby

import com.example.gamehub.lobby.codec.BattleshipsCodec
import com.example.gamehub.lobby.model.GameSession
import com.example.gamehub.lobby.model.Move
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await

class FirestoreSession(
    private val roomCode: String,
    private val codec: BattleshipsCodec
) {
    private val db   = FirebaseFirestore.getInstance()
    private val room = db.collection("rooms").document(roomCode)

    /**
     * Listen to the nested `gameState.battleships` map,
     * decode it via your codec, and emit as a GameSession.
     */
    val stateFlow = callbackFlow<GameSession> {
        val registration = room.addSnapshotListener { snap, _ ->
            val gs = snap?.get("gameState") as? Map<*, *>        ?: return@addSnapshotListener
            val bs = gs["battleships"]   as? Map<String, Any?> ?: return@addSnapshotListener
            // decode just that battleships‐state
            trySend(codec.decode(bs))
        }
        awaitClose { registration.remove() }
    }.stateIn(
        scope = CoroutineScope(Dispatchers.IO),
        started = SharingStarted.Eagerly,
        initialValue = GameSession.empty(roomCode)
    )

    /**
     * Fire one shot at (x,y) and switch turns, then write
     * *only* the nested `gameState.battleships` map back.
     */
    suspend fun submitMove(x: Int, y: Int, playerId: String) {
        // 1) pull the nested battleships map
        val snap = room.get().await()
        val gs   = snap.get("gameState") as? Map<*, *>           ?: error("Missing gameState")
        val bs   = gs["battleships"]   as? Map<String, Any?>     ?: error("Missing battleships")

        // 2) decode, update, re‐encode
        val current  = codec.decode(bs)
        val opponent = if (playerId == current.player1Id) {
            current.player2Id ?: error("No opponent yet")
        } else {
            current.player1Id
        }
        val updated = current.copy(
            moves       = current.moves + Move(x, y, playerId),
            currentTurn = opponent
        )
        val encoded = codec.encode(updated)

        // 3) write *just* that battleships sub‐map
        room
            .update("gameState.battleships", encoded)
            .await()
    }
}
