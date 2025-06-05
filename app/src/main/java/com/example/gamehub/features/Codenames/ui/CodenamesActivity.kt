package com.example.gamehub.features.codenames.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.gamehub.ui.theme.GameHubTheme

class CodenamesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val roomId = intent.getStringExtra("roomId") ?: return
        val userName = intent.getStringExtra("userName") ?: return

        setContent {
            GameHubTheme {
                val navController = rememberNavController()
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CodenamesScreen(
                        navController = navController,
                        roomId = roomId,
                        userName = userName
                    )
                }
            }
        }
    }
} 