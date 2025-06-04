import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gamehub.features.triviatoe.model.PlayerAnswer
import com.example.gamehub.features.triviatoe.model.TriviatoePlayer
import com.example.gamehub.features.triviatoe.model.TriviatoeQuestion
import kotlinx.coroutines.delay
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun TriviatoeQuestionScreen(
    question: TriviatoeQuestion.MultipleChoice,
    playerId: String,
    players: List<TriviatoePlayer>,
    allAnswers: Map<String, PlayerAnswer?>,
    correctIndex: Int,
    onAnswer: (PlayerAnswer) -> Unit,
    onQuestionResolved: (winnerId: String) -> Unit
) {
    if (players.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val isHost = playerId == players.firstOrNull()?.uid

    var timeLeft by remember { mutableStateOf(10f) }
    var progress by remember { mutableStateOf(1f) }
    var selectedAnswer by remember { mutableStateOf<Int?>(null) }
    var showWinner by remember { mutableStateOf(false) }
    var winnerId by remember { mutableStateOf<String?>(null) }
    var winnerName by remember { mutableStateOf<String?>(null) }
    var randomized by remember { mutableStateOf(false) }

    // Reset all UI state on new question
    LaunchedEffect(question) {
        timeLeft = 10f
        progress = 1f
        showWinner = false
        winnerId = null
        winnerName = null
        randomized = false
        selectedAnswer = null
    }

    // Timer loop watches for both time and allAnswers
    LaunchedEffect(question, allAnswers.values.toList()) {
        while (timeLeft > 0 && players.size == 2 && allAnswers.values.any { it == null }) {
            delay(50)
            timeLeft -= 0.05f
            progress = timeLeft / 10f
        }
    }

    // Host resolves winner exactly once
    LaunchedEffect(
        question, allAnswers.values.toList(), timeLeft
    ) {
        val allAnsweredWithTimestamps = players.all {
            val ans = allAnswers[it.uid]
            ans != null && ans.timestamp != null
        }
        if (
            isHost &&
            players.size == 2 &&
            !showWinner &&
            (
                    allAnsweredWithTimestamps
                            || timeLeft <= 0f
                    )
        ) {
            val correctPlayersWithTime = allAnswers
                .filter { (_, ans) -> ans?.answerIndex == correctIndex && ans.timestamp != null }
                .toList()
                .sortedBy { it.second?.timestamp ?: Long.MAX_VALUE }
            winnerId = when {
                correctPlayersWithTime.isNotEmpty() -> correctPlayersWithTime.first().first
                allAnswers.isNotEmpty() -> allAnswers.keys.random()
                else -> null
            }
            winnerName = players.find { it.uid == winnerId }?.name ?: "?"
            randomized = correctPlayersWithTime.isEmpty()
            showWinner = true
            delay(1500)
            if (winnerId != null) {
                onQuestionResolved(winnerId!!)
            }
        }
    }

    val resolved = showWinner && winnerId != null

    // This Box fills the screen and centers the panel
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // This Box is your panel, with the image background, sizing to the content
        Box(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(32.dp))
                .padding(3.dp)
        ) {
            // Panel background image
            Image(
                painter = painterResource(id = com.example.gamehub.R.drawable.triviatoe_box1),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.matchParentSize()
            )

            // The actual question UI
            Column(
                modifier = Modifier
                    .padding(horizontal = 48.dp, vertical = 64.dp) // Try more padding!
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = progress.coerceAtLeast(0f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp),
                    color = Color(0xFF4C2232) // Green color
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Time remaining: ${timeLeft.toInt()}s",
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    question.question,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Spacer(Modifier.height(20.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    for (row in 0 until 2) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (col in 0 until 2) {
                                val idx = row * 2 + col
                                OutlinedButton(
                                    onClick = {
                                        if (selectedAnswer == null && !resolved) {
                                            selectedAnswer = idx
                                            onAnswer(PlayerAnswer(idx, null))
                                        }
                                    },
                                    enabled = selectedAnswer == null && allAnswers[playerId] == null && !resolved,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(4.dp)
                                        .height(56.dp)
                                ) {
                                    Text(
                                        question.answers.getOrNull(idx) ?: "",
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(28.dp))
                if (resolved) {
                    if (randomized) {
                        Text(
                            "Both players wrong or didn't answer! Randomized winner: $winnerName",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            "Player $winnerName has won!",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                selectedAnswer?.let {
                    Text(
                        "You selected: ${question.answers[it]}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        // Defensive timer: if stuck for 10s, show retry UI
        var questionTimeout by remember { mutableStateOf(false) }
        LaunchedEffect(showWinner) {
            if (showWinner) {
                delay(10000)
                questionTimeout = true
            }
        }
        if (questionTimeout) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Still waiting... Host may have network issues.",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(12.dp))
                    // Optional: Add reload or leave button
                }
            }
        }
    }
}
