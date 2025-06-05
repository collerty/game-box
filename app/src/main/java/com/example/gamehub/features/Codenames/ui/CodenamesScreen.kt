package com.example.gamehub.features.codenames.ui

import android.util.Log
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
    LaunchedEffect(isMasterPhase) {
        timerSeconds = if (isMasterPhase) 60 else 10 // Both phases get 60 seconds
        while (timerSeconds > 0) {
            delay(1000)
            timerSeconds--
        }
        // When timer ends, switch phases and teams if needed
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
        } else {
            // If master phase ends, switch to team phase
            db.collection("rooms").document(roomId)
                .update("gameState.codenames.isMasterPhase", false)
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
                    
                    // Debug logging for state updates
                    Log.d("CodenamesDebug", """
                        State Update:
                        currentTurn: $currentTurn
                        currentTeam: $currentTeam
                        isMasterPhase: $isMasterPhase
                        masterTeam: $currentMasterTeam
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

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Red Team Panel (Left)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(end = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Red Team",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.Red
            )
            Text(
                "$redWordsRemaining words remaining",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Red
            )
            if (currentTurn == "RED" && isMasterPhase) {
                Text(
                    "Master's Turn!",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.Red,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else if (currentTurn == "RED" && !isMasterPhase) {
                Text(
                    "Team's Turn!",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.Red,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            // Master input field for Red team
            if (isMaster && isMasterPhase && currentTeam == "RED") {
                OutlinedTextField(
                    value = redMasterClue,
                    onValueChange = { redMasterClue = it },
                    label = { Text("Enter clue (e.g., 'word-2', 'word - 0', 'word0')") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
                Button(
                    onClick = {
                        if (redMasterClue.isNotEmpty()) {
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
                                        "word" to redMasterClue,
                                        "team" to currentTeam
                                    )
                                    
                                    // Update Firestore with new clue and switch to team phase
                                    db.collection("rooms").document(roomId)
                                        .update(
                                            mapOf(
                                                "gameState.codenames.clues" to (currentClues + newClue),
                                                "gameState.codenames.isMasterPhase" to false
                                            )
                                        )
                                        .addOnSuccessListener {
                                            redMasterClue = "" // Clear input field
                                        }
                                }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Submit Clue")
                }
            }
            
            // Clue history for Red team
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(redClues) { clue ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Red.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Text(
                                text = clue.word,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.Red
                            )
                        }
                    }
                }
            }
        }

        // Cards Grid (Center)
        Column(
            modifier = Modifier
                .weight(2f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Timer display
            Text(
                text = "${timerSeconds}s",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Phase indicator
            Text(
                text = when {
                    isMasterPhase && currentTurn == "RED" -> "Red Master's Turn"
                    !isMasterPhase && currentTurn == "RED" -> "Red Team's Turn"
                    isMasterPhase && currentTurn == "BLUE" -> "Blue Master's Turn"
                    else -> "Blue Team's Turn"
                },
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            @Suppress("UNCHECKED_CAST")
            val cards = (gameState?.get("cards") as? List<Map<String, Any>>) ?: emptyList()
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cards) { card ->
                    val word = card["word"] as? String ?: ""
                    val color = card["color"] as? String ?: "NEUTRAL"
                    val isRevealed = card["isRevealed"] as? Boolean ?: false

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                            .background(
                                when {
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
                            .clickable(enabled = !isRevealed && !isMasterPhase && currentTurn == currentTeam) {
                                // TODO: Handle card click
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = word,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = if (isMaster || isRevealed) Color.White else Color.Black
                        )
                    }
                }
            }
        }

        // Blue Team Panel (Right)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Blue Team",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.Blue
            )
            Text(
                "$blueWordsRemaining words remaining",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Blue
            )
            if (currentTurn == "BLUE" && isMasterPhase) {
                Text(
                    "Master's Turn!",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.Blue,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else if (currentTurn == "BLUE" && !isMasterPhase) {
                Text(
                    "Team's Turn!",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.Blue,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            // Master input field for Blue team
            if (isMaster && isMasterPhase && currentTeam == "BLUE") {
                OutlinedTextField(
                    value = blueMasterClue,
                    onValueChange = { blueMasterClue = it },
                    label = { Text("Enter clue (e.g., 'word-2', 'word - 0', 'word0')") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
                Button(
                    onClick = {
                        if (blueMasterClue.isNotEmpty()) {
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
                                        "word" to blueMasterClue,
                                        "team" to currentTeam
                                    )
                                    
                                    // Update Firestore with new clue and switch to team phase
                                    db.collection("rooms").document(roomId)
                                        .update(
                                            mapOf(
                                                "gameState.codenames.clues" to (currentClues + newClue),
                                                "gameState.codenames.isMasterPhase" to false
                                            )
                                        )
                                        .addOnSuccessListener {
                                            blueMasterClue = "" // Clear input field
                                        }
                                }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Submit Clue")
                }
            }
            
            // Clue history for Blue team
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(blueClues) { clue ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Blue.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Text(
                                text = clue.word,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.Blue
                            )
                        }
                    }
                }
            }
        }
    }
} 