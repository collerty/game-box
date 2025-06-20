package com.example.gamehub.ui

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.navigation.NavController
import com.example.gamehub.R
import com.example.gamehub.navigation.NavRoutes
import com.example.gamehub.ui.SpriteMenuButton

@Composable
fun TestSensorsScreen(navController: NavController) {
    val activity = (LocalContext.current as? Activity)

    Box(modifier = Modifier.fillMaxSize()) {
        // Background image, matching MainMenu
        Image(
            painter = painterResource(id = R.drawable.game_box_bg1),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Image(
                painter = painterResource(id = R.drawable.sensor_tests),
                contentDescription = "Sensor Tests",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .height(100.dp) // Adjust height as needed for your image
            )

            Spacer(Modifier.height(20.dp))

            // Custom sprite buttons for each test
            SpriteMenuButton(
                text = "Test Accelerometer",
                onClick = { navController.navigate(NavRoutes.ACCEL_TEST) },
                modifier = Modifier.fillMaxWidth(0.95f),
                normalRes = R.drawable.menu_button_extra_long,
                pressedRes = R.drawable.menu_button_extra_long_pressed

            )
            Spacer(Modifier.height(16.dp))
            SpriteMenuButton(
                text = "Test Gyroscope",
                onClick = { navController.navigate(NavRoutes.GYRO_TEST) },
                modifier = Modifier.fillMaxWidth(0.95f),
                normalRes = R.drawable.menu_button_extra_long,
                pressedRes = R.drawable.menu_button_extra_long_pressed
            )
            Spacer(Modifier.height(16.dp))
            SpriteMenuButton(
                text = "Test Proximity",
                onClick = { navController.navigate(NavRoutes.PROXIMITY_TEST) },
                modifier = Modifier.fillMaxWidth(0.95f),
                normalRes = R.drawable.menu_button_extra_long,
                pressedRes = R.drawable.menu_button_extra_long_pressed
            )
            Spacer(Modifier.height(16.dp))
            SpriteMenuButton(
                text = "Test Vibration",
                onClick = { navController.navigate(NavRoutes.VIBRATION_TEST) },
                modifier = Modifier.fillMaxWidth(0.95f),
                normalRes = R.drawable.menu_button_extra_long,
                pressedRes = R.drawable.menu_button_extra_long_pressed
            )
            Spacer(Modifier.height(16.dp))
            SpriteMenuButton(
                text = "Test Microphone",
                onClick = { navController.navigate(NavRoutes.MIC_TEST) },
                modifier = Modifier.fillMaxWidth(0.95f),
                normalRes = R.drawable.menu_button_extra_long,
                pressedRes = R.drawable.menu_button_extra_long_pressed
            )
            Spacer(Modifier.height(16.dp))
            SpriteMenuButton(
                text = "Test Camera",
                onClick = { navController.navigate(NavRoutes.CAMERA_TEST) },
                modifier = Modifier.fillMaxWidth(0.95f),
                normalRes = R.drawable.menu_button_extra_long,
                pressedRes = R.drawable.menu_button_extra_long_pressed
            )
            Spacer(Modifier.height(32.dp))
            SpriteMenuButton(
                text = "Back to Menu",
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth(0.95f),
                normalRes = R.drawable.menu_button_extra_long,
                pressedRes = R.drawable.menu_button_extra_long_pressed
            )
        }
    }
}
