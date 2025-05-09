package com.example.gamehub.ui

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gamehub.navigation.NavRoutes

@Composable
fun MainMenu(navController: NavController) {
    // Grab the Activity so we can call finish()
    val activity = (LocalContext.current as? Activity)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement   = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = { navController.navigate(NavRoutes.GAMES_LIST) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Play")
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { navController.navigate(NavRoutes.SETTINGS) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Settings")
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { navController.navigate(NavRoutes.TEST_SENSORS) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("TestSensors")
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { activity?.finish() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Exit")
        }
    }
}
