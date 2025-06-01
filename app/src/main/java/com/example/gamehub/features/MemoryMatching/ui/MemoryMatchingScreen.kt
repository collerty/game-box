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
// import androidx.compose.foundation.layout.Row // Not used if only one button
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
// import androidx.compose.foundation.layout.width // Not strictly needed if only one button
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
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
fun MemoryMatchingScreen() {
    val imageResources = remember {
        listOf(
            R.drawable.basketball, R.drawable.bee, R.drawable.dice, R.drawable.herosword,
            R.drawable.ladybug, R.drawable.ramen, R.drawable.taxi, R.drawable.zombie
        )
    }

    fun generateNewCards() = (imageResources + imageResources)
        .mapIndexed { index, resId -> MemoryCard(id = index, imageRes = resId) }
        .shuffled()
        .toMutableStateList()

    var cards by remember { mutableStateOf(generateNewCards()) }
    var flippedCardIndices by remember { mutableStateOf<List<Int>>(emptyList()) }
    var processingMatch by remember { mutableStateOf(false) }
    var attemptCount by remember { mutableIntStateOf(0) }
    var allPairsMatched by remember { mutableStateOf(false) }

    // Color Palette
    val backgroundColor = Color(0xFF2C2A4A)
    val cardBackColor = Color(0xFF4F518C)
    val cardFrontColor = Color(0xFF9092D8)
    val textColor = Color(0xFFE0E0E0)
    val accentColor = Color(0xFFF487B6)
    val buttonTextColor = Color.Black
    val congratsOverlayColor = Color.Black.copy(alpha = 0.90f)
    val congratsTitleColor = Color(0xFF76F7BF)
    val congratsDetailColor = Color.White

    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    val cardFlipSoundPlayer = remember {
        MediaPlayer.create(context, R.raw.card_flip).apply {
            setVolume(0.3f, 0.3f) // Set volume to 30%
        }
    }

    val winSoundPlayer = remember {
        MediaPlayer.create(context, R.raw.card_flip__win).apply {
            setVolume(0.6f, 0.6f) // Set volume to 60%
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cardFlipSoundPlayer.release()
            winSoundPlayer.release()
        }
    }

    fun playCardFlipSound() {
        if (cardFlipSoundPlayer.isPlaying) {
            cardFlipSoundPlayer.seekTo(0)
        } else {
            cardFlipSoundPlayer.start()
        }
    }

    fun playWinSound() {
        if (winSoundPlayer.isPlaying) {
            winSoundPlayer.seekTo(0)
        } else {
            winSoundPlayer.start()
        }
    }


    fun resetGame() {
        cards = generateNewCards()
        flippedCardIndices = emptyList()
        attemptCount = 0
        allPairsMatched = false
        processingMatch = false
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        // Stop win sound if it's playing from a previous game
        if (winSoundPlayer.isPlaying) {
            winSoundPlayer.stop()
            winSoundPlayer.prepare() // Prepare for next play
        }
    }

    LaunchedEffect(cards, flippedCardIndices) {
        if (flippedCardIndices.size == 2 && !processingMatch) {
            attemptCount++
            processingMatch = true
            val firstIndex = flippedCardIndices[0]
            val secondIndex = flippedCardIndices[1]

            val card1 = cards[firstIndex]
            val card2 = cards[secondIndex]

            if (card1.imageRes == card2.imageRes) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) // Vibration for a match
                val newCards = cards.toMutableStateList()
                newCards[firstIndex] = card1.copy(isMatched = true, isFlipped = true)
                newCards[secondIndex] = card2.copy(isMatched = true, isFlipped = true)
                cards = newCards

                if (cards.all { it.isMatched }) {
                    allPairsMatched = true
                    playWinSound() // Play win sound
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress) // Stronger vibration for winning
                }
                flippedCardIndices = emptyList()
                processingMatch = false
            } else {
                delay(1000)
                val newCards = cards.toMutableStateList()
                newCards[firstIndex] = card1.copy(isFlipped = false)
                newCards[secondIndex] = card2.copy(isFlipped = false)
                cards = newCards
                flippedCardIndices = emptyList()
                processingMatch = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Attempts: $attemptCount",
                fontSize = 28.sp,
                fontFamily = arcadeFontFamily,
                color = textColor,
                modifier = Modifier.padding(bottom = 20.dp)
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
            ) {
                itemsIndexed(cards, key = { _, card -> card.id }) { index, itemCard ->
                    GameCardItem(
                        card = itemCard,
                        onClick = {
                            if (!processingMatch && !itemCard.isFlipped && !itemCard.isMatched && flippedCardIndices.size < 2) {
                                val newCards = cards.toMutableStateList()
                                newCards[index] = itemCard.copy(isFlipped = true)
                                cards = newCards
                                flippedCardIndices = flippedCardIndices + index
                                playCardFlipSound()
                            }
                        },
                        cardBackColor = cardBackColor,
                        cardFrontColor = cardFrontColor,
                        processingMatch = processingMatch,
                        numFlippedCards = flippedCardIndices.size,
                        accentColor = accentColor
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = allPairsMatched,
            enter = fadeIn(animationSpec = tween(1000)) + scaleIn(animationSpec = tween(1000)),
            exit = fadeOut(animationSpec = tween(500)) + scaleOut(animationSpec = tween(500))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(congratsOverlayColor)
                    .clickable(enabled = false) { /* Consume clicks */ },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        text = "CONGRATULATIONS!",
                        fontSize = 36.sp,
                        fontFamily = arcadeFontFamily,
                        color = congratsTitleColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "You matched all pairs in $attemptCount attempts!",
                        fontSize = 22.sp,
                        fontFamily = arcadeFontFamily,
                        color = congratsDetailColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(36.dp))
                    Button(
                        onClick = { resetGame() },
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = buttonTextColor),
                        modifier = Modifier
                            .height(60.dp)
                            .fillMaxWidth(0.7f)
                            .border(2.dp, textColor, RectangleShape)
                    ) {
                        Text("Replay", fontSize = 24.sp, fontFamily = arcadeFontFamily)
                    }
                }
            }
        }
    }
}

