package com.example.gamehub.features.battleships.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gamehub.features.battleships.model.MapRepository
import com.example.gamehub.lobby.FirestoreSession
import com.example.gamehub.lobby.codec.BattleshipsCodec
import com.example.gamehub.lobby.codec.BattleshipsState
import com.example.gamehub.navigation.NavRoutes
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun MapVoteScreen(
    navController: NavController,
    code: String,
    userName: String
) {
    val session = remember { FirestoreSession(code, BattleshipsCodec) }
    val state by session.stateFlow.collectAsState(
        initial = BattleshipsState("", emptyList(), null)
    )

    val db = Firebase.firestore
    val roomDoc = remember { db.collection("rooms").document(code) }
    val scope = rememberCoroutineScope()

    var selectedLocal by remember { mutableStateOf<Int?>(null) }
    var hasVoted      by remember { mutableStateOf(false) }
    var timeLeft      by remember { mutableStateOf(60) }

    // Countdown / auto-vote
    LaunchedEffect(timeLeft, hasVoted) {
        if (timeLeft > 0 && !hasVoted) {
            delay(1_000L)
            timeLeft--
        } else if (timeLeft == 0 && !hasVoted) {
            val autoPick = Random.nextInt(MapRepository.allMaps.size)
            roomDoc.update("gameState.battleships.mapVotes.$userName", autoPick)
            hasVoted = true
        }
    }

    // Tally votes & set chosenMap
    LaunchedEffect(state.mapVotes) {
        if (state.mapVotes.containsKey(userName)) {
            hasVoted = true
        }
        if (state.mapVotes.size == 2 && state.chosenMap == null) {
            val votes  = state.mapVotes.values.toList()
            val chosen = if (votes[0] == votes[1]) votes[0]
            else Random.nextInt(MapRepository.allMaps.size)
            roomDoc.update("gameState.battleships.chosenMap", chosen)
        }
    }

    // Navigate once map is chosen
    LaunchedEffect(state.chosenMap) {
        state.chosenMap?.let { mapId ->
            navController.navigate(
                NavRoutes.BATTLE_PLACE
                    .replace("{code}", code)
                    .replace("{userName}", userName)
                    .replace("{mapId}", mapId.toString())
            )
        }
    }

    val opponentName = state.mapVotes.keys.firstOrNull { it != userName }
    val myVoteId     = state.mapVotes[userName]
    val oppVoteId    = opponentName?.let { state.mapVotes[it] }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = when {
                !hasVoted && oppVoteId != null ->
                    "Opponent picked: ${
                        MapRepository.allMaps.first { it.id == oppVoteId }.name
                    }\nYour turn to vote ($timeLeft s)"
                !hasVoted -> "Choose map ( $timeLeft s )"
                else      -> "Waiting for opponent…"
            },
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(8.dp))

        if (oppVoteId != null) {
            val oppMap = MapRepository.allMaps.first { it.id == oppVoteId }
            Text("Opponent chose: ${oppMap.name}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
        }

        LazyVerticalGrid(
            columns              = GridCells.Fixed(2),
            modifier             = Modifier.weight(1f),
            horizontalArrangement= Arrangement.spacedBy(16.dp),
            verticalArrangement  = Arrangement.spacedBy(16.dp)
        ) {
            items(MapRepository.allMaps) { mapDef ->
                val isMine   = if (!hasVoted) selectedLocal == mapDef.id else myVoteId == mapDef.id
                val isTheirs = hasVoted && oppVoteId == mapDef.id
                val border = when {
                    isMine   -> BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
                    isTheirs -> BorderStroke(3.dp, MaterialTheme.colorScheme.secondary)
                    else     -> null
                }

                Card(
                    border   = border,
                    modifier = Modifier
                        .aspectRatio(1f)
                        .then(if (!hasVoted) Modifier.clickable { selectedLocal = mapDef.id } else Modifier)
                ) {
                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter            = painterResource(mapDef.thumbnailRes),
                            contentDescription = mapDef.name,
                            modifier           = Modifier.weight(1f)
                        )
                        Text(mapDef.name, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                selectedLocal?.let { pick ->
                    scope.launch {
                        roomDoc.update("gameState.battleships.mapVotes.$userName", pick)
                    }
                    hasVoted = true
                }
            },
            enabled = !hasVoted && selectedLocal != null,
            modifier= Modifier.fillMaxWidth()
        ) {
            Text("Vote")
        }
    }
}
