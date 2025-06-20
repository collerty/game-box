package com.example.gamehub.ui

import GameBoxFontFamily
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.gamehub.model.Game
import com.example.gamehub.navigation.NavRoutes
import com.example.gamehub.ui.components.NinePatchBorder
import androidx.compose.ui.Alignment
import com.example.gamehub.R
import com.example.gamehub.audio.SoundManager
import androidx.compose.foundation.background

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun GamesListScreen(navController: NavController) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        context.stopService(Intent(context, com.example.gamehub.MusicService::class.java))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = com.example.gamehub.R.drawable.game_box_bg1),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // game_list.png is now OUTSIDE the border!
            Image(
                painter = painterResource(id = com.example.gamehub.R.drawable.game_list),
                contentDescription = "Game List Title",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 16.dp)
                    .height(70.dp)
            )

            // Big border around all cards
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                NinePatchBorder(
                    modifier = Modifier.matchParentSize(),
                    drawableRes = com.example.gamehub.R.drawable.game_list_border
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    items(Game.all) { game ->
                        GameCard(
                            game = game,
                            navController = navController
                        )
                    }
                }
            }

            // Spacer to push button to bottom if you want
            Spacer(modifier = Modifier.height(10.dp))

            // BACK BUTTON OUTSIDE THE BORDER, centered!
            SpriteMenuButton(
                text = "Back",
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .height(70.dp), // Or match settings menu height
                normalRes = com.example.gamehub.R.drawable.menu_button_long,
                pressedRes = com.example.gamehub.R.drawable.menu_button_long_pressed,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = GameBoxFontFamily,
                    fontSize = 24.sp
                )
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun GameCard(
    game: Game,
    navController: NavController
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(bottom = 10.dp)
    ) {
        // Border around the IMAGE (with padding to show border)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
        ) {
            Image(
                painter = painterResource(id = game.imageRes),
                contentDescription = game.title,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp), // <--- ADJUST THIS VALUE TO YOUR TASTE
                contentScale = ContentScale.Crop
            )
            NinePatchBorder(
                modifier = Modifier.matchParentSize(),
                drawableRes = com.example.gamehub.R.drawable.border_image1
            )
        }
        // Game info row: Icon, Name (centered), Info Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 18.dp) // Moves everything a bit to center
                .clickable {
                    SoundManager.playEffect(context, R.raw.menu_button_press)
                    if (game.online) {
                        navController.navigate(NavRoutes.LOBBY_MENU.replace("{gameId}", game.id))
                    } else {
                        val route = when (game.id) {
                            "spy"         -> NavRoutes.SPY_GAME
                            "jorisjump"   -> NavRoutes.JORISJUMP_GAME
                            "screamosaur" -> NavRoutes.SCREAMOSAUR_GAME
                            "whereandwhen" -> NavRoutes.WHERE_AND_WHEN_GAME
                            "spaceinvaders" -> NavRoutes.SPACE_INVADERS_PREGAME
                            "memoryMatching" -> NavRoutes.MEMORY_MATCHING_GAME
                            else          -> NavRoutes.SPY_GAME
                        }
                        navController.navigate(route)
                    }
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Game Icon with border, bigger size
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .padding(end = 3.dp), // more padding to separate from text
                contentAlignment = Alignment.Center
            ) {
                if (game.id == "codenames" || game.id == "spy" || game.id == "jorisjump" || game.id == "whereandwhen") {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFE6E6FA)) // Light purple background
                    )
                }
                Image(
                    painter = painterResource(id = game.iconRes),
                    contentDescription = "Game Icon",
                    modifier = Modifier
                        .fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                NinePatchBorder(
                    modifier = Modifier.matchParentSize(),
                    drawableRes = com.example.gamehub.R.drawable.border_game_icon
                )
            }

            // Centered Title, less weight so icons get more space
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = game.title,
                    fontFamily = GameBoxFontFamily,
                    fontSize = if (game.title.length > 11) 16.sp else 20.sp,
                    color = Color(0xFFc08cdc),
                    maxLines = 1
                )
            }

            // Info Icon with border, bigger size
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .padding(start = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = com.example.gamehub.R.drawable.ic_info),
                    contentDescription = "Info",
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            SoundManager.playEffect(context, R.raw.menu_button_press)
                            navController.navigate(
                                NavRoutes.GAME_INFO.replace("{gameId}", game.id)
                            )
                        },
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}


