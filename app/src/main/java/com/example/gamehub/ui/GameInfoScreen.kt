package com.example.gamehub.ui

import GameBoxFontFamily
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.gamehub.model.Game
import com.example.gamehub.ui.components.NinePatchBorder
import com.example.gamehub.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun GameInfoScreen(navController: NavController, gameId: String) {
    val game = Game.all.find { it.id == gameId }
    val gameTitle = game?.title ?: "Game Info"

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Background image
        Image(
            painter = painterResource(id = R.drawable.game_box_bg1),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Top
        ) {
            // Game name at the top
            Text(
                text = gameTitle,
                fontFamily = GameBoxFontFamily,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = Color(0xFFc08cdc)
            )

            // Border with per-game info inside
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                NinePatchBorder(
                    modifier = Modifier.matchParentSize(),
                    drawableRes = R.drawable.game_list_border
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    GameDetailsContent(gameId)
                }
            }

            Spacer(Modifier.height(16.dp))
            // Styled back button, outside border
            SpriteMenuButton(
                text = "Back",
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .height(70.dp),
                normalRes = R.drawable.menu_button_long,
                pressedRes = R.drawable.menu_button_long_pressed,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = GameBoxFontFamily,
                    fontSize = 24.sp
                )
            )
        }
    }
}

// ====== GAME-SPECIFIC INFO COMPOSABLES ======

@Composable
fun BattleshipsInfo() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Sink the enemy fleet. Classic naval strategy!",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Players: 2", style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun OhPardonInfo() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "A fast-paced party game of luck and humor.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Great for groups of 4 people. Laughter guaranteed! Roll the dice by shaking your phone to move around the game board, but be careful because opponents can send your pawn right back to the start if they land on it! To get your pawn onto the game board, you must roll a 6. Your task is to get all of your pawns into the victory zone, for this you must traverse the entire game board with all your pawns before the other players do.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SpaceInvadersInfo() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Can you defeat all the alien invaders and defend Earth?",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "The classic arcade game from the 80s! Move to the left or right by clicking the buttons, or by clicking on the tilt phone icon on the top right. Shooting enemies gives you points, the UFO is an especially valuable target! Bunkers will defend you from enemy bullets, but only for a while... Can you get the highest score out of all the players?", style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SpyInfo() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Can you uncover the spy among the players?",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Social deduction. Bluff and guess!", style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun JorisJumpInfo() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Jump your way to a new high score!",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Fast reflexes needed. Compete for the top!",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

// Add more per-game info as needed...

// ====== GAME INFO DISPATCHER ======

@Composable
fun GameDetailsContent(gameId: String) {
    when (gameId) {
        "battleships" -> BattleshipsInfo()
        "ohpardon" -> OhPardonInfo()
        "spy" -> SpyInfo()
        "jorisjump" -> JorisJumpInfo()
        "spaceinvaders" -> SpaceInvadersInfo()
        // Add further games here...
        else -> Text("No info available yet.", style = MaterialTheme.typography.bodyLarge)
    }
}
