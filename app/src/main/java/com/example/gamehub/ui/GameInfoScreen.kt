package com.example.gamehub.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gamehub.model.Game

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameInfoScreen(navController: NavController, gameId: String) {
    val game = Game.all.find { it.id == gameId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = game?.title ?: "Game Info") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_media_previous),
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                // Show game icon if available
                game?.let {
                    Image(
                        painter = painterResource(id = it.iconRes),
                        contentDescription = it.title,
                        modifier = Modifier
                            .size(80.dp)
                            .padding(bottom = 16.dp)
                    )
                }

                Text(
                    text = "Info about ${game?.title ?: "Unknown Game"}",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Placeholder info for each game
                Text(
                    text = when (gameId) {
                        "battleships" -> "Sink the enemy fleet. Classic naval strategy!"
                        "ohpardon" -> "A fast-paced party game of wit and humor."
                        "spy" -> "Can you uncover the spy among the players?"
                        "jorisjump" -> "Jump your way to a new high score!"
                        "screamosaur" -> "Roar as loud as you can to win!"
                        "memoryMatching" -> "Match all the pairs to win."
                        "triviatoe" -> "Answer trivia to conquer the board."
                        "codenames" -> "Guess words with the help of clever clues."
                        "whereandwhen" -> "A social deduction game about time and place."
                        "spaceinvaders" -> "Classic arcade action. Shoot down invaders!"
                        else -> "No info available yet."
                    },
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Back")
                }
            }
        }
    }
}
