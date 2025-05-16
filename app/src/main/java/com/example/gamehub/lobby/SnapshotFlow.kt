package com.example.gamehub.lobby

import com.google.firebase.firestore.DocumentReference
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

fun <T> DocumentReference.snapshotsAsFlow(
    mapper: (Map<String, Any?>) -> T
) = callbackFlow {
    val reg = addSnapshotListener { snap, err ->
        if (err != null) close(err)
        else if (snap != null && snap.exists()) trySend(mapper(snap.data!!))
    }
    awaitClose { reg.remove() }
}
