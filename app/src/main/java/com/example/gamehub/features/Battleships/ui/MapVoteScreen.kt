package com.example.gamehub.features.battleships.ui

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.gamehub.features.battleships.model.MapRepository
import com.example.gamehub.navigation.NavRoutes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun MapVoteScreen(
    navController: NavHostController,
    code: String,
    userName: String
) {
    val db      = FirebaseFirestore.getInstance()
    val roomRef = remember { db.collection("rooms").document(code) }
    val scope   = rememberCoroutineScope()
    val meUid   = FirebaseAuth.getInstance().uid ?: return

    // Live mapVotes listener
    var mapVotes by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    DisposableEffect(code) {
        val lis: ListenerRegistration = roomRef.addSnapshotListener { snap, _ ->
            val gs = snap?.get("gameState") as? Map<*, *>
            val bm = gs?.get("battleships") as? Map<*, *>
            @Suppress("UNCHECKED_CAST")
            val raw = bm?.get("mapVotes") as? Map<String, Number>
            mapVotes = raw?.mapValues { it.value.toInt() } ?: emptyMap()
        }
        onDispose { lis.remove() }
    }

    // Local selection & lock state
    var selectedMap by remember { mutableStateOf<Int?>(null) }
    var hasVoted    by remember { mutableStateOf(false) }

    fun doVote() {
        selectedMap?.let { choice ->
            hasVoted = true
            scope.launch {
                roomRef
                    .update("gameState.battleships.mapVotes.$meUid", choice)
                    .await()
            }
        }
    }

    // Determine votes
    val opponentUid = mapVotes.keys.firstOrNull { it != meUid }
    val myVote      = mapVotes[meUid]
    val oppVote     = opponentUid?.let { mapVotes[it] }

    // Once both have voted, navigate to placement using NavRoutes.BATTLE_PLACE
    if (mapVotes.size == 2 && myVote != null && oppVote != null) {
        val route = NavRoutes.BATTLE_PLACE
            .replace("{code}", code)
            .replace("{userName}", Uri.encode(userName))
            .replace("{mapId}", myVote.toString())
        navController.navigate(route)
        return
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = when {
                !hasVoted && oppVote != null ->
                    "Opponent voted for ${
                        MapRepository.allMaps.first { it.id == oppVote }.name
                    }\nYour turn to pick"
                !hasVoted ->
                    "Choose a map and press Vote"
                else ->
                    "Waiting for opponent to vote…"
            },
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(12.dp))

        LazyVerticalGrid(
            columns               = GridCells.Fixed(2),
            modifier              = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement   = Arrangement.spacedBy(12.dp)
        ) {
            items(MapRepository.allMaps) { mapDef ->
                val border = when {
                    !hasVoted && selectedMap == mapDef.id ->
                        BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
                    hasVoted && myVote == mapDef.id ->
                        BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
                    oppVote == mapDef.id ->
                        BorderStroke(3.dp, MaterialTheme.colorScheme.secondary)
                    else -> null
                }

                Card(
                    border   = border,
                    modifier = Modifier
                        .aspectRatio(1f)
                        .then(
                            if (!hasVoted) Modifier.clickable { selectedMap = mapDef.id }
                            else Modifier
                        )
                ) {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(mapDef.name, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (!hasVoted && selectedMap != null) {
            Button(
                onClick = { doVote() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Vote")
            }
        }
    }
}