@Composable
fun GameCardItem(
    card: MemoryCard,
    onClick: () -> Unit,
    cardBackColor: Color,
    cardFrontColor: Color,
    processingMatch: Boolean,
    numFlippedCards: Int,
    accentColor: Color
) {
    val animatedRotationY by animateFloatAsState(
        targetValue = if (card.isFlipped || card.isMatched) 0f else 180f,
        animationSpec = tween(durationMillis = 500),
        label = "cardRotation"
    )

    val clickableEnabled = !processingMatch &&
            !card.isMatched &&
            (!card.isFlipped || numFlippedCards < 1) &&
            numFlippedCards < 2

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .graphicsLayer {
                rotationY = animatedRotationY
                cameraDistance = 12f * density
            }
            .clickable(onClick = onClick, enabled = clickableEnabled && !card.isMatched)
            .border(
                width = 2.dp,
                color = if (animatedRotationY < 90f && (card.isFlipped || card.isMatched)) accentColor else Color.Transparent,
                shape = RectangleShape
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp, pressedElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (animatedRotationY < 90f) cardFrontColor else cardBackColor
        ),
        shape = RectangleShape
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (animatedRotationY < 90f) { // Front of card is visible
                Image(
                    painter = painterResource(id = card.imageRes),
                    contentDescription = "Card Image",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (card.isMatched) 6.dp else 8.dp), // Slightly less padding for matched cards to show border
                    contentScale = ContentScale.Fit
                )
            } else { // Back of card is visible
                Image(
                    painter = painterResource(id = R.drawable.question_mark),
                    contentDescription = "Card Back",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentScale = ContentScale.Fit
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