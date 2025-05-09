package com.example.gamehub.ui

import com.example.gamehub.navigation.NavRoutes
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun TestSensorsScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement   = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = { navController.navigate(NavRoutes.ACCEL_TEST) }, Modifier.fillMaxWidth()) {
            Text("Test Accelerometer")
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { navController.navigate(NavRoutes.GYRO_TEST) }, Modifier.fillMaxWidth()) {
            Text("Test Gyroscope")
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { navController.navigate(NavRoutes.PROXIMITY_TEST) }, Modifier.fillMaxWidth()) {
            Text("Test Proximity")
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { navController.navigate(NavRoutes.VIBRATION_TEST) }, Modifier.fillMaxWidth()) {
            Text("Test Vibration")
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { navController.navigate(NavRoutes.MIC_TEST) }, Modifier.fillMaxWidth()) {
            Text("Test Microphone")
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { navController.navigate(NavRoutes.CAMERA_TEST) }, Modifier.fillMaxWidth()) {
            Text("Test Camera")
        }
        Spacer(Modifier.height(32.dp))
        Button(onClick = { navController.popBackStack() }, Modifier.fillMaxWidth()) {
            Text("Back to Menu")
        }
    }
}
