package com.example.gamehub.features.codenames.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gamehub.features.codenames.model.CardColor
import com.example.gamehub.navigation.NavRoutes
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@Composable
fun CodenamesScreen(
    navController: NavController,
    roomId: String,
    userName: String
) {
    val db = Firebase.firestore
    var gameState by remember { mutableStateOf<Map<String, Any>?>(null) }
    var currentTurn by remember { mutableStateOf("RED") }
    var redWordsRemaining by remember { mutableStateOf(9) }
    var blueWordsRemaining by remember { mutableStateOf(8) }

    // Listen for game state updates
    LaunchedEffect(roomId) {
        db.collection("rooms").document(roomId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    val state = snapshot.get("gameState.codenames") as? Map<String, Any>
                    gameState = state
                    currentTurn = state?.get("currentTurn") as? String ?: "RED"
                    redWordsRemaining = (state?.get("redWordsRemaining") as? Number)?.toInt() ?: 9
                    blueWordsRemaining = (state?.get("blueWordsRemaining") as? Number)?.toInt() ?: 8
                }
            }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Red Team Panel (Left)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(end = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Red Team",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.Red
            )
            Text(
                "$redWordsRemaining words remaining",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Red
            )
            if (currentTurn == "RED") {
                Text(
                    "Your Turn!",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.Red,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // Cards Grid (Center)
        Column(
            modifier = Modifier
                .weight(2f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            @Suppress("UNCHECKED_CAST")
            val cards = (gameState?.get("cards") as? List<Map<String, Any>>) ?: emptyList()
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cards) { card ->
                    val word = card["word"] as? String ?: ""
                    val color = card["color"] as? String ?: "NEUTRAL"
                    val isRevealed = card["isRevealed"] as? Boolean ?: false

                    Card(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    !isRevealed -> Color.White
                                    color == "RED" -> Color.Red
                                    color == "BLUE" -> Color.Blue
                                    color == "NEUTRAL" -> Color.Gray
                                    color == "ASSASSIN" -> Color.Black
                                    else -> Color.White
                                }
                            )
                            .clickable(enabled = !isRevealed) {
                                // TODO: Handle card click
                            }
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = word,
                                textAlign = TextAlign.Center,
                                color = if (isRevealed && color == "ASSASSIN") Color.White else Color.Black,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Blue Team Panel (Right)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Blue Team",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.Blue
            )
            Text(
                "$blueWordsRemaining words remaining",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Blue
            )
            if (currentTurn == "BLUE") {
                Text(
                    "Your Turn!",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.Blue,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
} 