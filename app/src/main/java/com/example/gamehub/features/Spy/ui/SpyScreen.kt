package com.example.gamehub.features.spy.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gamehub.R
import com.example.gamehub.ui.SpriteMenuButton
import GameBoxFontFamily
import androidx.navigation.NavController

@Composable
fun SpyScreen(
    navController: NavController
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Background image
        Image(
            painter = painterResource(id = R.drawable.game_box_bg1),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(50.dp))

            Text(
                text = "🕵️ Spy Mode",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = GameBoxFontFamily,
                    fontSize = 28.sp
                ),
                color = androidx.compose.ui.graphics.Color(0xFFc08cdc)
            )
            
            Spacer(Modifier.height(16.dp))
            
            Text(
                text = "Single-player mission: uncover the spy!",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = GameBoxFontFamily,
                    fontSize = 18.sp
                ),
                color = androidx.compose.ui.graphics.Color(0xFFc08cdc),
                textAlign = TextAlign.Center
            )
            
            Spacer(Modifier.height(32.dp))
            
            SpriteMenuButton(
                text = "Start Spy Game",
                onClick = {
                    navController.navigate("spy_game")
                },
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .height(70.dp)
            )

            Spacer(Modifier.weight(1f))

            SpriteMenuButton(
                text = "Back",
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .height(70.dp)
            )

            Spacer(Modifier.height(20.dp))
        }
    }
}
