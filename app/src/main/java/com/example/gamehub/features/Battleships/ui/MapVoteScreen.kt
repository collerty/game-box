package com.example.gamehub.features.battleships.ui

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    Box(Modifier.fillMaxSize()) {
        // --- Background Image ---
        Image(
            painter = painterResource(com.example.gamehub.R.drawable.bg_battleships),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // --- Foreground content: transparent rounded card ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- Title ---
            Text(
                text = "Map Voting",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                softWrap = true,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            Spacer(Modifier.height(12.dp))

            // --- "Opponent voted..." text ---
            Text(
                text = when {
                    !hasVoted && oppVote != null ->
                        "Opponent voted for ${
                            MapRepository.allMaps.first { it.id == oppVote }.name
                        }\nYour turn to pick"
                    !hasVoted -> "Choose a map and press Vote"
                    else -> "Waiting for opponent to vote…"
                },
                fontSize = 18.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            // --- Transparent voting box ---
            Box(
                Modifier
                    .background(Color(0xCC222222), shape = MaterialTheme.shapes.medium)
                    .padding(18.dp)
                    .fillMaxWidth()
                    .heightIn(min = 250.dp, max = 540.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // --- The grid scrolls only, button stays visible ---
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        content = {
                            items(MapRepository.allMaps) { mapDef ->
                                val border = when {
                                    !hasVoted && selectedMap == mapDef.id ->
                                        BorderStroke(3.dp, Color.White)
                                    hasVoted && myVote == mapDef.id ->
                                        BorderStroke(3.dp, Color.White)
                                    oppVote == mapDef.id ->
                                        BorderStroke(3.dp, Color(0xFFBB86FC)) // Secondary
                                    else -> null
                                }

                                Card(
                                    border   = border,
                                    modifier = Modifier
                                        .aspectRatio(0.85f)
                                        .padding(4.dp)
                                        .then(
                                            if (!hasVoted) Modifier.clickable { selectedMap = mapDef.id }
                                            else Modifier
                                        ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0x99111111)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        // Map preview image
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(1f)
                                        ) {
                                            Image(
                                                painter = painterResource(mapDef.previewRes),
                                                contentDescription = mapDef.name,
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        // Map name
                                        Text(
                                            mapDef.name,
                                            color = Color.White,
                                            textAlign = TextAlign.Center,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            fontSize = 15.sp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    )

                    Spacer(Modifier.height(20.dp))

                    if (!hasVoted && selectedMap != null) {
                        Button(
                            onClick = { doVote() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF444444),
                                contentColor = Color.White
                            )
                        ) {
                            Text("Vote", fontSize = 18.sp)
                        }
                    }
                }
            }
        }
    }
}
