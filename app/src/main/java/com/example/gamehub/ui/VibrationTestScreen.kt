package com.example.gamehub.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gamehub.R
import com.example.gamehub.ui.components.NinePatchBorder

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun VibrationTestScreen(navController: NavController) {
    val context = LocalContext.current

    var durationMs by remember { mutableStateOf(1000f) }
    var intensity by remember { mutableStateOf(1f) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background
        Image(
            painter = painterResource(id = R.drawable.game_box_bg1),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))

            // Pixel-art title image
            Image(
                painter = painterResource(id = R.drawable.vibration),
                contentDescription = "Vibration Test",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .height(90.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Centered bordered box with controls
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight()
            ) {
                NinePatchBorder(
                    modifier = Modifier.matchParentSize(),
                    drawableRes = R.drawable.game_list_border // your 9-patch border
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Duration slider
                    Text(
                        "Duration: ${durationMs.toInt()} ms",
                        color = Color(0xFFc08cdc)
                    )
                    PixelSlider(
                        value = durationMs,
                        onValueChange = { durationMs = it },
                        valueRange = 0f..5000f,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(20.dp))

                    // Intensity slider
                    Text(
                        "Intensity: ${(intensity * 100).toInt()} %",
                        color = Color(0xFFc08cdc)
                    )
                    PixelSlider(
                        value = intensity,
                        onValueChange = { intensity = it },
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(32.dp))

                    SpriteMenuButton(
                        text = "Vibrate",
                        onClick = {
                            val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                val vm = context.getSystemService(VibratorManager::class.java)!!
                                vm.defaultVibrator
                            } else {
                                @Suppress("DEPRECATION")
                                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                            }
                            val ampValue = (intensity * 255).toInt().coerceIn(1, 255)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
                        modifier = Modifier.fillMaxWidth(0.88f),
                        normalRes = R.drawable.menu_button_long,
                        pressedRes = R.drawable.menu_button_long_pressed
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            SpriteMenuButton(
                text = "Back",
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .fillMaxWidth(0.95f),
                normalRes = R.drawable.menu_button_long,
                pressedRes = R.drawable.menu_button_long_pressed
            )

            Spacer(Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PixelSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier
) {
    // Compose doesn't have "fully pixel-art" sliders out of the box,
    // but you can make them less rounded and squarer.
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        modifier = modifier.height(28.dp),
        colors = SliderDefaults.colors(
            thumbColor = Color(0xFFc08cdc),
            activeTrackColor = Color(0xFF522C7B),
            inactiveTrackColor = Color(0xFF231d2b)
        ),
        // No round shape:
        thumb = {
            Box(
                Modifier
                    .size(18.dp, 18.dp)
                    .background(Color(0xFFc08cdc))
            )
        },
        track = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(Color(0xFF522C7B))
            )
        }
    )
}
