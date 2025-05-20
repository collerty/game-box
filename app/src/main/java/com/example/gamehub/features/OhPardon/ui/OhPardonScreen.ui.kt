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
import com.example.gamehub.features.ohpardon.classes.DiceRoll
import com.example.gamehub.features.ohpardon.classes.ShakeDetector
import androidx.compose.foundation.Image
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.example.gamehub.R

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
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            val context = LocalContext.current

            // Use an image loader that supports SVG
            val imageLoader = ImageLoader.Builder(context)
                .components {
                    add(SvgDecoder.Factory())
                }
                .build()


            val painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(context)
                    .data("android.resource://${context.packageName}/${R.raw.ohpardon_board}")
                    .crossfade(true)
                    .build(),
                imageLoader = imageLoader
            )

            Image(
                painter = painter,
                contentDescription = "Game Board",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f) // Square
            )
        }
    }
}
