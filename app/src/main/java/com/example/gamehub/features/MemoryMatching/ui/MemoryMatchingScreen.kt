package com.example.gamehub.features.MemoryMatching.ui

import android.media.MediaPlayer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gamehub.R
import kotlinx.coroutines.delay

// Data class to hold card information
data class MemoryCard(
    val id: Int, // Unique ID for the card instance
    val imageRes: Int, // Drawable resource ID for the image
    var isFlipped: Boolean = false,
    var isMatched: Boolean = false
)

// Define the custom font
val arcadeFontFamily = FontFamily(
    Font(R.font.arcade_classic, FontWeight.Normal)
)

private const val MAX_TURN_ATTEMPTS = 5 // Max incorrect attempts in a row before losing
private val STATS_FONT_SIZE = 28.sp // Increased font size for stats

// Enum for managing screen states
enum class GameScreen {
    DIFFICULTY_SELECTION,
    PLAYING
}

// Data class for defining difficulty levels
data class GameDifficulty(
    val pairs: Int,
    val columns: Int,
    val displayName: String,
    val cardBackResId: Int, // Resource ID for the card back image
    val timeLimitSeconds: Int, // Time limit for this difficulty
    val totalCards: Int = pairs * 2
)

@Composable
fun MemoryMatchingScreen() {
    var currentScreen by remember { mutableStateOf(GameScreen.DIFFICULTY_SELECTION) }
    var currentDifficulty by remember { mutableStateOf<GameDifficulty?>(null) }

    val allImageResources = remember {
        listOf(
            R.drawable.basketball, R.drawable.bee, R.drawable.dice, R.drawable.herosword,
            R.drawable.ladybug, R.drawable.ramen, R.drawable.taxi, R.drawable.zombie,
            R.drawable.zelda, R.drawable.spaceman, R.drawable.robot, R.drawable.island,
            R.drawable.gamingcontroller, R.drawable.dragon, R.drawable.browncar
        )
    }

    val gameDifficulties = remember {
        listOf(
            GameDifficulty(pairs = 6, columns = 3, displayName = "Easy (3x4 - 12 Cards)", cardBackResId = R.drawable.card_back_light_blue, timeLimitSeconds = 50),
            GameDifficulty(pairs = 8, columns = 4, displayName = "Medium (4x4 - 16 Cards)", cardBackResId = R.drawable.card_back_red, timeLimitSeconds = 60),
            GameDifficulty(pairs = 10, columns = 4, displayName = "Hard (4x5 - 20 Cards)", cardBackResId = R.drawable.cards_back_blue, timeLimitSeconds = 75),
            GameDifficulty(pairs = 12, columns = 4, displayName = "Expert (4x6 - 24 Cards)", cardBackResId = R.drawable.back_card_brown, timeLimitSeconds = 90)
        )
    }

    var cards by remember { mutableStateOf(emptyList<MemoryCard>().toMutableStateList()) }
    var flippedCardIndices by remember { mutableStateOf<List<Int>>(emptyList()) }
    var processingMatch by remember { mutableStateOf(false) }
    var attemptCount by remember { mutableIntStateOf(0) } // Renamed to "Flips" in UI
    var currentTurnIncorrectAttempts by remember { mutableIntStateOf(0) }
    var allPairsMatched by remember { mutableStateOf(false) }
    var timeLeftInSeconds by remember { mutableIntStateOf(gameDifficulties.first().timeLimitSeconds) } // Default to first difficulty's time
    var isTimerRunning by remember { mutableStateOf(false) }
    var showLoseScreen by remember { mutableStateOf(false) }
    var loseReason by remember { mutableStateOf<String?>(null) }

    val cardFrontColor = Color(0xFF9092D8)
    val textColor = Color(0xFFE0E0E0)
    val accentColor = Color(0xFFF487B6)
    val buttonTextColor = Color.Black
    val overlayColor = Color.Black.copy(alpha = 0.90f)
    val difficultySelectionBackgroundColor = Color.Black.copy(alpha = 0.6f)
    val congratsTitleColor = Color(0xFF76F7BF)
    val loseTitleColor = Color(0xFFF44336)
    val detailColor = Color.White

    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    val cardFlipSoundPlayer = remember { MediaPlayer.create(context, R.raw.card_flip).apply { setVolume(0.3f, 0.3f) } }
    val winSoundPlayer = remember { MediaPlayer.create(context, R.raw.card_flip__win).apply { setVolume(0.6f, 0.6f) } }
    val matchSoundPlayer = remember { MediaPlayer.create(context, R.raw.dats_right).apply { setVolume(0.5f, 0.5f) } }
    val noMatchSoundPlayer = remember { MediaPlayer.create(context, R.raw.dats_wrong).apply { setVolume(0.5f, 0.5f) } }
    val loseGameSoundPlayer = remember { MediaPlayer.create(context, R.raw.matching_gamelose_sound).apply { setVolume(0.6f, 0.6f) } }


    DisposableEffect(Unit) {
        onDispose {
            cardFlipSoundPlayer.release()
            winSoundPlayer.release()
            matchSoundPlayer.release()
            noMatchSoundPlayer.release()
            loseGameSoundPlayer.release()
        }
    }

    fun playCardFlipSound() { if (cardFlipSoundPlayer.isPlaying) cardFlipSoundPlayer.seekTo(0) else cardFlipSoundPlayer.start() }
    fun playWinSound() { if (winSoundPlayer.isPlaying) winSoundPlayer.seekTo(0) else winSoundPlayer.start() }
    fun playMatchSound() { if (matchSoundPlayer.isPlaying) matchSoundPlayer.seekTo(0) else matchSoundPlayer.start() }
    fun playNoMatchSound() { if (noMatchSoundPlayer.isPlaying) noMatchSoundPlayer.seekTo(0) else noMatchSoundPlayer.start() }
    fun playLoseSound() { if (loseGameSoundPlayer.isPlaying) loseGameSoundPlayer.seekTo(0) else loseGameSoundPlayer.start() }


    fun generateCardsForDifficulty(difficulty: GameDifficulty): SnapshotStateList<MemoryCard> {
        val numPairs = difficulty.pairs
        val uniqueImagesToTake = minOf(numPairs, allImageResources.size)
        val selectedImages = allImageResources.shuffled().take(uniqueImagesToTake)
        return (selectedImages + selectedImages)
            .mapIndexed { index, resId -> MemoryCard(id = index, imageRes = resId) }
            .shuffled()
            .toMutableStateList()
    }

    val startGameAction: (GameDifficulty) -> Unit = { difficulty ->
        currentDifficulty = difficulty
        cards = generateCardsForDifficulty(difficulty)
        flippedCardIndices = emptyList()
        attemptCount = 0
        currentTurnIncorrectAttempts = 0
        allPairsMatched = false
        processingMatch = false
        timeLeftInSeconds = difficulty.timeLimitSeconds
        isTimerRunning = true
        showLoseScreen = false
        loseReason = null
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (winSoundPlayer.isPlaying) {
            winSoundPlayer.stop()
            winSoundPlayer.prepareAsync()
        }
        if (loseGameSoundPlayer.isPlaying) {
            loseGameSoundPlayer.stop()
            loseGameSoundPlayer.prepareAsync()
        }
        currentScreen = GameScreen.PLAYING
    }

    val restartCurrentGameAction: () -> Unit = {
        currentDifficulty?.let { difficulty ->
            startGameAction(difficulty)
        } ?: run {
            currentScreen = GameScreen.DIFFICULTY_SELECTION
        }
    }

    LaunchedEffect(isTimerRunning, timeLeftInSeconds, allPairsMatched, showLoseScreen) {
        if (currentScreen == GameScreen.PLAYING && isTimerRunning && !allPairsMatched && !showLoseScreen) {
            if (timeLeftInSeconds > 0) {
                delay(1000)
                timeLeftInSeconds--
            } else {
                isTimerRunning = false
                if (!allPairsMatched && !showLoseScreen) {
                    loseReason = "Time's Up!"
                    showLoseScreen = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }
        }
    }

    LaunchedEffect(showLoseScreen) {
        if (showLoseScreen && !allPairsMatched) {
            playLoseSound()
            if (winSoundPlayer.isPlaying) {
                winSoundPlayer.stop()
                winSoundPlayer.prepareAsync()
            }
        }
    }

    LaunchedEffect(flippedCardIndices, currentScreen) {
        if (currentScreen != GameScreen.PLAYING || allPairsMatched || showLoseScreen) {
            if(processingMatch) processingMatch = false
            return@LaunchedEffect
        }

        if (flippedCardIndices.size == 2) {
            if (processingMatch) return@LaunchedEffect

            processingMatch = true
            val firstIndex = flippedCardIndices[0]
            val secondIndex = flippedCardIndices[1]
            val card1 = cards[firstIndex]
            val card2 = cards[secondIndex]

            if (card1.imageRes == card2.imageRes) { // Match
                playMatchSound()
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                val newCards = cards.toMutableStateList()
                newCards[firstIndex] = card1.copy(isMatched = true, isFlipped = true)
                newCards[secondIndex] = card2.copy(isMatched = true, isFlipped = true)
                cards = newCards
                currentTurnIncorrectAttempts = 0

                if (cards.all { it.isMatched }) {
                    allPairsMatched = true
                    isTimerRunning = false
                    playWinSound()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                flippedCardIndices = emptyList()
                processingMatch = false
            } else { // No match
                playNoMatchSound()
                currentTurnIncorrectAttempts++
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                delay(1000)

                if (!allPairsMatched && !showLoseScreen) {
                    val newCards = cards.toMutableStateList()
                    newCards[firstIndex] = card1.copy(isFlipped = false)
                    newCards[secondIndex] = card2.copy(isFlipped = false)
                    cards = newCards
                }
                flippedCardIndices = emptyList()
                processingMatch = false

                if (!allPairsMatched && !showLoseScreen && currentTurnIncorrectAttempts >= MAX_TURN_ATTEMPTS) {
                    loseReason = "Too many mistakes!"
                    showLoseScreen = true
                    isTimerRunning = false
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }
        }
    }


    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.purple_background),
            contentDescription = "Game Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        when (currentScreen) {
            GameScreen.DIFFICULTY_SELECTION -> {
                DifficultySelectionContent(
                    difficulties = gameDifficulties,
                    onDifficultySelected = { difficulty ->
                        startGameAction(difficulty)
                    },
                    backgroundColor = difficultySelectionBackgroundColor,
                    textColor = textColor,
                    accentColor = accentColor,
                    buttonTextColor = buttonTextColor
                )
            }
            GameScreen.PLAYING -> {
                val difficulty = currentDifficulty ?: gameDifficulties.first()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Time: $timeLeftInSeconds",
                            fontSize = STATS_FONT_SIZE,
                            fontFamily = arcadeFontFamily,
                            color = textColor,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Flips: $attemptCount",
                            fontSize = STATS_FONT_SIZE,
                            fontFamily = arcadeFontFamily,
                            color = textColor,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Mistakes: $currentTurnIncorrectAttempts / $MAX_TURN_ATTEMPTS",
                            fontSize = STATS_FONT_SIZE,
                            fontFamily = arcadeFontFamily,
                            color = if (currentTurnIncorrectAttempts >= MAX_TURN_ATTEMPTS -1) loseTitleColor else textColor,
                            textAlign = TextAlign.Center
                        )
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(difficulty.columns),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        itemsIndexed(cards) { index, card ->
                            GameCardItem(
                                card = card,
                                onClick = {
                                    if (!processingMatch && !card.isFlipped && !card.isMatched && flippedCardIndices.size < 2) {
                                        playCardFlipSound()
                                        val newCards = cards.toMutableStateList()
                                        newCards[index] = card.copy(isFlipped = true)
                                        cards = newCards
                                        flippedCardIndices = flippedCardIndices + index
                                        if (flippedCardIndices.size == 1) {
                                            attemptCount++
                                        }
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                },
                                cardFrontColor = cardFrontColor,
                                cardBackImageRes = difficulty.cardBackResId,
                                processingMatch = processingMatch,
                                numFlippedCards = flippedCardIndices.size,
                                accentColor = accentColor,
                                isGameEnded = allPairsMatched || showLoseScreen
                            )
                        }
                    }

                    // Buttons are now part of the overlays below, no longer here.
                    // if (allPairsMatched || showLoseScreen) { ... } // REMOVED
                }

                // Win Screen Overlay
                AnimatedVisibility(
                    visible = allPairsMatched,
                    enter = fadeIn(animationSpec = tween(1000)) + scaleIn(animationSpec = tween(1000)),
                    exit = fadeOut(animationSpec = tween(500)) + scaleOut(animationSpec = tween(500))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(overlayColor)
                            .clickable(enabled = false, onClick = {}), // Consume clicks
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("YOU WIN!", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = congratsTitleColor, fontFamily = arcadeFontFamily)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Flips: $attemptCount", fontSize = 24.sp, color = detailColor, fontFamily = arcadeFontFamily)
                            Text("Time Left: $timeLeftInSeconds s", fontSize = 24.sp, color = detailColor, fontFamily = arcadeFontFamily)
                            Spacer(modifier = Modifier.height(24.dp)) // Space before buttons
                            Button(
                                onClick = restartCurrentGameAction,
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                modifier = Modifier.fillMaxWidth(0.7f)
                            ) {
                                Text("Play Again", color = buttonTextColor, fontFamily = arcadeFontFamily, fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { currentScreen = GameScreen.DIFFICULTY_SELECTION },
                                colors = ButtonDefaults.buttonColors(containerColor = textColor.copy(alpha = 0.8f)),
                                modifier = Modifier.fillMaxWidth(0.7f)
                            ) {
                                Text("Change Difficulty", color = buttonTextColor, fontFamily = arcadeFontFamily, fontSize = 20.sp)
                            }
                        }
                    }
                }

                // Lose Screen Overlay
                AnimatedVisibility(
                    visible = showLoseScreen && !allPairsMatched, // Don't show if already won
                    enter = fadeIn(animationSpec = tween(1000)) + scaleIn(animationSpec = tween(1000)),
                    exit = fadeOut(animationSpec = tween(500)) + scaleOut(animationSpec = tween(500))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(overlayColor)
                            .clickable(enabled = false, onClick = {}), // Consume clicks
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(loseReason ?: "GAME OVER", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = loseTitleColor, fontFamily = arcadeFontFamily, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Flips: $attemptCount", fontSize = 24.sp, color = detailColor, fontFamily = arcadeFontFamily)
                            if (loseReason == "Time's Up!") {
                                Text("You ran out of time!", fontSize = 20.sp, color = detailColor, fontFamily = arcadeFontFamily)
                            } else if (loseReason == "Too many mistakes!") {
                                Text("You made $currentTurnIncorrectAttempts mistakes in a row.", fontSize = 20.sp, color = detailColor, fontFamily = arcadeFontFamily, textAlign = TextAlign.Center)
                            }
                            Spacer(modifier = Modifier.height(24.dp)) // Space before buttons
                            Button(
                                onClick = restartCurrentGameAction,
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                modifier = Modifier.fillMaxWidth(0.7f)
                            ) {
                                Text("Play Again", color = buttonTextColor, fontFamily = arcadeFontFamily, fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { currentScreen = GameScreen.DIFFICULTY_SELECTION },
                                colors = ButtonDefaults.buttonColors(containerColor = textColor.copy(alpha = 0.8f)),
                                modifier = Modifier.fillMaxWidth(0.7f)
                            ) {
                                Text("Change Difficulty", color = buttonTextColor, fontFamily = arcadeFontFamily, fontSize = 20.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DifficultySelectionContent(
    difficulties: List<GameDifficulty>,
    onDifficultySelected: (GameDifficulty) -> Unit,
    backgroundColor: Color,
    textColor: Color,
    accentColor: Color,
    buttonTextColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Select Difficulty",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                fontFamily = arcadeFontFamily,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            difficulties.forEach { difficulty ->
                Button(
                    onClick = { onDifficultySelected(difficulty) },
                    modifier = Modifier.fillMaxWidth(0.8f),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    Text(
                        difficulty.displayName,
                        color = buttonTextColor,
                        fontFamily = arcadeFontFamily,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}


@Composable
fun GameCardItem(
    card: MemoryCard,
    onClick: () -> Unit,
    cardFrontColor: Color,
    cardBackImageRes: Int,
    processingMatch: Boolean,
    numFlippedCards: Int,
    accentColor: Color,
    isGameEnded: Boolean
) {
    val animatedRotationY by animateFloatAsState(
        targetValue = if (card.isFlipped || card.isMatched) 0f else 180f,
        animationSpec = tween(durationMillis = 500),
        label = "cardRotation"
    )

    val clickableEnabled = !processingMatch &&
            !card.isMatched &&
            !card.isFlipped &&
            numFlippedCards < 2 &&
            !isGameEnded

    val cardContainerColor = if (animatedRotationY < 90f) cardFrontColor else Color.Transparent

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .graphicsLayer {
                rotationY = animatedRotationY
                cameraDistance = 12f * density
            }
            .clickable(onClick = onClick, enabled = clickableEnabled)
            .border(
                width = 2.dp,
                color = if (card.isMatched) accentColor.copy(alpha = 0.7f)
                else if (card.isFlipped && animatedRotationY < 90f) accentColor.copy(alpha = 0.5f)
                else Color.Transparent,
                shape = RectangleShape
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp, pressedElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardContainerColor
        ),
        shape = RectangleShape
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (animatedRotationY < 90f) {
                Image(
                    painter = painterResource(id = card.imageRes),
                    contentDescription = "Card Image ${card.id}",
                    modifier = Modifier
                        .fillMaxSize(0.8f)
                        .graphicsLayer { rotationY = 180f },
                    contentScale = ContentScale.Fit
                )
            } else {
                Image(
                    painter = painterResource(id = cardBackImageRes),
                    contentDescription = "Card Back",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 1f
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MemoryMatchingScreenPreview() {
    MaterialTheme {
        MemoryMatchingScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun DifficultySelectionPreview() {
    MaterialTheme {
        Box {
            Image(
                painter = painterResource(id = R.drawable.purple_background),
                contentDescription = "Preview Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            DifficultySelectionContent(
                difficulties = listOf(
                    GameDifficulty(pairs = 6, columns = 3, displayName = "Easy (3x4 - 12 Cards)", cardBackResId = R.drawable.card_back_light_blue, timeLimitSeconds = 50),
                    GameDifficulty(pairs = 8, columns = 4, displayName = "Medium (4x4 - 16 Cards)", cardBackResId = R.drawable.card_back_red, timeLimitSeconds = 60)
                ),
                onDifficultySelected = {},
                backgroundColor = Color.Black.copy(alpha = 0.6f),
                textColor = Color(0xFFE0E0E0),
                accentColor = Color(0xFFF487B6),
                buttonTextColor = Color.Black
            )
        }
    }
}