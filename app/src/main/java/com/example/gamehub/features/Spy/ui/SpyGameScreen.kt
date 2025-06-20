package com.example.gamehub.features.spy.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.gamehub.R
import com.example.gamehub.ui.SpriteMenuButton
import GameBoxFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SpyGameScreen(navController: NavController) {
    val context = LocalContext.current
    var gameState by remember { mutableStateOf("settings") } // "settings" or "game"
    var currentPlayer by remember { mutableStateOf(0) }
    var numPlayers by remember { mutableStateOf(4) }
    var numSpies by remember { mutableStateOf(1) }
    var timerMinutes by remember { mutableStateOf(5) }
    var locations by remember { mutableStateOf(listOf("Restaurant", "Beach", "Office", "School")) }
    var playerRoles by remember { mutableStateOf(listOf<String>()) }
    var timeRemaining by remember { mutableStateOf(timerMinutes * 60) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var showRoleDialog by remember { mutableStateOf(false) }
    var currentRole by remember { mutableStateOf("") }
    var currentLocation by remember { mutableStateOf("") }
    var allRolesRevealed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Function to vibrate
    fun vibrate(duration: Long) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }

    // Background image
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.spy_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Semi-transparent overlay for better text visibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (gameState == "settings") {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.weight(1f))
                // Settings UI
                Text(
                    text = "Game Settings",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = GameBoxFontFamily,
                        color = Color.White
                    )
                )
                Spacer(Modifier.height(32.dp))
                // Number of Players
                Text(
                    text = "Number of Players: $numPlayers",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = GameBoxFontFamily,
                        color = Color.White
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SpriteMenuButton(
                        text = "-",
                        onClick = { if (numPlayers > 3) numPlayers-- },
                            modifier = Modifier.size(128.dp),
                            minWidth = 0.dp,
                            contentPadding = PaddingValues(top = 8.dp)
                    )
                    SpriteMenuButton(
                        text = "+",
                        onClick = { if (numPlayers < 8) numPlayers++ },
                            modifier = Modifier.size(128.dp),
                            minWidth = 0.dp,
                            contentPadding = PaddingValues(top = 8.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                // Number of Spies
                Text(
                    text = "Number of Spies: $numSpies",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = GameBoxFontFamily,
                        color = Color.White
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SpriteMenuButton(
                        text = "-",
                        onClick = { if (numSpies > 1) numSpies-- },
                            modifier = Modifier.size(128.dp),
                            minWidth = 0.dp,
                            contentPadding = PaddingValues(top = 8.dp)
                    )
                    SpriteMenuButton(
                        text = "+",
                        onClick = { if (numSpies < numPlayers - 1) numSpies++ },
                            modifier = Modifier.size(128.dp),
                            minWidth = 0.dp,
                            contentPadding = PaddingValues(top = 8.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                // Timer
                Text(
                    text = "Timer (minutes): $timerMinutes",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = GameBoxFontFamily,
                        color = Color.White
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SpriteMenuButton(
                        text = "-",
                        onClick = { if (timerMinutes > 1) timerMinutes-- },
                            modifier = Modifier.size(128.dp),
                            minWidth = 0.dp,
                            contentPadding = PaddingValues(top = 8.dp)
                    )
                    SpriteMenuButton(
                        text = "+",
                        onClick = { if (timerMinutes < 30) timerMinutes++ },
                            modifier = Modifier.size(128.dp),
                            minWidth = 0.dp,
                            contentPadding = PaddingValues(top = 8.dp)
                    )
                }
                Spacer(Modifier.height(32.dp))
                // Start Game Button
                SpriteMenuButton(
                    text = "Start Game",
                    onClick = {
                        // Generate random roles
                        val roles = List(numPlayers) { "Civilian" }.toMutableList()
                        repeat(numSpies) {
                            var index: Int
                            do {
                                index = (0 until numPlayers).random()
                            } while (roles[index] == "Spy")
                            roles[index] = "Spy"
                        }
                        playerRoles = roles
                        timeRemaining = timerMinutes * 60
                        gameState = "game"
                        // Vibrate when game starts
                        vibrate(500)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                }
            } else {
                // Game UI
                if (!allRolesRevealed) {
                    Text(
                        text = "Player ${currentPlayer + 1}'s Turn",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = GameBoxFontFamily,
                            color = Color.White
                        )
                    )

                    Spacer(Modifier.height(32.dp))

                    // Role reveal button
                    SpriteMenuButton(
                        text = "Reveal Role",
                        onClick = {
                            currentRole = playerRoles[currentPlayer]
                            currentLocation = locations.random()
                            showRoleDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(16.dp))

                    // Next player button
                    SpriteMenuButton(
                        text = "Next Player",
                        onClick = {
                            if (currentPlayer < numPlayers - 1) {
                                currentPlayer++
                            } else {
                                allRolesRevealed = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (!isTimerRunning) {
                    // Show start timer button after all roles revealed
                    Text(
                        text = "All roles have been revealed!",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = GameBoxFontFamily,
                            color = Color.White
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(32.dp))

                    SpriteMenuButton(
                        text = "Start Timer",
                        onClick = { 
                            isTimerRunning = true
                            // Vibrate when timer starts
                            vibrate(500)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // Timer display
                    val minutes = timeRemaining / 60
                    val seconds = timeRemaining % 60
                    Text(
                        text = "Time Remaining: ${String.format("%02d:%02d", minutes, seconds)}",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = GameBoxFontFamily,
                            color = Color.White
                        )
                    )
                }

                // Start timer when isTimerRunning becomes true
                LaunchedEffect(isTimerRunning) {
                    if (isTimerRunning) {
                        while (timeRemaining > 0) {
                            delay(1000)
                            timeRemaining--
                        }
                        // Vibrate when timer runs out
                        vibrate(1000)
                        // Game over when timer reaches 0
                        // TODO: Show game over dialog
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Back button
            SpriteMenuButton(
                text = "Back",
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Role reveal dialog
        if (showRoleDialog) {
            AlertDialog(
                onDismissRequest = { showRoleDialog = false },
                title = {
                    Text(
                        text = "Your Role",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = GameBoxFontFamily,
                            color = Color.White
                        )
                    )
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Role image
                        Image(
                            painter = painterResource(
                                id = if (currentRole == "Spy") {
                                    R.drawable.spy_reveal
                                } else {
                                    R.drawable.civilian_reveal
                                }
                            ),
                            contentDescription = "Role Image",
                            modifier = Modifier
                                .size(200.dp)
                                .padding(bottom = 16.dp),
                            contentScale = ContentScale.Fit
                        )

                        Text(
                            text = if (currentRole == "Spy") {
                                "You are a Spy! Try to blend in and figure out the location."
                            } else {
                                "You are a Civilian! The location is: $currentLocation"
                            },
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = GameBoxFontFamily,
                                color = Color.White
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                },
                confirmButton = {
                    SpriteMenuButton(
                        text = "OK",
                        onClick = { showRoleDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                containerColor = Color(0xCC000000),
                titleContentColor = Color.White,
                textContentColor = Color.White
            )
        }
    }
} 