package com.example.gamehub.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.gamehub.R
import com.example.gamehub.ui.components.NinePatchBorder

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ProximityTestScreen(navController: NavController) {
    val context = LocalContext.current

    // Sensor setup
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    val proxSensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) }
    val maxRange = proxSensor?.maximumRange ?: 0f

    var isDetected by remember { mutableStateOf(false) }

    DisposableEffect(proxSensor) {
        if (proxSensor != null) {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    isDetected = event.values[0] < maxRange
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            sensorManager.registerListener(listener, proxSensor, SensorManager.SENSOR_DELAY_UI)
            onDispose { sensorManager.unregisterListener(listener) }
        } else {
            onDispose { }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Retro pixel background
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
            Spacer(Modifier.height(50.dp))
            Image(
                painter = painterResource(id = R.drawable.proximity),
                contentDescription = "Proximity Test",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .height(90.dp)
            )
            Spacer(Modifier.height(90.dp))
            // Centered bordered box with proximity message
            Box(
                modifier = Modifier
                    .size(220.dp) // or adjust as needed
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                // 9-patch border (replace with your correct resource name)
                NinePatchBorder(
                    modifier = Modifier.matchParentSize(),
                    drawableRes = R.drawable.game_list_border // your 9-patch border resource
                )
                // Inside: green/red background + content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .background(
                            if (isDetected) Color(0xFFba8add) else Color(0xFF5b2f77),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp) // no extra rounding
                        ),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isDetected) "OBJECT DETECTED" else "NO OBJECT",
                        color = if (isDetected) Color(0xFF5b2f77) else Color(0xFFba8add),
                        fontSize = 24.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            SpriteMenuButton(
                text = "Back to Menu",
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
