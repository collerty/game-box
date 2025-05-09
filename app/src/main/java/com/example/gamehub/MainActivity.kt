package com.example.gamehub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gamehub.navigation.NavRoutes
//Main menu screens
import com.example.gamehub.ui.MainMenu
import com.example.gamehub.ui.GamesListScreen
import com.example.gamehub.ui.SettingsScreen
import com.example.gamehub.ui.TestSensorsScreen
//Test sensors screens
import com.example.gamehub.ui.AccelerometerTestScreen
import com.example.gamehub.ui.GyroscopeTestScreen
import com.example.gamehub.ui.ProximityTestScreen
import com.example.gamehub.ui.VibrationTestScreen
import com.example.gamehub.ui.MicrophoneTestScreen
import com.example.gamehub.ui.CameraTestScreen

import com.example.gamehub.ui.theme.GameHubTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GameHubTheme {
                // If you don’t need a top bar, you can skip Scaffold and call NavHost() directly here.
                Scaffold { innerPadding ->
                    AppNavHost(Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = NavRoutes.MAIN_MENU,
        modifier = modifier
    ) {
        composable(NavRoutes.MAIN_MENU)  { MainMenu(navController) }
        composable(NavRoutes.GAMES_LIST) { GamesListScreen() }
        composable(NavRoutes.SETTINGS)   { SettingsScreen() }
        composable(NavRoutes.TEST_SENSORS)   { TestSensorsScreen(navController) }

        composable(NavRoutes.ACCEL_TEST)     { AccelerometerTestScreen(navController) }
        composable(NavRoutes.GYRO_TEST)      { GyroscopeTestScreen(navController) }
        composable(NavRoutes.PROXIMITY_TEST) { ProximityTestScreen(navController) }
        composable(NavRoutes.VIBRATION_TEST) { VibrationTestScreen(navController) }
        composable(NavRoutes.MIC_TEST)       { MicrophoneTestScreen(navController) }
        composable(NavRoutes.CAMERA_TEST)    { CameraTestScreen(navController) }
    }
}
