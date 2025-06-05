// app/src/main/java/com/example/gamehub/ui/GuestGameScreen.kt
package com.example.gamehub.ui

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gamehub.navigation.NavRoutes
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import kotlinx.coroutines.launch

@Composable
fun GuestGameScreen(
    navController: NavController,
    gameId: String,
    code: String,
    userName: String
) {
    val db = Firebase.firestore
    var status by remember { mutableStateOf("waiting") }

    // Listen for room status
    LaunchedEffect(code) {
        db.collection("rooms").document(code)
            .addSnapshotListener { snap, _ ->
                if (snap != null && snap.exists()) {
                    status = snap.getString("status") ?: "waiting"
                }
            }
    }

    // When status flips to started, navigate *this* guest into vote
    LaunchedEffect(status) {
        if (status == "started") {
            val route = when (gameId) {
                "battleships" -> NavRoutes.BATTLE_VOTE
                "ohpardon"    -> NavRoutes.OHPARDON_GAME
                "whereandwhen" -> NavRoutes.WHERE_AND_WHEN_GAME
                else          -> null
            }
            route?.let {
                navController.navigate(
                    it.replace("{code}", code)
                        .replace("{userName}", Uri.encode(userName))
                )
            }
        }
    }

    // … UI for showing “waiting for host” + “Leave” button …
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Waiting for host to start…")
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            db.collection("rooms").document(code)
                .update(
                    "players",
                    FieldValue.arrayRemove(
                        mapOf("uid" to Firebase.auth.uid, "name" to userName)
                    )
                )
                .addOnSuccessListener { navController.popBackStack() }
        }) {
            Text("Leave Room")
        }
    }
}
