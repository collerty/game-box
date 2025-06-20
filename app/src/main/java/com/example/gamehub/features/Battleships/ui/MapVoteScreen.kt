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
    val db = FirebaseFirestore.getInstance()
    val roomRef = remember { db.collection("rooms").document(code) }
    val scope = rememberCoroutineScope()
    val meUid = FirebaseAuth.getInstance().uid ?: return

    // State
    var mapVotes by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var chosenMap by remember { mutableStateOf<Int?>(null) }
    var selectedMap by remember { mutableStateOf<Int?>(null) }
    var hasVoted by remember { mutableStateOf(false) }
    var votingStartTime by remember { mutableStateOf<Long?>(null) }
    var timerMillisLeft by remember { mutableStateOf(20_000L) }

    // ---- Listen for game state (mapVotes, chosenMap, votingStartTime) ----
    DisposableEffect(code) {
        val lis = roomRef.addSnapshotListener { snap, _ ->
            val gs = snap?.get("gameState") as? Map<*, *>
            val bm = gs?.get("battleships") as? Map<*, *>
            @Suppress("UNCHECKED_CAST")
            val rawVotes = bm?.get("mapVotes") as? Map<String, Number>
            mapVotes = rawVotes?.mapValues { it.value.toInt() } ?: emptyMap()
            chosenMap = (bm?.get("chosenMap") as? Number)?.toInt()
            votingStartTime = (bm?.get("votingStartTime") as? Number)?.toLong()
        }
        onDispose { lis.remove() }
    }

    // ---- On entry, ensure votingStartTime is set in Firestore (by one client only) ----
    LaunchedEffect(code) {
        scope.launch {
            // Use Firestore server timestamp so all clients sync properly
            val snap = roomRef.get().await()
            val bm = ((snap.get("gameState") as? Map<*, *>)?.get("battleships") as? Map<*, *>)
            if (bm?.get("votingStartTime") == null) {
                // Only one client (host, or lowest uid) should do this
                val players = snap.get("players") as? List<Map<*, *>>
                val hostUid = players?.firstOrNull()?.get("uid") as? String
                if (hostUid == meUid) {
                    roomRef.update("gameState.battleships.votingStartTime", System.currentTimeMillis())
                }
            }
        }
    }

    // ---- Timer logic based on votingStartTime from Firestore ----
    LaunchedEffect(votingStartTime, chosenMap) {
        if (chosenMap == null && votingStartTime != null) {
            while (timerMillisLeft > 0 && chosenMap == null) {
                timerMillisLeft = 20_000L - (System.currentTimeMillis() - votingStartTime!!)
                if (timerMillisLeft < 0) timerMillisLeft = 0
                kotlinx.coroutines.delay(100)
            }
            // Timer ended, resolve if not already resolved
            if (chosenMap == null) {
                scope.launch {
                    // Only resolve if you are host or lowest uid
                    val isResolver = mapVotes.keys.minOrNull() == meUid || mapVotes.isEmpty()
                    if (isResolver) {
                        val allMaps = MapRepository.allMaps
                        val myVote = mapVotes[meUid]
                        val oppVote = mapVotes.keys.firstOrNull { it != meUid }?.let { mapVotes[it] }
                        val selected = when {
                            mapVotes.size == 2 && myVote != null && oppVote != null -> {
                                if (myVote == oppVote) myVote
                                else listOf(myVote, oppVote).random()
                            }
                            mapVotes.size == 1 && myVote != null -> myVote
                            mapVotes.size == 1 && oppVote != null -> oppVote
                            else -> allMaps.random().id
                        }
                        roomRef.update("gameState.battleships.chosenMap", selected)
                    }
                }
            }
        }
    }

    // ---- Resolve instantly if both voted (no need to wait for timer) ----
    LaunchedEffect(mapVotes, chosenMap) {
        if (chosenMap == null && mapVotes.size == 2) {
            // Only resolver client does this
            val isResolver = mapVotes.keys.minOrNull() == meUid
            if (isResolver) {
                val myVote = mapVotes[meUid]
                val oppVote = mapVotes.keys.firstOrNull { it != meUid }?.let { mapVotes[it] }
                val selected = if (myVote == oppVote) myVote else listOf(myVote, oppVote).random()
                roomRef.update("gameState.battleships.chosenMap", selected)
            }
        }
    }

    // ---- When chosenMap appears, navigate! ----
    LaunchedEffect(chosenMap) {
        if (chosenMap != null) {
            val route = NavRoutes.BATTLE_PLACE
                .replace("{code}", code)
                .replace("{userName}", Uri.encode(userName))
                .replace("{mapId}", chosenMap.toString())
            navController.navigate(route) {
                popUpTo(0)
                launchSingleTop = true
            }
        }
    }

    // ---- Vote submission ----
    fun doVote() {
        selectedMap?.let { choice ->
            hasVoted = true
            scope.launch {
                roomRef.update("gameState.battleships.mapVotes.$meUid", choice).await()
            }
        }
    }

    // ---- Vote states for UI ----
    val opponentUid = mapVotes.keys.firstOrNull { it != meUid }
    val myVote = mapVotes[meUid]
    val oppVote = opponentUid?.let { mapVotes[it] }

    // ---- UI ----
    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(com.example.gamehub.R.drawable.bg_battleships),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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

            // --- Status / Timer
            Text(
                text = when {
                    chosenMap != null -> "Map chosen! Loading…"
                    !hasVoted && oppVote != null -> "Opponent voted for ${MapRepository.allMaps.firstOrNull { it.id == oppVote }?.name ?: "?"}\nYour turn to pick"
                    !hasVoted -> "Choose a map and press Vote"
                    else -> "Waiting for opponent to vote…"
                } + if (chosenMap == null) "\nTime left: ${((timerMillisLeft/1000).coerceAtLeast(0))}s" else "",
                fontSize = 18.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

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
                                    !hasVoted && selectedMap == mapDef.id -> BorderStroke(3.dp, Color.White)
                                    hasVoted && myVote == mapDef.id -> BorderStroke(3.dp, Color.White)
                                    oppVote == mapDef.id -> BorderStroke(3.dp, Color(0xFFBB86FC))
                                    else -> null
                                }
                                Card(
                                    border = border,
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

