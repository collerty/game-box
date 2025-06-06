package com.example.gamehub.features.codenames.ui

import android.util.Log
import android.widget.Toast // Import Toast
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
import androidx.compose.ui.platform.LocalContext // Import LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gamehub.features.codenames.model.CardColor
import com.example.gamehub.navigation.NavRoutes
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.toObject

data class Clue(
    val word: String,
    val team: String
)

@Composable
fun CodenamesScreen(
    navController: NavController,
    roomId: String,
    isMaster: Boolean,
    masterTeam: String? = null
) {
    Log.d("CodenamesDebug", "CodenamesScreen received masterTeam: $masterTeam")

    val db = Firebase.firestore
    var gameState by remember { mutableStateOf<Map<String, Any>?>(null) }
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

    val context = LocalContext.current // Get the current context for Toast

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
                delay(1000)
                timerSeconds--
            }
            // When timer ends, switch phases and teams if needed
            if (winner == null) { // Double check winner in case it changed during delay
                if (!isMasterPhase) {
                    // If team phase ends, switch to next team's master phase
                    val nextTeam = if (currentTeam == "RED") "BLUE" else "RED"
                    db.collection("rooms").document(roomId)
                        .update(
                            mapOf(
                                "gameState.codenames.isMasterPhase" to true,
                                "gameState.codenames.currentTeam" to nextTeam,
                                "gameState.codenames.currentTurn" to nextTeam
                            )
                        )
                        .addOnFailureListener { e -> Log.e("Codenames", "Error updating Firestore on timer end (team phase): $e") }
                } else {
                    // If master phase ends, switch to team phase
                    db.collection("rooms").document(roomId)
                        .update("gameState.codenames.isMasterPhase", false)
                        .addOnFailureListener { e -> Log.e("Codenames", "Error updating Firestore on timer end (master phase): $e") }
                }
            }
        }
    }

    // Listen for game state updates
    LaunchedEffect(roomId) {
        db.collection("rooms").document(roomId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    val state = snapshot.get("gameState.codenames") as? Map<String, Any>
                    gameState = state
                    currentTurn = state?.get("currentTurn") as? String ?: "RED"
                    redWordsRemaining = (state?.get("redWordsRemaining") as? Number)?.toInt() ?: 9
                    blueWordsRemaining = (state?.get("blueWordsRemaining") as? Number)?.toInt() ?: 8
                    currentTeam = (state?.get("currentTeam") as? String)?.uppercase() ?: "RED"
                    isMasterPhase = state?.get("isMasterPhase") as? Boolean ?: true
                    winner = state?.get("winner") as? String

                    // Debug logging for state updates
                    Log.d("CodenamesDebug", """
                        State Update:
                        currentTurn: $currentTurn
                        currentTeam: $currentTeam
                        isMasterPhase: $isMasterPhase
                        masterTeam: $currentMasterTeam
                        winner: $winner
                        Raw values:
                        currentTeam from state: ${state?.get("currentTeam")}
                        currentTeam after uppercase: $currentTeam
                    """.trimIndent())

                    // Update clues
                    @Suppress("UNCHECKED_CAST")
                    val clues = state?.get("clues") as? List<Map<String, Any>> ?: emptyList()
                    redClues = clues.filter { it["team"] == "RED" }
                        .map { Clue(it["word"] as String, "RED") }
                    blueClues = clues.filter { it["team"] == "BLUE" }
                        .map { Clue(it["word"] as String, "BLUE") }
                }
            }
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
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = when (winner) {
                        "RED" -> "Red team found all their words!"
                        "BLUE" -> "Blue team found all their words!"
                        else -> "Game ended"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        navController.navigate("game_lobby/$roomId") {
                            popUpTo("game_lobby/$roomId") {
                                inclusive = true
                            }
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
                    Text("Return to Lobby", color = Color.White)
                }
            }
        }
        return
    }

    // Main game layout
    Column(modifier = Modifier.fillMaxSize()) {
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
                    .padding(end = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Red Team Info and Score
                Text(
                    text = redWordsRemaining.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.Red
                )
                Text(
                    "Red Team",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.Red
                )
                // Log/Clue History Section
                Text(
                    "Log",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(top = 8.dp)
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f) // Allow log to take available space in this column
                ) {
                    items(redClues) { clue ->
                        Text(
                            text = clue.word,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Red
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
                val cards = (gameState?.get("cards") as? List<Map<String, Any>>) ?: emptyList()

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
                            val word = card["word"] as? String ?: ""
                            val color = card["color"] as? String ?: "NEUTRAL"
                            val isRevealed = card["isRevealed"] as? Boolean ?: false
                            val cardIndex = cards.indexOf(card)

                            Card(
                                modifier = Modifier
                                    .width(cardWidth)
                                    .height(cardHeight)
                                    .clickable(
                                        enabled = !isRevealed && !isMasterPhase && currentTeam == currentTurn && !isMaster && winner == null
                                    ) {
                                        // Update card's revealed state in Firestore
                                        db.collection("rooms").document(roomId)
                                            .get()
                                            .addOnSuccessListener { document ->
                                                @Suppress("UNCHECKED_CAST")
                                                val state = document.get("gameState.codenames") as? Map<String, Any>
                                                @Suppress("UNCHECKED_CAST")
                                                val currentCards = state?.get("cards") as? List<Map<String, Any>> ?: emptyList()

                                                // Create updated cards list with the selected card marked as revealed
                                                val updatedCards = currentCards.toMutableList()
                                                updatedCards[cardIndex] = updatedCards[cardIndex].toMutableMap().apply {
                                                    put("isRevealed", true)
                                                }

                                                // Update remaining words count based on the revealed card's color
                                                val updates = mutableMapOf<String, Any>(
                                                    "gameState.codenames.cards" to updatedCards
                                                )

                                                when (color) {
                                                    "RED" -> {
                                                        if (currentTeam == "RED") {
                                                            // Current team is Red and revealed a Red card - continue turn
                                                            val newCount = redWordsRemaining - 1
                                                            updates["gameState.codenames.redWordsRemaining"] = newCount
                                                            if (newCount == 0) {
                                                                // Red team wins
                                                                updates["gameState.codenames.winner"] = "RED"
                                                            }
                                                        } else {
                                                            // Current team is Blue and revealed a Red card - switch turn to Red
                                                            updates["gameState.codenames.currentTeam"] = "RED"
                                                            updates["gameState.codenames.currentTurn"] = "RED"
                                                            updates["gameState.codenames.isMasterPhase"] = true
                                                             // Optionally, you could decrement the revealed team's word count here, but the standard rule just ends the turn.
                                                             // val newCount = redWordsRemaining - 1
                                                             // updates["gameState.codenames.redWordsRemaining"] = newCount
                                                        }
                                                    }
                                                    "BLUE" -> {
                                                         if (currentTeam == "BLUE") {
                                                            // Current team is Blue and revealed a Blue card - continue turn
                                                            val newCount = blueWordsRemaining - 1
                                                            updates["gameState.codenames.blueWordsRemaining"] = newCount
                                                            if (newCount == 0) {
                                                                // Blue team wins
                                                                updates["gameState.codenames.winner"] = "BLUE"
                                                            }
                                                        } else {
                                                            // Current team is Red and revealed a Blue card - switch turn to Blue
                                                            updates["gameState.codenames.currentTeam"] = "BLUE"
                                                            updates["gameState.codenames.currentTurn"] = "BLUE"
                                                            updates["gameState.codenames.isMasterPhase"] = true
                                                             // Optionally, you could decrement the revealed team's word count here.
                                                             // val newCount = blueWordsRemaining - 1
                                                             // updates["gameState.codenames.blueWordsRemaining"] = newCount
                                                        }
                                                    }
                                                    "ASSASSIN" -> {
                                                        // Game over - other team wins
                                                        updates["gameState.codenames.winner"] = if (currentTeam == "RED") "BLUE" else "RED"
                                                    }
                                                    "NEUTRAL" -> {
                                                        // Revealed a Neutral card - switch turns
                                                        val nextTeam = if (currentTeam == "RED") "BLUE" else "RED"
                                                        updates["gameState.codenames.currentTeam"] = nextTeam
                                                        updates["gameState.codenames.currentTurn"] = nextTeam
                                                        updates["gameState.codenames.isMasterPhase"] = true
                                                    }
                                                }

                                                // Update Firestore
                                                db.collection("rooms").document(roomId)
                                                    .update(updates)
                                                    .addOnFailureListener { e -> Log.e("Codenames", "Error updating Firestore after card click: $e") }
                                            }
                                            .addOnFailureListener { e -> Log.e("Codenames", "Error getting document on card click: $e") }
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = when {
                                        isMaster -> {
                                            when (color) {
                                                "RED" -> Color.Red
                                                "BLUE" -> Color.Blue
                                                "NEUTRAL" -> Color.Gray
                                                "ASSASSIN" -> Color.Black
                                                else -> Color.White
                                            }
                                        }
                                        isRevealed -> {
                                            when (color) {
                                                "RED" -> Color.Red
                                                "BLUE" -> Color.Blue
                                                "NEUTRAL" -> Color.Gray
                                                "ASSASSIN" -> Color.Black
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
                                        modifier = Modifier.padding(2.dp)
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
                    .padding(start = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Blue Team Info and Score
                Text(
                    text = blueWordsRemaining.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.Blue
                )
                Text(
                    "Blue Team",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.Blue
                )
                // Log/Clue History Section
                Text(
                    "Log",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(top = 8.dp)
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f) // Allow log to take available space in this column
                ) {
                    items(blueClues) { clue ->
                        Text(
                            text = clue.word,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Blue
                        )
                    }
                }
            }
        }

        // Bottom section for global controls/status (Timer, Master Input, General Status)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${timerSeconds}s",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Conditional display for Master Input or General Player Status
            if (isMaster && isMasterPhase && currentTeam == currentMasterTeam) {
                // Master input field for the current master's team
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = if (currentTeam == "RED") redMasterClue else blueMasterClue,
                        onValueChange = {
                            if (currentTeam == "RED") redMasterClue = it else blueMasterClue = it
                        },
                        label = { Text("Enter clue and number (e.g., 'APPLE 3')") }, // More descriptive label
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
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
                                    return@Button // Exit onClick if invalid
                                }

                                if (word.isNotEmpty()) {
                                    // Get current clues
                                    db.collection("rooms").document(roomId)
                                        .get()
                                        .addOnSuccessListener { document ->
                                            @Suppress("UNCHECKED_CAST")
                                            val state = document.get("gameState.codenames") as? Map<String, Any>
                                            @Suppress("UNCHECKED_CAST")
                                            val currentClues = state?.get("clues") as? List<Map<String, Any>> ?: emptyList()

                                            // Add new clue
                                            val newClue = mapOf(
                                                "word" to "$word $count", // Store clue as "WORD X"
                                                "team" to currentTeam
                                            )

                                            // Update Firestore with new clue and switch to team phase
                                            db.collection("rooms").document(roomId)
                                                .update(
                                                    mapOf(
                                                        "gameState.codenames.clues" to (currentClues + newClue),
                                                        "gameState.codenames.isMasterPhase" to false,
                                                        "gameState.codenames.currentGuardingWordCount" to count, // Add guarding word count to state
                                                        "gameState.codenames.guessesRemaining" to (count + 1) // Players get 1 more guess than the number provided
                                                    )
                                                )
                                                .addOnSuccessListener {
                                                    if (currentTeam == "RED") redMasterClue = "" else blueMasterClue = "" // Clear input field
                                                    Toast.makeText(context, "Clue submitted!", Toast.LENGTH_SHORT).show()
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.e("Codenames", "Error updating Firestore after clue submission: $e")
                                                    Toast.makeText(context, "Failed to submit clue: ${e.message}", Toast.LENGTH_LONG).show()
                                                }
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("Codenames", "Error getting document to submit clue: $e")
                                            Toast.makeText(context, "Failed to get game state: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                } else {
                                    Toast.makeText(context, "Clue word cannot be empty", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Invalid clue format. Use 'WORD NUMBER'", Toast.LENGTH_SHORT).show()
                                Log.e("Codenames", "Invalid clue format. Please use 'WORD NUMBER'. Input: $clueText")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Submit Clue")
                    }
                }
            } else {
                // Display Current Turn/Phase for non-masters or for the other master's turn
                val phaseText = if (isMasterPhase) "Master Phase" else "Guessing Phase"
                Text(
                    text = "Current Turn: $currentTurn ($phaseText)",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (currentTurn == "RED") Color.Red else Color.Blue
                )

                // Only show recent clues here if the current user is NOT the master,
                // or if they are the master but it's not their turn to give a clue.
                // The master's own clue history is less critical to see here, as they're focused on input.
                if (!(isMaster && isMasterPhase && currentTeam == currentMasterTeam)) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Recent Clues for ${currentTeam} Team",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    // Use a fixed small height for the clue list to prevent it from growing too large
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 80.dp) // Adjusted height to be more compact
                    ) {
                        items(if (currentTeam == "RED") redClues else blueClues) { clue ->
                            Text(
                                text = clue.word, // Clue will now be "WORD X"
                                style = MaterialTheme.typography.bodySmall,
                                color = if (clue.team == "RED") Color.Red else Color.Blue,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}