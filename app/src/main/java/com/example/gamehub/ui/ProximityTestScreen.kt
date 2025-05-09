package com.example.gamehub.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun ProximityTestScreen(navController: NavController) {
    val context = LocalContext.current

    // Get sensor manager & proximity sensor
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    val proxSensor = remember {
        sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    }
    val maxRange = proxSensor?.maximumRange ?: 0f

    // Track whether something is close
    var isDetected by remember { mutableStateOf(false) }

    DisposableEffect(proxSensor) {
        if (proxSensor != null) {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    isDetected = event.values[0] < maxRange
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            sensorManager.registerListener(
                listener, proxSensor, SensorManager.SENSOR_DELAY_UI
            )
            onDispose { sensorManager.unregisterListener(listener) }
        } else {
            onDispose { }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = if (isDetected) Color.Red else Color.Green
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Proximity Sensor",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = if (isDetected) "OBJECT DETECTED" else "NO OBJECT",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White
            )

            Spacer(Modifier.height(48.dp))

            Button(onClick = { navController.popBackStack() }) {
                Text("Back to Menu")
            }
        }
    }
}
