package com.example.gamehub.lobby

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class FirestoreSession<MOVE, STATE>(
    private val roomCode: String,
    private val codec: GameCodec<MOVE, STATE>
) : GameSession<MOVE, STATE> {

    private val roomRef = Firebase.firestore.collection("rooms").document(roomCode)

    override val stateFlow =
        roomRef.snapshotsAsFlow { data -> codec.decodeState(data) }

    override suspend fun sendMove(move: MOVE) {
        roomRef.update(codec.encodeMove(move)).await()
    }

    override suspend fun close() {}   // nothing to clean up (yet)
}
