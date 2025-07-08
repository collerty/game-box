package com.example.gamehub.features.codenames.ui

import android.util.Log
import android.widget.Toast
import android.view.WindowManager
import android.view.View
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import com.example.gamehub.R
import com.example.gamehub.features.codenames.model.CardColor
import com.example.gamehub.navigation.NavRoutes
import com.example.gamehub.ui.SpriteMenuButton
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.toObject
import com.example.gamehub.ui.theme.ArcadeClassic
import com.google.firebase.auth.FirebaseAuth
import com.example.gamehub.audio.SoundManager
import com.example.gamehub.repository.interfaces.ICodenamesRepository
import com.example.gamehub.features.codenames.model.CodenamesGameState
import com.example.gamehub.features.codenames.model.Clue

@Composable
fun CodenamesScreen(
    navController: NavController,
    roomId: String,
    isMaster: Boolean,
    masterTeam: String? = null,
    repository: ICodenamesRepository
) {
    val view = LocalView.current
    val window = (view.context as? android.app.Activity)?.window
    val context = LocalContext.current

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

    // Hide system bars
    LaunchedEffect(Unit) {
        window?.let {
            WindowCompat.setDecorFitsSystemWindows(it, false)
            WindowInsetsControllerCompat(it, view).let { controller ->
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    Log.d("CodenamesDebug", "CodenamesScreen received masterTeam: $masterTeam")

    var gameState by remember { mutableStateOf<CodenamesGameState?>(null) }
    var currentTurn by remember { mutableStateOf("RED") }
    var redWordsRemaining by remember { mutableStateOf(9) }
    var blueWordsRemaining by remember { mutableStateOf(8) }
    var winner by remember { mutableStateOf<String?>(null) }

    // Add new state variables
    var redMasterClue by remember { mutableStateOf("") }
    var blueMasterClue by remember { mutableStateOf("") }
    var timerSeconds by remember { mutableStateOf(0) }
    var isMasterPhase by remember { mutableStateOf(true) }
    var currentTeam by remember { mutableStateOf("RED") }
    var redClues by remember { mutableStateOf<List<Clue>>(emptyList()) }
    var blueClues by remember { mutableStateOf<List<Clue>>(emptyList()) }
    var currentMasterTeam by remember { mutableStateOf(masterTeam?.uppercase()) }

    // Debug logging for visibility conditions
    LaunchedEffect(isMaster, currentMasterTeam, isMasterPhase, currentTeam) {
        Log.d("CodenamesDebug", """
            Visibility Debug:
            isMaster: $isMaster
            masterTeam: $currentMasterTeam
            isMasterPhase: $isMasterPhase
            currentTeam: $currentTeam
            currentTurn: $currentTurn
            Red team conditions: ${isMaster && currentMasterTeam == "RED" && isMasterPhase && currentTeam == "RED"}
            Blue team conditions: ${isMaster && currentMasterTeam == "BLUE" && isMasterPhase && currentTeam == "BLUE"}
            Raw values:
            masterTeam from intent: $masterTeam
            currentMasterTeam: $currentMasterTeam
            currentTeam: $currentTeam
        """.trimIndent())
    }

    // Timer effect
    LaunchedEffect(isMasterPhase, currentTeam) {
        timerSeconds = 60 // Both phases get 60 seconds
        // Only run timer if there's no winner
        if (winner == null) {
            while (timerSeconds > 0) {
                // Play ticking sound for last 5 seconds
                if (timerSeconds <= 5) {
                    SoundManager.playEffect(context, R.raw.ticking_sound)
                }
                delay(1000)
                timerSeconds--
            }
            // When timer ends, switch phases and teams if needed
            if (winner == null) { // Double check winner in case it changed during delay
                if (!isMasterPhase) {
                    // If team phase ends, switch to next team's master phase
                    val nextTeam = if (currentTeam == "RED") "BLUE" else "RED"
                    repository.updateGameState(
                        roomId,
                        CodenamesGameState(
                            currentTurn = nextTeam,
                            currentTeam = nextTeam,
                            isMasterPhase = true,
                            redWordsRemaining = redWordsRemaining,
                            blueWordsRemaining = blueWordsRemaining,
                            winner = winner,
                            clues = redClues + blueClues,
                            cards = gameState?.cards ?: emptyList(),
                            currentGuardingWordCount = 0,
                            guessesRemaining = 0
                        ),
                        onSuccess = {},
                        onError = { e -> Log.e("Codenames", "Error updating Firestore on timer end (team phase): $e") }
                    )
                } else {
                    // If master phase ends, switch to team phase
                    repository.updateGameState(
                        roomId,
                        CodenamesGameState(
                            currentTurn = currentTurn,
                            currentTeam = currentTeam,
                            isMasterPhase = false,
                            redWordsRemaining = redWordsRemaining,
                            blueWordsRemaining = blueWordsRemaining,
                            winner = winner,
                            clues = redClues + blueClues,
                            cards = gameState?.cards ?: emptyList(),
                            currentGuardingWordCount = 0,
                            guessesRemaining = 0
                        ),
                        onSuccess = {},
                        onError = { e -> Log.e("Codenames", "Error updating Firestore on timer end (master phase): $e") }
                    )
                }
            }
        }
    }

    // Listen for game state updates
    LaunchedEffect(roomId) {
        repository.listenToGameState(roomId, { state ->
            gameState = state
            currentTurn = state?.currentTurn ?: "RED"
            redWordsRemaining = state?.redWordsRemaining ?: 9
            blueWordsRemaining = state?.blueWordsRemaining ?: 8
            currentTeam = state?.currentTeam ?: "RED"
            isMasterPhase = state?.isMasterPhase ?: true
            winner = state?.winner

                    // Debug logging for state updates
                    Log.d("CodenamesDebug", """
                        State Update:
                        currentTurn: $currentTurn
                        currentTeam: $currentTeam
                        isMasterPhase: $isMasterPhase
                        masterTeam: $currentMasterTeam
                        winner: $winner
                        Raw values:
                        currentTeam from state: ${state?.currentTeam}
                        currentTeam after uppercase: $currentTeam
                    """.trimIndent())

                    // Update clues
                    val clues = state?.clues ?: emptyList()
                    redClues = clues.filter { it.team == "RED" }
                        .map { Clue(it.word, "RED") }
                    blueClues = clues.filter { it.team == "BLUE" }
                        .map { Clue(it.word, "BLUE") }
        }, { e -> Log.e("Codenames", "Error listening to game state", e) })
    }

    // Game Over Screen
    if (winner != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = when (winner) {
                        "RED" -> "Red Team Wins!"
                        "BLUE" -> "Blue Team Wins!"
                        else -> "Game Over"
                    },
                    style = MaterialTheme.typography.headlineLarge,
                    color = when (winner) {
                        "RED" -> Color.Red
                        "BLUE" -> Color.Blue
                        else -> Color.White
                    },
                    fontFamily = ArcadeClassic
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = when (winner) {
                        "RED" -> "Red team found all their words!"
                        "BLUE" -> "Blue team found all their words!"
                        else -> "Game ended"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontFamily = ArcadeClassic
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        navController.navigate(NavRoutes.LOBBY_MENU.replace("{gameId}", "codenames")) {
                            // Pop up to the Codenames lobby menu route, removing the game screen
                            popUpTo(NavRoutes.LOBBY_MENU.replace("{gameId}", "codenames")) {
                                inclusive = true // Remove the Codenames game screen from the back stack
                            }
                            // launchSingleTop = true // May not be necessary with popUpTo inclusive
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (winner) {
                            "RED" -> Color.Red
                            "BLUE" -> Color.Blue
                            else -> Color.Gray
                        }
                    )
                ) {
                    Text("Return to Lobby", color = Color.White, fontFamily = ArcadeClassic)
                }
            }
        }
        return
    }

    // Main game layout with background
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background image
        androidx.compose.foundation.Image(
            painter = painterResource(id = R.drawable.stars_bg),
            contentDescription = "Stars Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Top section (scores and grid)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Allows this row to take up available space, pushing controls to bottom
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Panel (Red Team/Log)
                Column(
                    modifier = Modifier
                        .weight(0.7f)
                        .fillMaxHeight()
                        .padding(end = 4.dp)
                        .background(
                            color = Color.Red.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(1.dp, Color.Red.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Red Team Info and Score
                    Text(
                        text = redWordsRemaining.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        color = Color.White,
                        fontFamily = ArcadeClassic
                    )
                    Text(
                        "Red Team",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        fontFamily = ArcadeClassic
                    )
                    // Log/Clue History Section
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                         Text(
                            "Log",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontFamily = ArcadeClassic
                        )
                         if (currentTurn == "RED") {
                             Text(
                                 text = "${timerSeconds}s",
                                 style = MaterialTheme.typography.titleMedium,
                                 color = Color.White,
                                 fontFamily = ArcadeClassic
                             )
                         }
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(redClues) { clue ->
                            Text(
                                text = clue.word,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                fontFamily = ArcadeClassic
                            )
                        }
                    }
                }

                // Center Grid (Cards)
                Column(
                    modifier = Modifier
                        .weight(4f) // Increased horizontal weight for the grid
                        .fillMaxHeight()
                        .padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    @Suppress("UNCHECKED_CAST")
                    val cards = (gameState?.cards) ?: emptyList()

                    // Use BoxWithConstraints to determine available size for the grid
                    BoxWithConstraints(modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight() // Box fills the height of its parent Column
                    ) {
                        val gridWidth = maxWidth
                        val gridHeight = maxHeight
                        val spacing = 4.dp // Increased spacing for better visual separation

                        // Calculate card width and height to fit 5x5 grid evenly
                        // Subtract total spacing from width/height and divide by 5
                        val totalHorizontalSpacing = spacing * 4 // 4 gaps between 5 cards
                        val totalVerticalSpacing = spacing * 4   // 4 gaps between 5 rows

                        val cardWidth = (gridWidth - totalHorizontalSpacing) / 5
                        val cardHeight = (gridHeight - totalVerticalSpacing) / 5

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(5),
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(spacing),
                            verticalArrangement = Arrangement.spacedBy(spacing)
                        ) {
                            items(cards) { card ->
                                val word = card.word
                                val color = card.color
                                val isRevealed = card.isRevealed
                                val cardIndex = cards.indexOf(card)

                                Card(
                                    modifier = Modifier
                                        .width(cardWidth)
                                        .height(cardHeight)
                                        .clickable(
                                            enabled = !isRevealed && !isMasterPhase && currentTeam == currentTurn && !isMaster && winner == null
                                        ) {
                                            // Update card's revealed state in Firestore
                                            repository.updateGameState(
                                                roomId,
                                                CodenamesGameState(
                                                    currentTurn = currentTurn,
                                                    currentTeam = currentTeam,
                                                    isMasterPhase = isMasterPhase,
                                                    redWordsRemaining = redWordsRemaining,
                                                    blueWordsRemaining = blueWordsRemaining,
                                                    winner = winner,
                                                    clues = redClues + blueClues,
                                                    cards = cards.toMutableList().apply {
                                                        this[cardIndex] = this[cardIndex].copy(isRevealed = true)
                                                    },
                                                    currentGuardingWordCount = 0,
                                                    guessesRemaining = 0
                                                ),
                                                onSuccess = {},
                                                onError = { e -> Log.e("Codenames", "Error updating Firestore after card click: $e") }
                                            )
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = when {
                                            isMaster -> {
                                                when (color) {
                                                    CardColor.RED -> Color.Red
                                                    CardColor.BLUE -> Color.Blue
                                                    CardColor.NEUTRAL -> Color.Gray
                                                    CardColor.ASSASSIN -> Color.Black
                                                    else -> Color.White
                                                }
                                            }
                                            isRevealed -> {
                                                when (color) {
                                                    CardColor.RED -> Color.Red
                                                    CardColor.BLUE -> Color.Blue
                                                    CardColor.NEUTRAL -> Color.Gray
                                                    CardColor.ASSASSIN -> Color.Black
                                                    else -> Color.White
                                                }
                                            }
                                            else -> Color.White
                                        }
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = word,
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = TextAlign.Center,
                                            color = if (isMaster || isRevealed) Color.White else Color.Black,
                                            modifier = Modifier.padding(2.dp),
                                            fontFamily = ArcadeClassic
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Right Panel (Blue Team/Clue History)
                Column(
                    modifier = Modifier
                        .weight(0.7f)
                        .fillMaxHeight()
                        .padding(start = 4.dp)
                        .background(
                            color = Color.Blue.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(1.dp, Color.Blue.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Blue Team Info and Score
                    Text(
                        text = blueWordsRemaining.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        color = Color.White,
                        fontFamily = ArcadeClassic
                    )
                    Text(
                        "Blue Team",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        fontFamily = ArcadeClassic
                    )
                    // Log/Clue History Section for Blue Team
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                         Text(
                            "Log",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontFamily = ArcadeClassic
                        )
                         if (currentTurn == "BLUE") {
                             Text(
                                 text = "${timerSeconds}s",
                                 style = MaterialTheme.typography.titleMedium,
                                 color = Color.White,
                                 fontFamily = ArcadeClassic
                             )
                         }
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(blueClues) { clue ->
                            Text(
                                text = clue.word,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                fontFamily = ArcadeClassic
                            )
                        }
                    }
                }
            }

            // Bottom section for global controls/status (Master Input, General Status)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Conditional display for Master Input or General Player Status
                if (isMaster && isMasterPhase && currentTeam == currentMasterTeam) {
                    // Master input field for the current master's team
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = if (currentTeam == "RED") redMasterClue else blueMasterClue,
                            onValueChange = {
                                if (currentTeam == "RED") redMasterClue = it else blueMasterClue = it
                            },
                            label = { Text("Enter clue and number (e.g., 'APPLE 3')", fontFamily = ArcadeClassic, color = Color.White) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                                focusedLabelColor = Color.White,
                                unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                                cursorColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        SpriteMenuButton(
                            text = "Submit Clue",
                            onClick = {
                                val clueText = if (currentTeam == "RED") redMasterClue else blueMasterClue
                                // Basic validation for clue format (e.g., "WORD X")
                                val parts = clueText.split(" ")
                                if (parts.size == 2 && parts[1].toIntOrNull() != null) {
                                    val word = parts[0]
                                    val count = parts[1].toInt()
                                    // Add more robust validation for the count (e.g., 1-5, or max words remaining)
                                    if (count < 0 || count > 9) { // Example constraint for count
                                        Toast.makeText(context, "Clue number must be between 0 and 9", Toast.LENGTH_SHORT).show()
                                        Log.e("Codenames", "Invalid clue number: $count")
                                        return@SpriteMenuButton // Exit onClick if invalid
                                    }

                                    if (word.isNotEmpty()) {
                                        // Get current clues
                                        repository.getGameState(roomId, onSuccess = { state ->
                                            val currentClues: List<Clue> = state?.clues ?: emptyList()
                                            val newClue = Clue(clueText, currentTeam)
                                            repository.updateGameState(
                                                roomId,
                                                CodenamesGameState(
                                                    currentTurn = currentTurn,
                                                    currentTeam = currentTeam,
                                                    isMasterPhase = false,
                                                    redWordsRemaining = redWordsRemaining,
                                                    blueWordsRemaining = blueWordsRemaining,
                                                    winner = winner,
                                                    clues = currentClues + newClue,
                                                    cards = gameState?.cards ?: emptyList(),
                                                    currentGuardingWordCount = count, // Add guarding word count to state
                                                    guessesRemaining = count + 1 // Players get 1 more guess than the number provided
                                                ),
                                                onSuccess = {
                                                    if (currentTeam == "RED") redMasterClue = "" else blueMasterClue = "" // Clear input field
                                                    Toast.makeText(context, "Clue submitted!", Toast.LENGTH_SHORT).show()
                                                },
                                                onError = { e ->
                                                    Log.e("Codenames", "Error updating Firestore after clue submission: $e")
                                                    Toast.makeText(context, "Failed to submit clue: ${e.message}", Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        }, onError = { e ->
                                            Log.e("Codenames", "Error getting game state to submit clue: $e")
                                            Toast.makeText(context, "Failed to get game state: ${e.message}", Toast.LENGTH_LONG).show()
                                        })
                                    } else {
                                        Toast.makeText(context, "Clue word cannot be empty", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Invalid clue format. Use 'WORD NUMBER'", Toast.LENGTH_SHORT).show()
                                    Log.e("Codenames", "Invalid clue format. Please use 'WORD NUMBER'. Input: $clueText")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    // Display Current Turn/Phase for non-masters or for the other master's turn
                    val phaseText = if (isMasterPhase) "Master Phase" else "Guessing Phase"
                    Text(
                        text = "Current Turn: $currentTurn ($phaseText)",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (currentTurn == "RED") Color.Red else Color.Blue,
                        fontFamily = ArcadeClassic
                    )
                }
            }
        }
    }
}