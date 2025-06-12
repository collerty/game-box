package com.example.gamehub.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.gamehub.R
import com.example.gamehub.ui.components.NinePatchBorder

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun GyroscopeTestScreen(navController: NavController) {
    val context = LocalContext.current
    var x by remember { mutableStateOf(0f) }
    var y by remember { mutableStateOf(0f) }
    var z by remember { mutableStateOf(0f) }

    // Sensor listener setup
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                x = event.values[0]
                y = event.values[1]
                z = event.values[2]
            }
            override fun onAccuracyChanged(s: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, gyro, SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager.unregisterListener(listener) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Pixel-art background
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

            // Title image
            Image(
                painter = painterResource(id = R.drawable.gyroscope),
                contentDescription = "Gyroscope Test",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .height(90.dp)
            )

            // Spacer before data box to help center
            Spacer(modifier = Modifier.weight(1f))

            // Centered bordered box
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight()
            ) {
                NinePatchBorder(
                    modifier = Modifier.matchParentSize(),
                    drawableRes = R.drawable.border // 9-patch border resource
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Gyro X: ${"%.2f".format(x)}",
                        color = androidx.compose.ui.graphics.Color(0xFFc08cdc),
                        fontSize = 24.sp
                    )
                    Text(
                        "Gyro Y: ${"%.2f".format(y)}",
                        color = androidx.compose.ui.graphics.Color(0xFFc08cdc),
                        fontSize = 24.sp
                    )
                    Text(
                        "Gyro Z: ${"%.2f".format(z)}",
                        color = androidx.compose.ui.graphics.Color(0xFFc08cdc),
                        fontSize = 24.sp
                    )
                }
            }

            // Spacer after data box to help center
            Spacer(modifier = Modifier.weight(1f))

            // Back button at the bottom
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
