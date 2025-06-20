package com.example.gamehub.features.spaceinvaders.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.gamehub.R

@Composable
fun SpaceInvadersPreGameScreen(
    navController: NavController,
    viewModel: SpaceInvadersViewModel = viewModel()
) {
    val playerName by viewModel.playerName.collectAsState()
    val highScores by viewModel.highScores.collectAsState()

    val greenTextColor = Color(0xFF00FF00)

    val gameBoxFont = FontFamily(
        Font(R.font.gamebox_font, FontWeight.Bold)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Enter your name:",
            color = greenTextColor,
            fontFamily = gameBoxFont,
            fontSize = 24.sp
        )

        OutlinedTextField(
            value = playerName,
            onValueChange = { viewModel.onPlayerNameChanged(it) },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Name", color = greenTextColor, fontFamily = gameBoxFont, fontSize = 22.sp)
            },
            textStyle = TextStyle(color = greenTextColor, fontFamily = gameBoxFont, fontSize = 22.sp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Black,
                unfocusedContainerColor = Color.Black,
                focusedLabelColor = greenTextColor,
                unfocusedLabelColor = greenTextColor,
                focusedIndicatorColor = greenTextColor,
                unfocusedIndicatorColor = greenTextColor,
                cursorColor = greenTextColor
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val encodedName = Uri.encode(playerName)
                navController.navigate("${NavRoutes.SPACE_INVADERS_GAME}/$encodedName")
            },
            enabled = playerName.isNotBlank(),
            modifier = Modifier.align(Alignment.CenterHorizontally),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                contentColor = Color(0xFF00FF00) // Green
            )
        ) {
            Text("Play", fontFamily = gameBoxFont, fontSize = 24.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "High Scores",
            color = greenTextColor,
            fontFamily = gameBoxFont,
            fontSize = 26.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            items(highScores) { score ->
                Text(
                    "${score.player}: ${score.score}",
                    color = greenTextColor,
                    fontFamily = gameBoxFont,
                    fontSize = 22.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}
