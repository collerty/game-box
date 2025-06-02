import android.util.Log
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

@Composable
fun TriviatoeQuestionScreen(
    question: TriviatoeQuestion.MultipleChoice,
    playerId: String,
    players: List<TriviatoePlayer>,
    allAnswers: Map<String, PlayerAnswer?>, // playerId -> PlayerAnswer (null if not answered)
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

    // Debug
    Log.d("TriviatoeDebug", "RENDER players: ${players.map { it.uid }} answers: $allAnswers, timeLeft=$timeLeft, state host=$isHost showWinner=$showWinner winnerId=$winnerId")

    // Reset all UI state on new question
    LaunchedEffect(question) {
        timeLeft = 10f
        progress = 1f
        showWinner = false
        winnerId = null
        winnerName = null
        randomized = false
        selectedAnswer = null
        Log.d("TriviatoeDebug", "New question started - resetting timer")
    }

    // Timer loop watches for both time and allAnswers
    LaunchedEffect(question, allAnswers.values.toList()) {
        // Only run timer if there are unanswered players
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
        Log.d("TriviatoeDebug", "LaunchedEffect: isHost=$isHost, showWinner=$showWinner, answers=$allAnswers, timeLeft=$timeLeft")
        if (
            isHost &&
            players.size == 2 &&
            !showWinner &&
            (
                    (allAnswers.size == 2 && allAnswers.values.all { it != null })
                            || timeLeft <= 0f
                    )
        ) {
            // Find correct answers, with timestamps!
            val correctPlayersWithTime = allAnswers
                .filter { (_, ans) -> ans?.answerIndex == correctIndex && ans.timestamp != null }
                .toList()
                .sortedBy { it.second?.timestamp ?: Long.MAX_VALUE }

            // Pick winner: fastest correct, else random
            winnerId = when {
                correctPlayersWithTime.isNotEmpty() -> correctPlayersWithTime.first().first
                allAnswers.isNotEmpty() -> allAnswers.keys.random() // fallback if nobody correct
                else -> null
            }
            winnerName = players.find { it.uid == winnerId }?.name ?: "?"
            randomized = correctPlayersWithTime.isEmpty()
            showWinner = true
            Log.d("TriviatoeDebug", "Winner resolved: winnerId=$winnerId winnerName=$winnerName randomized=$randomized")
            delay(1500)
            if (winnerId != null) {
                onQuestionResolved(winnerId!!)
            }
        }
    }

    val resolved = showWinner && winnerId != null

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LinearProgressIndicator(
            progress = progress.coerceAtLeast(0f),
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text("Time remaining: ${timeLeft.toInt()}s")
        Spacer(Modifier.height(16.dp))
        Text(
            question.question,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Column {
            for (row in 0 until 2) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    for (col in 0 until 2) {
                        val idx = row * 2 + col
                        OutlinedButton(
                            onClick = {
                                if (selectedAnswer == null && !resolved) {
                                    val now = System.currentTimeMillis()
                                    selectedAnswer = idx
                                    onAnswer(PlayerAnswer(idx, now))
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
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        if (resolved) {
            if (randomized) {
                Text(
                    "Both players wrong or didn't answer! Randomized winner: $winnerName",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )
            } else {
                Text(
                    "Player $winnerName has won!",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        selectedAnswer?.let {
            Text(
                "You selected: ${question.answers[it]}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
