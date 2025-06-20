import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.example.gamehub.features.triviatoe.model.PlayerAnswer
import com.example.gamehub.features.triviatoe.model.TriviatoeQuestion
import com.example.gamehub.features.triviatoe.model.TriviatoeSession
import com.example.gamehub.features.triviatoe.model.TriviatoeRoundState
import kotlinx.coroutines.delay
import android.media.MediaPlayer
import androidx.compose.ui.platform.LocalContext

@Composable
fun TriviatoeQuestionScreen(
    session: TriviatoeSession,
    playerId: String,
    randomized: Boolean,
    onAnswer: (PlayerAnswer) -> Unit,
    onQuestionResolved: (winnerId: String, randomized: Boolean) -> Unit
) {

    val context = LocalContext.current

    DisposableEffect(key1 = true) {
        val mediaPlayer = MediaPlayer.create(context, com.example.gamehub.R.raw.triviatoe_timer)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()
        onDispose {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            } catch (_: Exception) {}
        }
    }

    val question = session.quizQuestion as? TriviatoeQuestion.MultipleChoice
    val players = session.players
    val allAnswers = session.answers
    val correctIndex = question?.correctIndex ?: 0

    if (question == null || players.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    val isHost = playerId == players.firstOrNull()?.uid

    // Timer and UI state
    val timerKey = "${session.currentRound}_${question.question}"
    var timeLeft by remember(timerKey) { mutableStateOf(10f) }
    var progress by remember(timerKey) { mutableStateOf(1f) }
    var selectedAnswer by remember(timerKey) { mutableStateOf<Int?>(null) }
    var questionTimeout by remember(timerKey) { mutableStateOf(false) }

    // Timer logic
    LaunchedEffect(timerKey) {
        while (
            timeLeft > 0f &&
            players.size == 2 &&
            (allAnswers.size < players.size || allAnswers.values.any { it == null })
        ) {
            delay(50)
            timeLeft -= 0.05f
            progress = timeLeft / 10f
        }
    }

    // Host: Write winner/randomized to Firestore exactly once
    LaunchedEffect(question, allAnswers.size, timeLeft) {
        if (
            isHost &&
            players.size == 2 &&
            session.firstToMove == null
        ) {
            val correctPlayersWithTime = allAnswers
                .filter { (_, ans) -> ans?.answerIndex == correctIndex && ans.timestamp != null }
                .toList()
                .sortedBy { it.second?.timestamp ?: Long.MAX_VALUE }

            val bothAnswered = allAnswers.size == 2 && allAnswers.values.all { it?.timestamp != null }
            val timeout = timeLeft <= 0f

            if (bothAnswered || timeout) {
                val winnerId = when {
                    correctPlayersWithTime.isNotEmpty() -> correctPlayersWithTime.first().first
                    allAnswers.isNotEmpty() -> allAnswers.keys.random()
                    else -> players.random().uid
                }
                val randomized = correctPlayersWithTime.isEmpty()
                onQuestionResolved(winnerId, randomized)
            }
        }
    }

    // Defensive timeout if stuck
    LaunchedEffect(session.firstToMove) {
        if (session.firstToMove != null) {
            delay(10000)
            questionTimeout = true
        }
    }

    // Only show result if it's for this round/question (not leftover from last round)
    val showWinner = (session.state == TriviatoeRoundState.QUESTION || session.state == TriviatoeRoundState.REVEAL)
            && session.firstToMove != null

    val winnerIdFromFirestore = if (showWinner) session.firstToMove else null
    val winnerNameFromFirestore = players.find { it.uid == winnerIdFromFirestore }?.name ?: "?"
    val randomizedFromFirestore = showWinner && session.randomized == true
    val resolved = showWinner

    // --- Dynamic Button Sizing ---
    val answers = question.answers.map { it ?: "" }
    val textStyle = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Medium)
    val buttonSizes = remember { mutableStateListOf<Size>() }
    var maxButtonWidth by remember { mutableStateOf(0.dp) }
    var maxButtonHeight by remember { mutableStateOf(0.dp) }
    val density = androidx.compose.ui.platform.LocalDensity.current

    // Measure all answers, find largest size
    LaunchedEffect(buttonSizes.toList()) {
        if (buttonSizes.size == 4) {
            maxButtonWidth = with(density) { buttonSizes.maxOf { it.width }.toDp() + 18.dp }
            maxButtonHeight = with(density) { buttonSizes.maxOf { it.height }.toDp() + 50.dp }
        }
    }

    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val useColumn = screenWidthDp < 380
    val isSmallScreen = screenWidthDp < 400


    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val minHeight = if (isSmallScreen) 580.dp else 100.dp
        // Panel with sprite background
        Box(
            modifier = Modifier
                .widthIn(max = 440.dp) // more space!
                .fillMaxWidth(0.97f)
                .heightIn(min = minHeight)
                .clip(RoundedCornerShape(32.dp))
                .padding(2.dp)
        ) {
            Image(
                painter = painterResource(id = com.example.gamehub.R.drawable.triviatoe_box1),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.matchParentSize()
            )

            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 14.dp) // slightly tighter vertical for more bottom space
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // More space at the top
                Spacer(Modifier.height(if (isSmallScreen) 55.dp else 50.dp))
                LinearProgressIndicator(
                    progress = progress.coerceAtLeast(0f),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(10.dp),
                    color = Color(0xFF4C2232)
                )
                Spacer(Modifier.height(if (isSmallScreen) 14.dp else 24.dp))
                Text(
                    "Time remaining: ${timeLeft.toInt()}s",
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(if (isSmallScreen) 10.dp else 18.dp))
                Text(
                    question.question,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 18.dp)
                )
                Spacer(Modifier.height(if (isSmallScreen) 8.dp else 12.dp))

                // Invisible measurement
                answers.forEachIndexed { idx, ans ->
                    if (buttonSizes.size <= idx) {
                        Text(
                            text = ans,
                            style = textStyle,
                            maxLines = 3,
                            modifier = Modifier
                                .padding(8.dp)
                                .widthIn(max = 230.dp)
                                .onGloballyPositioned { layoutCoordinates ->
                                    val size = layoutCoordinates.size.toSize()
                                    if (buttonSizes.size <= idx) buttonSizes.add(size)
                                },
                            softWrap = true,
                            color = Color.Transparent
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Buttons
                if (buttonSizes.size == 4) {
                    if (useColumn) {
                        Column(
                            modifier = Modifier.fillMaxWidth(0.95f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            for (i in 0..3) {
                                SpriteButton(
                                    text = answers.getOrNull(i) ?: "",
                                    onClick = {
                                        if (selectedAnswer == null && !resolved) {
                                            val player = MediaPlayer.create(context, com.example.gamehub.R.raw.triviatoe_select)
                                            player?.setOnCompletionListener { it.release() }
                                            player?.start()

                                            selectedAnswer = i
                                            onAnswer(PlayerAnswer(i, null))
                                        }
                                    },
                                    enabled = selectedAnswer == null && allAnswers[playerId] == null && !resolved,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(maxButtonHeight.coerceAtLeast(64.dp))
                                        .padding(vertical = 5.dp),
                                    minWidth = maxButtonWidth
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(0.95f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            for (row in 0 until 2) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                                ) {
                                    for (col in 0 until 2) {
                                        val idx = row * 2 + col
                                        SpriteButton(
                                            text = answers.getOrNull(idx) ?: "",
                                            onClick = {
                                                if (selectedAnswer == null && !resolved) {
                                                    val player = MediaPlayer.create(context, com.example.gamehub.R.raw.triviatoe_select)
                                                    player?.setOnCompletionListener { it.release() }
                                                    player?.start()

                                                    selectedAnswer = idx
                                                    onAnswer(PlayerAnswer(idx, null))
                                                }
                                            },
                                            enabled = selectedAnswer == null && allAnswers[playerId] == null && !resolved,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(maxButtonHeight)
                                                .padding(vertical = 5.dp),
                                            minWidth = maxButtonWidth
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // "You selected" is close to the buttons
                if (selectedAnswer != null) {
                    Spacer(Modifier.height(if (isSmallScreen) 8.dp else 12.dp))
                    Text(
                        "You selected: ${question.answers[selectedAnswer!!]}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = if (isSmallScreen) 12.sp else 14.sp),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(10.dp))
                // Padding before the winner/result
                Spacer(Modifier.height(if (resolved) 15.dp else 0.dp))

                if (resolved) {
                    val winner = players.find { it.uid == winnerIdFromFirestore }
                    val winnerSpriteRes = when (winner?.symbol) {
                        "X" -> com.example.gamehub.R.drawable.x_icon
                        "O" -> com.example.gamehub.R.drawable.o_icon
                        else -> null
                    }
                    // Display winner text and icon side by side
                    if (randomizedFromFirestore) {
                        Text(
                            "Both players wrong or out of time!",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = if (isSmallScreen) 13.sp else 16.sp),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(5.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Winner after randomization: ",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = if (isSmallScreen) 13.sp else 16.sp),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                winnerNameFromFirestore,
                                color = Color.Yellow,
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = if (isSmallScreen) 13.sp else 16.sp),
                                modifier = Modifier.padding(horizontal = 3.dp)
                            )
                            winnerSpriteRes?.let { resId ->
                                Image(
                                    painter = painterResource(id = resId),
                                    contentDescription = "Winner sprite",
                                    modifier = Modifier.size(if (isSmallScreen) 30.dp else 32.dp)
                                )
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Winner is: ",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = if (isSmallScreen) 13.sp else 16.sp),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                winnerNameFromFirestore,
                                color = Color.Yellow,
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = if (isSmallScreen) 13.sp else 16.sp),
                                modifier = Modifier.padding(horizontal = 3.dp)
                            )
                            winnerSpriteRes?.let { resId ->
                                Image(
                                    painter = painterResource(id = resId),
                                    contentDescription = "Winner sprite",
                                    modifier = Modifier.size(if (isSmallScreen) 30.dp else 32.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(if (isSmallScreen) 50.dp else 14.dp))
                }

                // Big bottom padding for sprite border safety
                Spacer(Modifier.height(if (isSmallScreen) 40.dp else 30.dp))
            }
        }

        // Defensive timer: if stuck for 10s, show retry UI
        if (questionTimeout) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Still waiting... Host may have network issues.",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun SpriteButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    minWidth: Dp = 0.dp
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .widthIn(min = minWidth)
    ) {
        Image(
            painter = painterResource(id = com.example.gamehub.R.drawable.triviatoe_button),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.matchParentSize()
        )
        TextButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxSize(),
            colors = ButtonDefaults.textButtonColors(
                containerColor = Color.Transparent,
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .fillMaxWidth(),
                softWrap = true,
                overflow = TextOverflow.Visible,
                maxLines = 3
            )
        }
    }
}
