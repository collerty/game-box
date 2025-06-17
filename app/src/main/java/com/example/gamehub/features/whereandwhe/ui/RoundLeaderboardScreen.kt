package com.example.gamehub.features.whereandwhen.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gamehub.features.whereandwhe.model.WWPlayerRoundResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class LeaderboardPlayerItem(
    val playerId: String,
    val playerName: String,
    val scoreBeforeRound: Int,
    val scoreGainedThisRound: Int,
    val newTotalScore: Int,
    var currentDisplayScore: Int = scoreBeforeRound, // For animation
    var rank: Int = 0,
    var previousRank: Int = 0 // To detect rank changes
)

@Composable
fun RoundLeaderboardScreen(
    playerResults: Map<String, WWPlayerRoundResult>, // UID to RoundResult
    roomPlayers: List<Map<String, Any>>, // To get names and previous total scores
    onFinished: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var leaderboardItems by remember { mutableStateOf<List<LeaderboardPlayerItem>>(emptyList()) }
    var animationPhase by remember { mutableStateOf(0) } // 0: init, 1: animating scores, 2: animating ranks, 3: finished

    LaunchedEffect(playerResults, roomPlayers) {
        // 1. Prepare initial leaderboard items
        val initialItems = roomPlayers.mapNotNull { playerMap ->
            val uid = playerMap["uid"] as? String ?: return@mapNotNull null
            val name = playerMap["name"] as? String ?: "Player"
            val currentTotalScore = (playerMap["totalScore"] as? Long)?.toInt() ?: 0 // This is total AFTER last round
            val lastRoundScore = playerResults[uid]?.roundScore ?: 0
            val scoreBeforeThisRound = currentTotalScore - lastRoundScore

            LeaderboardPlayerItem(
                playerId = uid,
                playerName = name,
                scoreBeforeRound = scoreBeforeThisRound,
                scoreGainedThisRound = lastRoundScore,
                newTotalScore = currentTotalScore,
                currentDisplayScore = scoreBeforeThisRound
            )
        }.sortedByDescending { it.scoreBeforeRound } // Initial sort by score before

        // Assign initial ranks
        initialItems.forEachIndexed { index, item ->
            item.previousRank = index + 1
            item.rank = index + 1
        }
        leaderboardItems = initialItems
        animationPhase = 1 // Move to score animation
    }

    // Score Animation Phase
    LaunchedEffect(animationPhase) {
        if (animationPhase == 1) {
            leaderboardItems.forEach { item ->
                scope.launch {
                    animate(
                        initialValue = item.scoreBeforeRound.toFloat(),
                        targetValue = item.newTotalScore.toFloat(),
                        animationSpec = tween(durationMillis = 1000 + (item.scoreGainedThisRound * 10).coerceAtMost(1000), easing = LinearEasing)
                    ) { value, _ ->
                        // Find item in the list and update its display score (important to update the list state)
                        leaderboardItems = leaderboardItems.map { listItem ->
                            if (listItem.playerId == item.playerId) {
                                listItem.copy(currentDisplayScore = value.toInt())
                            } else {
                                listItem
                            }
                        }
                    }
                }
            }
            delay(2000) // Wait for score animations to likely finish
            animationPhase = 2 // Move to rank animation
        }
    }

    // Rank Animation Phase (Re-sorting)
    LaunchedEffect(animationPhase) {
        if (animationPhase == 2) {
            // Update ranks based on new total scores
            val sortedByNewScore = leaderboardItems
                .sortedByDescending { it.newTotalScore }
                .mapIndexed { index, item -> item.copy(rank = index + 1) }

            leaderboardItems = sortedByNewScore // This will trigger recomposition and items will re-sort
            delay(2000) // Hold the final ranks for a bit
            animationPhase = 3
        }
    }

    // Auto-proceed after a delay in the finished phase
    LaunchedEffect(animationPhase) {
        if (animationPhase == 3) {
            delay(3000) // Show leaderboard for 3 seconds
            onFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2C3E50)) // Dark background
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "LEADERBOARD",
                fontSize = 36.sp,
                fontFamily = arcadeFontFamily_WhereAndWhen,
                color = Color.Yellow,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    items = leaderboardItems.sortedBy { it.rank }, // Always display sorted by current rank
                    key = { _, item -> item.playerId } // Stable key for animations
                ) { index, playerItem ->
                    LeaderboardItemRow(
                        playerItem = playerItem,
                        rank = playerItem.rank, // Use the animated/updated rank
                        isTop3 = playerItem.rank <= 3
                    )
                }
            }

            if (animationPhase < 3) {
                // Show a loading or animating indicator if needed
            } else {
                Button(onClick = onFinished, modifier = Modifier.padding(top = 24.dp)) {
                    Text("CONTINUE", fontFamily = arcadeFontFamily_WhereAndWhen)
                }
            }
        }
    }
}

@Composable
fun LeaderboardItemRow(
    playerItem: LeaderboardPlayerItem,
    rank: Int,
    isTop3: Boolean,
    modifier: Modifier = Modifier // Added modifier as discussed for animations
) {
    val backgroundColor = when (rank) {
        1 -> Gold.copy(alpha = 0.3f) // Use the defined Gold
        2 -> Color.LightGray.copy(alpha = 0.3f) // Silver
        3 -> Color(0xFFCD7F32).copy(alpha = 0.3f) // Bronze
        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
    }
    val rankColor = if (isTop3) Color.Black else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "$rank.",
            fontSize = 20.sp,
            fontFamily = arcadeFontFamily_WhereAndWhen,
            color = rankColor,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(16.dp))
        Text(
            playerItem.playerName,
            fontSize = 20.sp,
            fontFamily = arcadeFontFamily_WhereAndWhen,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Start
        )
        Text(
            "${playerItem.currentDisplayScore}", // Animated score
            fontSize = 22.sp,
            fontFamily = arcadeFontFamily_WhereAndWhen,
            color = if (isTop3) Color.White else MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        // Optional: Rank change indicator
        if (playerItem.rank < playerItem.previousRank && playerItem.previousRank != 0) {
            Text(" ▲", color = Color.Green, fontWeight = FontWeight.Bold)
        } else if (playerItem.rank > playerItem.previousRank && playerItem.previousRank != 0) {
            Text(" ▼", color = Color.Red, fontWeight = FontWeight.Bold)
        }
    }
}