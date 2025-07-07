package com.example.gamehub.features.spaceinvaders.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.gamehub.features.spaceinvaders.classes.SpaceInvadersViewModel
import com.example.gamehub.features.spaceinvaders.ui.components.GameButton
import com.example.gamehub.features.spaceinvaders.ui.components.GameTextField
import com.example.gamehub.features.spaceinvaders.ui.components.GameText
import com.example.gamehub.features.spaceinvaders.ui.components.HighScoresList
import com.example.gamehub.features.spaceinvaders.ui.theme.SpaceInvadersTheme
import com.example.gamehub.navigation.NavRoutes

@Composable
fun SpaceInvadersPreGameScreen(
    navController: NavController,
    viewModel: SpaceInvadersViewModel = viewModel()
) {
    val playerName by viewModel.playerName.collectAsState()
    val highScores by viewModel.highScores.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceInvadersTheme.backgroundColor)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GameText(
            text = "Enter your name:",
            fontSize = SpaceInvadersTheme.FontSizes.normal
        )

        GameTextField(
            value = playerName,
            onValueChange = { viewModel.onPlayerNameChanged(it) },
            label = "Name"
        )

        Spacer(modifier = Modifier.height(16.dp))

        GameButton(
            text = "Play",
            onClick = {
                val encodedName = Uri.encode(playerName)
                navController.navigate("${NavRoutes.SPACE_INVADERS_GAME}/$encodedName")
            },
            enabled = playerName.isNotBlank(),
            modifier = Modifier.align(Alignment.CenterHorizontally),
            fontSize = SpaceInvadersTheme.FontSizes.normal
        )

        Spacer(modifier = Modifier.height(32.dp))

        HighScoresList(
            highScores = highScores,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}
