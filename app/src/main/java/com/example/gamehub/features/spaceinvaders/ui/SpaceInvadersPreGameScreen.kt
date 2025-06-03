package com.example.gamehub.features.spaceinvaders.ui

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.gamehub.features.spaceinvaders.classes.SpaceInvadersViewModel
import com.example.gamehub.navigation.NavRoutes
import androidx.compose.foundation.lazy.items


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
            .padding(16.dp)
    ) {
        Text("Enter your name:")

        OutlinedTextField(
            value = playerName,
            onValueChange = { viewModel.onPlayerNameChanged(it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Name") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val encodedName = Uri.encode(playerName)
                navController.navigate("${NavRoutes.SPACE_INVADERS_GAME}/$encodedName")
            },
            enabled = playerName.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Play")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text("High Scores")

        LazyColumn {
            items(highScores) { score ->
                Text("${score.player}: ${score.score}")
            }
        }

    }
}
