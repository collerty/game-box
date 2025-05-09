// app/src/main/java/com/example/gamehub/ui/VibrationTestScreen.kt

package com.example.gamehub.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun VibrationTestScreen(navController: NavController) {
    val context = LocalContext.current

    // State for duration (0–5000ms) and intensity (0f–1f)
    var durationMs by remember { mutableStateOf(1000f) }
    var intensity by remember { mutableStateOf(1f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement   = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Duration slider
        Text("Duration: ${durationMs.toInt()} ms")
        Slider(
            value = durationMs,
            onValueChange = { durationMs = it },
            valueRange = 0f..5000f,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        // Intensity slider
        Text("Intensity: ${(intensity * 100).toInt()} %")
        Slider(
            value = intensity,
            onValueChange = { intensity = it },
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                // pick the right Vibrator instance
                val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = context.getSystemService(VibratorManager::class.java)!!
                    vm.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }

                // Compute actual amplitude 1–255
                val ampValue = (intensity * 255).toInt().coerceIn(1, 255)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // waveform: [delay, on, off, on] with dynamic amp
                    val on = durationMs.toLong()
                    val off = 200L
                    val pattern = longArrayOf(0, on, off, on)
                    val amps    = intArrayOf(0, ampValue, 0, ampValue)
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, amps, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(durationMs.toLong())
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Vibrate")
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}
