package com.example.gamehub.ui

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gamehub.model.Game
import com.example.gamehub.navigation.NavRoutes

@Composable
fun GamesListScreen(navController: NavController) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        context.stopService(Intent(context, com.example.gamehub.MusicService::class.java))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(Game.all) { game ->
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (game.online) {
                            // go to lobby menu for this game
                            navController.navigate(
                                NavRoutes.LOBBY_MENU
                                    .replace("{gameId}", game.id)
                            )
                        } else {
                            // single-player: dispatch to its own screen
                            val route = when (game.id) {
                                "spy"         -> NavRoutes.SPY_GAME
                                "jorisjump"   -> NavRoutes.JORISJUMP_GAME
                                "screamosaur" -> NavRoutes.SCREAMOSAUR_GAME
                                else          -> NavRoutes.SPY_GAME
                            }
                            navController.navigate(route)
                        }
                    }
            ) {
                Row(modifier = Modifier.padding(16.dp)) {
                    Image(
                        painter = painterResource(id = game.iconRes),
                        contentDescription = game.title,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(text = game.title)
                }
            }
        }
    }
}
