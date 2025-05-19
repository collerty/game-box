package com.example.gamehub.features.jorisjump.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
// import androidx.compose.ui.platform.LocalDensity // Not strictly needed for this version
import androidx.compose.ui.tooling.preview.Preview // For Composable Previews
import androidx.compose.ui.unit.dp

// Constants for the game
private const val PLAYER_WIDTH_DP = 40
private const val PLAYER_HEIGHT_DP = 60
// IMPORTANT: You WILL need to test and adjust this sensitivity and the +/- sign in onSensorChanged
private const val ACCELEROMETER_SENSITIVITY = 4.0f // Start with this, adjust as needed

@Composable
fun JorisJumpScreen() {
    val context = LocalContext.current
    // val density = LocalDensity.current // Can be useful for px conversions, not strictly needed here

    // Player's absolute horizontal position from the left edge of the screen, in Dp
    var playerXPositionDp by remember { mutableStateOf(0f) }
    var playerInitialized by remember { mutableStateOf(false) } // To set initial position once

    // --- SENSOR SETUP ---
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    val accelerometer = remember {
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    // This will hold the screen width in Dp once BoxWithConstraints provides it
    var screenWidthDp by remember { mutableStateOf(0f) }
    var rawTiltXForDebug by remember { mutableStateOf(0f) } // For debugging tilt values

    DisposableEffect(accelerometer, screenWidthDp, playerInitialized) {
        // If accelerometer is not available, set up a no-op listener or simply log
        // but ensure onDispose is still returned.
        if (accelerometer == null) {
            // Optional: Log or show a more prominent message if accelerometer is critical
            // For now, the Text composable below handles visual feedback
            onDispose { } // Return the onDispose block even if no listener is registered
            // No return@DisposableEffect here, let it fall through to the onDispose at the end
            // OR, if you want to strictly do nothing more:
            // return@DisposableEffect onDispose { } // This is the fix!
        }

        // If accelerometer is null, we actually don't want to proceed to register a listener
        // So the return@DisposableEffect onDispose { } is the correct pattern
        if (accelerometer == null) {
            return@DisposableEffect onDispose {
                // Nothing to unregister if accelerometer was null
            }
        }


        val sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                // Only process sensor data if player & screen width are initialized
                if (!playerInitialized || screenWidthDp == 0f) return

                event?.let {
                    if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        val tiltX = it.values[0] // Raw accelerometer X value
                        rawTiltXForDebug = tiltX // Update debug value

                        // --- PLAYER MOVEMENT LOGIC ---
                        // This is the CRITICAL part to test and adjust based on your device.
                        // Goal: Tilt phone left -> player moves left on screen.
                        //       Tilt phone right -> player moves right on screen.

                        // Scenario 1: If tilting left gives POSITIVE tiltX values (common)
                        // To move left (decrease X) when tiltX is positive:
                        playerXPositionDp -= tiltX * ACCELEROMETER_SENSITIVITY

                        // Scenario 2: If tilting left gives NEGATIVE tiltX values
                        // To move left (decrease X) when tiltX is negative:
                        // playerXPositionDp += tiltX * ACCELEROMETER_SENSITIVITY // (e.g. X += (-5) * 2 = X - 10)

                        // **CHOOSE ONE of the above lines based on your testing.**
                        // **Comment out the one that doesn't work for your device.**


                        // Screen wrapping logic
                        // Player's right edge is (playerXPositionDp + PLAYER_WIDTH_DP)
                        // Player's left edge is playerXPositionDp

                        // If player moves completely off the left edge
                        if ((playerXPositionDp + PLAYER_WIDTH_DP) < 0) {
                            playerXPositionDp = screenWidthDp // Place player's left edge at the screen's right edge
                        }
                        // If player moves completely off the right edge
                        else if (playerXPositionDp > screenWidthDp) {
                            playerXPositionDp = 0f - PLAYER_WIDTH_DP // Place player's right edge at the screen's left edge
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Not typically used for simple accelerometer games
            }
        }

        sensorManager.registerListener(
            sensorEventListener,
            accelerometer, // This is safe now due to the null check above
            SensorManager.SENSOR_DELAY_GAME // Good update rate for games
        )

        onDispose {
            sensorManager.unregisterListener(sensorEventListener)
        }
    }
    // --- END SENSOR SETUP ---

    BoxWithConstraints( // This Composable gives us the actual screen dimensions (maxWidth, maxHeight in Dp)
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF87CEEB)) // A nice sky blue color
    ) {
        // Initialize player position and screenWidthDp once after BoxWithConstraints provides dimensions
        if (!playerInitialized && this.maxWidth > 0.dp) { // Check if maxWidth is available
            screenWidthDp = this.maxWidth.value // Convert Dp to Float for calculations
            playerXPositionDp = (screenWidthDp / 2) - (PLAYER_WIDTH_DP / 2) // Center the player initially
            playerInitialized = true
        }

        // Only draw the player if it has been initialized
        if (playerInitialized) {
            Box(
                modifier = Modifier
                    // Use .absoluteOffset for positioning from top-left in Dp
                    // Y position is fixed near the bottom for now
                    .absoluteOffset(x = playerXPositionDp.dp, y = this.maxHeight - PLAYER_HEIGHT_DP.dp - 20.dp)
                    .size(PLAYER_WIDTH_DP.dp, PLAYER_HEIGHT_DP.dp)
                    .background(Color.Green)
            ) {
                Text("J", color = Color.Black, modifier = Modifier.align(Alignment.Center))
            }
        }

        // Debugging Text - very helpful during development
        Column(modifier = Modifier.align(Alignment.TopStart).padding(8.dp).background(Color.Black.copy(alpha = 0.3f))) {
            Text(
                text = "PlayerX (Dp): ${"%.1f".format(playerXPositionDp)}",
                color = Color.White
            )
            Text(
                text = "ScreenWidth (Dp): ${"%.1f".format(screenWidthDp)}",
                color = Color.White
            )
            Text(
                text = "Raw Tilt X: ${"%.2f".format(rawTiltXForDebug)}",
                color = Color.White
            )
            if (accelerometer == null) {
                Text(
                    text = "ACCELEROMETER NOT AVAILABLE!",
                    color = Color.Red,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

// It's good practice to add a @Preview Composable for easy UI checking in Android Studio
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun JorisJumpScreenPreview() {
    // Since the actual screen uses sensors and BoxWithConstraints, a full preview is tricky.
    // This preview will just show the basic layout without sensor interaction.
    // You can mock sensor data for more complex previews if needed.
    MaterialTheme { // Ensure a theme is applied for Material components
        // JorisJumpScreen() // Calling the actual screen might not work well in preview due to context/sensor deps
        // For a simple static preview:
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF87CEEB))) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-20).dp) // Approximate position
                    .size(PLAYER_WIDTH_DP.dp, PLAYER_HEIGHT_DP.dp)
                    .background(Color.Green)
            ) {
                Text("J", color = Color.Black, modifier = Modifier.align(Alignment.Center))
            }
            Text(
                text = "Preview Mode (No Sensors)",
                modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
                color = Color.Black
            )
        }
    }
}