package com.example.gamehub.lobby

import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlinx.coroutines.channels.awaitClose

object LobbyService {
    private val firestore = FirebaseFirestore.getInstance()
    private val rooms     = firestore.collection("rooms")
    private val auth      = Firebase.auth

    /**
     * Hosts a new room.
     * @param gameId    the id of the game (“battleships”, “ohpardon”, …)
     * @param roomName  the human-friendly room name (required)
     * @param hostName  the display name of the host (required)
     * @param password  optional raw password; will be hashed with .hashCode()
     * @return the newly created room’s document ID (the join code)
     */
    suspend fun host(
        gameId: String,
        roomName: String,
        hostName: String,
        password: String?
    ): String {
        val hostUid = auth.currentUser?.uid
            ?: throw IllegalStateException("Must be signed in to host")

        // 6-char uppercase join code
        val code = UUID.randomUUID()
            .toString()
            .replace("-", "")
            .take(6)
            .uppercase()

        // Decide maxPlayers based on the game:
        val maxPlayers = when (gameId) {
            "battleships" -> 2
            "ohpardon"    -> 4
            else          -> 4
        }

        // Build our Room map (no nested serverTimestamp in arrays!)
        val roomData = mapOf<String, Any?>(
            "gameId"     to gameId,
            "name"       to roomName,
            "hostUid"    to hostUid,
            "hostName"   to hostName,
            "password"   to password?.hashCode(),
            "maxPlayers" to maxPlayers,
            "status"     to "waiting",
            "players"    to listOf(
                mapOf(
                    "uid"  to hostUid,
                    "name" to hostName
                )
            ),
            "createdAt"  to FieldValue.serverTimestamp()
        )

        rooms.document(code)
            .set(roomData)
            .await()

        return code
    }

    /**
     * Attempts to join an existing room.
     * @param code      the 6-char room code
     * @param userName  the display name of the joining player (required)
     * @param password  the raw room password (if any)
     * @return the gameId if joined; null if room not found, password mismatch, or full.
     */
    suspend fun join(
        code: String,
        userName: String,
        password: String?
    ): String? {
        val user = auth.currentUser ?: return null
        val snap = rooms.document(code).get().await()
        if (!snap.exists()) return null

        // 1️⃣ Password check
        val expectedHash = snap.getLong("password")?.toInt()
        if (expectedHash != null && expectedHash != password?.hashCode()) {
            return null
        }

        // 2️⃣ Capacity check
        val maxPlayers     = snap.getLong("maxPlayers")?.toInt() ?: return null
        val currentPlayers = (snap.get("players") as? List<*>)?.size ?: 0
        if (currentPlayers >= maxPlayers) return null

        // 3️⃣ Add this player to the array
        val playerData = mapOf(
            "uid"  to user.uid,
            "name" to userName
        )
        rooms.document(code)
            .update("players", FieldValue.arrayUnion(playerData))
            .await()

        // 4️⃣ Return gameId so the UI can navigate
        return snap.getString("gameId")
    }

    /**
     * Streams the list of *waiting* rooms for a given gameId.
     */
    fun publicRoomsFlow(gameId: String): Flow<List<RoomSummary>> = callbackFlow {
        val registration = rooms
            .whereEqualTo("gameId", gameId)
            .whereEqualTo("status", "waiting")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { qs, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }
                val list = qs!!.documents.mapNotNull { doc ->
                    val name    = doc.getString("name") ?: return@mapNotNull null
                    val host    = doc.getString("hostName") ?: "?"
                    val players = (doc.get("players") as? List<*>)?.size ?: 0
                    val maxP    = doc.getLong("maxPlayers")?.toInt() ?: 0
                    val locked  = doc.getLong("password") != null
                    RoomSummary(
                        code           = doc.id,
                        name           = name,
                        hostName       = host,
                        currentPlayers = players,
                        maxPlayers     = maxP,
                        hasPassword    = locked
                    )
                }
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    data class RoomSummary(
        val code: String,
        val name: String,
        val hostName: String,
        val currentPlayers: Int,
        val maxPlayers: Int,
        val hasPassword: Boolean
    )
}
