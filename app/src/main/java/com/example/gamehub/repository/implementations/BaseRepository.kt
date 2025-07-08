package com.example.gamehub.repository.implementations

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * BaseRepository provides generic Firestore CRUD and real-time operations for any collection.
 * Extend this class for specific repositories to avoid code duplication.
 */
abstract class BaseRepository(private val collectionName: String) {
    protected val db = FirebaseFirestore.getInstance()
    protected val collection = db.collection(collectionName)

    open fun getById(
        id: String,
        onSuccess: (Map<String, Any?>?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        collection.document(id).get()
            .addOnSuccessListener { doc -> onSuccess(doc.data) }
            .addOnFailureListener(onError)
    }

    open fun update(
        id: String,
        data: Map<String, Any?>,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        collection.document(id).update(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener(onError)
    }

    open fun set(
        id: String,
        data: Map<String, Any?>,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        collection.document(id).set(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener(onError)
    }

    open fun delete(
        id: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        collection.document(id).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener(onError)
    }

    open fun listen(
        id: String,
        onDataChange: (Map<String, Any?>?) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return collection.document(id)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    onError(e)
                } else {
                    onDataChange(snapshot?.data)
                }
            }
    }
} 