
package com.example.gamehub.ui

import GameBoxFontFamily
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stairs
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import androidx.navigation.NavController
import com.example.gamehub.R
import com.example.gamehub.model.Game
import com.example.gamehub.ui.GameBoxFontFamily
import com.example.gamehub.ui.components.NinePatchBorder

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
            contentScale = ContentScale.Crop
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
                    // SCROLLABLE COLUMN!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        GameDetailsContent(gameId)
                    }
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Text(
            text = "Sink the enemy fleet before they sink yours!",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = Color(0xFFc08cdc)
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "How to Play",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFFc08cdc),
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            ),
            textAlign = TextAlign.Center
        )

        // Voting for the Map
        Text(
            text = "🗺️ Map Voting",
            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFc08cdc), fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "• Both players vote for a map (20 seconds).\n• If both choose the same map, it's picked. If not, a map between the two votes is chosen.",
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFc08cdc)),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )

        // Ship Placement
        Text(
            text = "🚢 Ship Placement",
            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFc08cdc), fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "• Each player places 5 ships on the map.\n• Ships can be rotated and repositioned before confirming.\n• Tap a placed ship to retract it and place again.",
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFc08cdc)),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )

        // Gameplay
        Text(
            text = "🎯 Battle Phase",
            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFc08cdc), fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "• Take turns attacking tiles on the opponent's map.\n• Visual feedback tells you if you hit or missed.\n• The first to destroy all enemy ships wins!",
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFc08cdc)),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )

        // Power-Ups Section
        Text(
            text = "💥 Power-Ups (once per round)",
            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFc08cdc), fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "• **Mine (2 energy):** Place on your map. If hit, rebounds on enemy’s map in 3 random spots. You can attack after using a mine.\n" +
                    "• **Bomb (3 energy):** Destroy a 2x2 square. Using this skips your attack turn.\n" +
                    "• **Laser (4 energy):** Destroy an entire row or column. Using this skips your attack turn.",
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFc08cdc)),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )

        // Energy
        Text(
            text = "⚡ Energy System",
            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFc08cdc), fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "• Destroying an enemy ship: +1 energy\n• Losing your own ship: +2 energy",
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFc08cdc)),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(6.dp))
        Text(
            text = "Strategize, use power-ups wisely, and sink the fleet before they sink you!",
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFFc08cdc),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            ),
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
            textAlign = TextAlign.Center,
            color = Color(0xFFc08cdc)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Great for groups of 4 people. Laughter guaranteed! \n\nRoll the dice by shaking your phone to move around the game board, but be careful because opponents can send your pawn right back to the start if they land on it! \n\nTo get your pawn onto the game board, you must roll a 6. \n\nYour task is to get all of your pawns into the victory zone, for this you must traverse the entire game board with all your pawns before the other players do.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = Color(0xFFc08cdc)
        )
    }
}

@Composable
fun SpaceInvadersInfo() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Can you defeat all the alien invaders and defend Earth?",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = Color(0xFFc08cdc)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "The classic arcade game from the 80s! \n\nMove to the left or right by clicking the buttons, or by clicking on the tilt phone icon on the top right. \n\nShooting enemies gives you points, the UFO is an especially valuable target! \n\nBunkers will defend you from enemy bullets, but only for a while... \n\nCan you get the highest score out of all the players?",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = Color(0xFFc08cdc)
        )
    }
}


@Composable
fun WhereAndWhenInfo() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Guess the place and year of famous world events! Compete for accuracy.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = Color(0xFFc08cdc)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "A multiplayer trivia game. Each round, see a photo and hint, then guess the location on the map and the year it happened. The closer you are, the more points you get!",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = Color(0xFFc08cdc)
        )
    }
}


