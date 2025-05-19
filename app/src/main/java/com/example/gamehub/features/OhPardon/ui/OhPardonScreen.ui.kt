package com.example.gamehub.features.ohpardon.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.example.gamehub.features.OhPardon.DiceRoll
import com.example.gamehub.features.OhPardon.ShakeDetector


@Composable
fun OhPardonScreen(
    navController: NavController,
    code: String,
    userName: String
) {
    val context = LocalContext.current
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    val accelerometer = remember {
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    val diceRoll = remember { DiceRoll() }
    var diceResult by remember { mutableStateOf<Int?>(null) }

    // Define the shake detector with the dice logic
    val shakeDetector = remember {
        ShakeDetector {
            diceResult = diceRoll.roll()
        }
    }

    // Handle sensor lifecycle registration
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    sensorManager.registerListener(shakeDetector, accelerometer, SensorManager.SENSOR_DELAY_UI)
                }
                Lifecycle.Event.ON_PAUSE -> {
                    sensorManager.unregisterListener(shakeDetector)
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            sensorManager.unregisterListener(shakeDetector)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // UI
    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "🙊 Oh Pardon started!",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(8.dp))
            Text("Room code: $code", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            Text("You are: $userName", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(16.dp))

            if (diceResult != null) {
                Text("🎲 You rolled: $diceResult", style = MaterialTheme.typography.titleLarge)
            } else {
                Text("Shake your device to roll the dice!", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