@Composable
fun SpyInfo() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Text(
            text = "Unmask the spy—or blend in and escape discovery!",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = Color(0xFFc08cdc)
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "How to Play",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFFc08cdc),
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            ),
            textAlign = TextAlign.Center
        )

        // Setup
        Text(
            text = "🎭 Game Setup",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFFc08cdc), fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "• Players are secretly assigned roles: most are agents, but one is the spy.\n• All agents know the secret location. The spy does NOT.",
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFc08cdc)),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )

        // Questioning
        Text(
            text = "❓ Ask & Answer",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFFc08cdc), fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "• Players take turns asking each other questions about the secret location.\n• Agents must answer truthfully (but not too obvious!).\n• The spy tries to blend in, guessing based on the answers.",
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFc08cdc)),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )

        // End of Game
        Text(
            text = "🔎 Accuse or Escape",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFFc08cdc), fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "• At any point, players can vote to accuse someone of being the spy.\n• If the spy is caught, the agents win!\n• If the spy can guess the location before being caught, the spy wins!",
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFc08cdc)),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(6.dp))
        Text(
            text = "Bluff, deduce, and outsmart your friends. Will you reveal the spy—or get away with the secret?",
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFFc08cdc),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            ),
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


@Composable
fun ScreamOSaurInfo() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.dinasourtemplate),
            contentDescription = "Scream-O-Saur Dinosaur",
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .padding(bottom = 8.dp)
        )

        InfoRow(
            icon = {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = "Microphone Icon",
                    tint = Color(0xFFc08cdc),
                    modifier = Modifier.size(32.dp)
                )
            },
            text = "Use your voice! Shout, sing, or roar to make the dinosaur jump."
        )

        InfoRow(
            icon = {
                Icon(
                    Icons.Default.ArrowUpward,
                    contentDescription = "Jump Icon",
                    tint = Color(0xFFc08cdc),
                    modifier = Modifier.size(32.dp)
                )
            },
            text = "Time your jumps to clear the approaching cacti."
        )

        Text(
            text = "The sound meter shows your volume. Fill it past the line to jump!",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = Color(0xFFc08cdc)
        )
        MiniSoundMeter()

        Text(
            text = "The game gets faster as you score higher. Good luck!",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = Color(0xFFc08cdc)
        )
    }
}

@Composable
fun MiniSoundMeter() {
    Box(
        modifier = Modifier
            .height(20.dp)
            .fillMaxWidth(0.8f)
            .background(Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .border(1.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.6f) // Example fill level
                .background(Color(0xFFc08cdc), RoundedCornerShape(10.dp))
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(2.dp)
                .align(Alignment.Center)
                .background(Color.White)
        )
    }
}

@Composable
private fun InfoRow(icon: @Composable () -> Unit, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFc08cdc),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun MemoryMatchInfo() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Text(
            text = "Test your memory and find all the pairs!",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = Color(0xFFc08cdc),
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        MiniCardGrid()

        InfoRow(
            icon = {
                Icon(
                    Icons.Default.Style,
                    contentDescription = "Find Pairs",
                    tint = Color(0xFFc08cdc),
                    modifier = Modifier.size(32.dp)
                )
            },
            text = "Tap cards to flip them over and find matching pairs."
        )

        InfoRow(
            icon = {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = "Timer",
                    tint = Color(0xFFc08cdc),
                    modifier = Modifier.size(32.dp)
                )
            },
            text = "Match all the cards before the timer runs out."
        )

        InfoRow(
            icon = {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = "Mistakes",
                    tint = Color(0xFFc08cdc),
                    modifier = Modifier.size(32.dp)
                )
            },
            text = "Be careful! Too many incorrect matches will end the game."
        )

        InfoRow(
            icon = {
                Icon(
                    Icons.Default.Stairs,
                    contentDescription = "Difficulty",
                    tint = Color(0xFFc08cdc),
                    modifier = Modifier.size(32.dp)
                )
            },
            text = "Choose from multiple difficulties for a greater challenge."
        )
    }
}

@Composable
private fun MiniCardGrid() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        // Column 1
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(
                modifier = Modifier.size(50.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF9092D8)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(Icons.Default.Style, contentDescription = null, tint = Color.White)
                }
            }
            Card(
                modifier = Modifier.size(50.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.old_card_back),
                    contentDescription = "Card Back"
                )
            }
        }
        // Column 2
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(
                modifier = Modifier.size(50.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.old_card_back),
                    contentDescription = "Card Back"
                )
            }
            Card(
                modifier = Modifier.size(50.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF9092D8)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(Icons.Default.Style, contentDescription = null, tint = Color.White)
                }
            }
        }
    }
}
@Composable
fun TriviatoeInfo() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Text(
            text = "Answer questions. Claim the board. Connect four!",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = Color(0xFFc08cdc)
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "How to Play",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFFc08cdc),
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            ),
            textAlign = TextAlign.Center
        )

        // Assignment of X and O
        Text(
            text = "❌⭕ Get Your Symbol",
            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFc08cdc), fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "• At the start, you are assigned either X or O. Your opponent gets the other symbol.",
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFc08cdc)),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )

        // Question Round
        Text(
            text = "❓ Question Round",
            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFc08cdc), fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "• A general question appears, with a 10 second timer.\n• The first player to answer correctly wins the round!\n• If both answer incorrectly or time runs out, the winner is chosen randomly.",
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFc08cdc)),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )

        // Placing Pieces
        Text(
            text = "🟦 Place Your Symbol",
            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFc08cdc), fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "• The winner places their X or O anywhere on a 10x10 board.\n• Your goal: connect 4 of your symbols in a row—horizontally, vertically, or diagonally.",
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFc08cdc)),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )

        // Strategy and Win
        Text(
            text = "🧠 Outplay & Win",
            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFc08cdc), fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "• Place strategically to block your opponent while setting up your own victory!\n• First to connect 4 wins the game.",
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFc08cdc)),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(6.dp))
        Text(
            text = "Quick thinking and smart moves—can you outwit your opponent and connect four first?",
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFFc08cdc),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun CodenamesInfo() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Text(
            text = "Give clever clues. Guess the right words. Outsmart the other team!",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = Color(0xFFc08cdc)
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "How to Play",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFFc08cdc),
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            ),
            textAlign = TextAlign.Center
        )

        // Setup
        Text(
            text = "🧩 Teams & Roles",
            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFc08cdc), fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "• Play in two teams: Red and Blue.\n• Each team has one Spymaster and one or more Field Operatives.",
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFc08cdc)),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )

        // Clue giving
        Text(
            text = "💬 Spymaster’s Clue",
            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFc08cdc), fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "• The Spymaster gives a one-word clue and a number (e.g. \"Animal: 2\").\n• The clue relates to words on the table belonging to their team.",
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFc08cdc)),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )

        // Guessing
        Text(
            text = "🔍 Guessing",
            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFc08cdc), fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "• Field Operatives discuss (use chat!) and guess the words the Spymaster meant.\n• Click to select a word. Be careful! Guess only your team's words.",
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFc08cdc)),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )

        // Danger and win
        Text(
            text = "⚠️ End & Danger",
            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFc08cdc), fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "• If you guess a neutral word, your turn ends.\n• If you guess the assassin word, your team instantly loses!\n• First team to find all their words wins.",
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFc08cdc)),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )

        // Chat feature
        Text(
            text = "💬 Team Chat",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFFc08cdc),
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "• Use the in-game chat to talk, discuss clues, and plan your guesses with teammates.",
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFc08cdc)),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(6.dp))
        Text(
            text = "Think fast, make clever connections, and help your team win!",
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFFc08cdc),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            ),
            textAlign = TextAlign.Center
        )
    }
}

// ====== GAME INFO DISPATCHER ======

@Composable
fun GameDetailsContent(gameId: String) {
    when (gameId) {
        "battleships" -> BattleshipsInfo()
        "ohpardon" -> OhPardonInfo()
        "spy" -> SpyInfo()
        "jorisjump" -> JorisJumpInfo()
        "spaceinvaders" -> SpaceInvadersInfo()
        "screamosaur" -> ScreamOSaurInfo()
        "memoryMatching" -> MemoryMatchInfo()
        "triviatoe" -> TriviatoeInfo()
        "codenames" -> CodenamesInfo()
        "whereandwhen" -> WhereAndWhenInfo()
        // Add further games here...
        else -> Text("No info available yet.", style = MaterialTheme.typography.bodyLarge)
    }
}
