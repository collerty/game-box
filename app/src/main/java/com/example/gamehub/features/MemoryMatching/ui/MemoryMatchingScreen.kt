package com.example.gamehub.features.MemoryMatching.ui

import android.media.MediaPlayer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gamehub.R
import com.example.gamehub.features.MemoryMatching.model.GameDifficulty
import com.example.gamehub.features.MemoryMatching.model.GameScreen
import com.example.gamehub.features.MemoryMatching.model.MemoryCard
import com.example.gamehub.features.MemoryMatching.model.MemoryMatchingViewModel

private val STATS_FONT_SIZE = 26.sp
private val BUTTON_TEXT_SIZE = 18.sp
private val GRID_SPACING = 8.dp

@Composable
fun MemoryMatchingScreen(viewModel: MemoryMatchingViewModel = viewModel()) {
    val gameState by viewModel.gameState.collectAsState()
    val haptic = LocalHapticFeedback.current

    val startGameAction: (GameDifficulty) -> Unit = { difficulty ->
        viewModel.startGame(difficulty)
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    val restartCurrentGameAction: () -> Unit = {
        viewModel.restartGame()
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    val changeDifficultyAction: () -> Unit = {
        viewModel.selectDifficultyScreen()
    }

    LaunchedEffect(gameState.showLoseScreen, gameState.allPairsMatched) {
        if (gameState.showLoseScreen && !gameState.allPairsMatched) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        if (gameState.allPairsMatched) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val cardFrontColor = Color(0xFF9092D8)
    val textColor = Color(0xFFE0E0E0)
    val accentColor = Color(0xFFF487B6)
    val buttonTextColor = Color.Black
    val overlayColor = Color.Black.copy(alpha = 0.90f)
    val difficultySelectionBackgroundColor = Color.Black.copy(alpha = 0.7f)
    val congratsTitleColor = Color(0xFF76F7BF)
    val loseTitleColor = Color(0xFFF44336)
    val detailColor = Color.White
    val panelBackgroundColor = Color.Black.copy(alpha = 0.4f)
    val panelBorderColor = textColor.copy(alpha = 0.6f)
    val buttonBorderColor = textColor.copy(alpha = 0.8f)

    val gameButtonColors = ButtonDefaults.buttonColors(
        containerColor = accentColor,
        contentColor = buttonTextColor
    )
    val gameButtonElevation = ButtonDefaults.buttonElevation(
        defaultElevation = 0.dp,
        pressedElevation = 0.dp,
        disabledElevation = 0.dp
    )
    val gameButtonBorder = BorderStroke(2.dp, buttonBorderColor)
    val gameButtonPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.purple_background),
            contentDescription = "Game Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        when (gameState.currentScreen) {
            GameScreen.DIFFICULTY_SELECTION -> {
                DifficultySelectionContent(
                    difficulties = viewModel.gameDifficulties,
                    onDifficultySelected = { difficulty ->
                        startGameAction(difficulty)
                    },
                    backgroundColor = difficultySelectionBackgroundColor,
                    textColor = textColor,
                    buttonColors = gameButtonColors,
                    buttonBorder = gameButtonBorder,
                    buttonPadding = gameButtonPadding,
                    buttonElevation = gameButtonElevation
                )
            }
            GameScreen.PLAYING -> {
                val difficulty = gameState.currentDifficulty ?: viewModel.gameDifficulties.first()
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
                            .background(panelBackgroundColor, RectangleShape)
                            .border(BorderStroke(1.dp, panelBorderColor), RectangleShape)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Time: ${gameState.timeLeftInSeconds}s",
                            fontSize = STATS_FONT_SIZE,
                            color = textColor,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Flips: ${gameState.attemptCount}",
                            fontSize = STATS_FONT_SIZE,
                            color = textColor,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Mistakes: ${gameState.currentTurnIncorrectAttempts}/${difficulty.maxAttempts}",
                            fontSize = STATS_FONT_SIZE,
                            color = if (gameState.currentTurnIncorrectAttempts >= difficulty.maxAttempts - 1) loseTitleColor else textColor,
                            textAlign = TextAlign.Center
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(difficulty.columns),
                            contentPadding = PaddingValues(GRID_SPACING / 2),
                            horizontalArrangement = Arrangement.spacedBy(GRID_SPACING, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(GRID_SPACING, Alignment.CenterVertically),
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth()
                                .align(Alignment.Center)
                        ) {
                            itemsIndexed(gameState.cards, key = { _, card -> card.id }) { index, card ->
                                GameCardItem(
                                    card = card,
                                    onClick = {
                                        if (!card.isFlipped && !card.isMatched && gameState.flippedCardIndices.size < 2 && !gameState.processingMatch) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.onCardClicked(index)
                                        }
                                    },
                                    cardFrontColor = cardFrontColor,
                                    cardBackImageRes = difficulty.cardBackResId,
                                    processingMatch = gameState.processingMatch,
                                    numFlippedCards = gameState.flippedCardIndices.size,
                                    accentColor = accentColor,
                                    isGameEnded = gameState.allPairsMatched || gameState.showLoseScreen,
                                    baseBorderColor = panelBorderColor.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }

                    if (!gameState.allPairsMatched && !gameState.showLoseScreen) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp, top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = restartCurrentGameAction,
                                shape = RectangleShape,
                                colors = gameButtonColors,
                                border = gameButtonBorder,
                                elevation = gameButtonElevation,
                                contentPadding = gameButtonPadding,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Restart", fontSize = BUTTON_TEXT_SIZE)
                            }
                            Button(
                                onClick = changeDifficultyAction,
                                shape = RectangleShape,
                                colors = gameButtonColors,
                                border = gameButtonBorder,
                                elevation = gameButtonElevation,
                                contentPadding = gameButtonPadding,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Difficulty", fontSize = BUTTON_TEXT_SIZE, textAlign = TextAlign.Center)
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(60.dp))
                    }
                }

                AnimatedVisibility(
                    visible = gameState.allPairsMatched,
                    enter = fadeIn(animationSpec = tween(1000)) + scaleIn(animationSpec = tween(1000)),
                    exit = fadeOut(animationSpec = tween(500)) + scaleOut(animationSpec = tween(500))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(overlayColor)
                            .clickable(enabled = false, onClick = {}),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("YOU WIN!", fontSize = 60.sp, fontWeight = FontWeight.Bold, color = congratsTitleColor, textAlign = TextAlign.Center)
                            Text("Flips: ${gameState.attemptCount}", fontSize = 28.sp, color = detailColor)
                            Text("Time Left: ${gameState.timeLeftInSeconds}s", fontSize = 24.sp, color = detailColor)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = restartCurrentGameAction,
                                shape = RectangleShape, colors = gameButtonColors, border = gameButtonBorder, elevation = gameButtonElevation, contentPadding = gameButtonPadding
                            ) {
                                Text("Play Again", fontSize = BUTTON_TEXT_SIZE)
                            }
                            Button(
                                onClick = changeDifficultyAction,
                                shape = RectangleShape, colors = gameButtonColors, border = gameButtonBorder, elevation = gameButtonElevation, contentPadding = gameButtonPadding
                            ) {
                                Text("Change Difficulty", fontSize = BUTTON_TEXT_SIZE)
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = gameState.showLoseScreen && !gameState.allPairsMatched,
                    enter = fadeIn(animationSpec = tween(1000)) + scaleIn(animationSpec = tween(1000)),
                    exit = fadeOut(animationSpec = tween(500)) + scaleOut(animationSpec = tween(500))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(overlayColor)
                            .clickable(enabled = false, onClick = {}),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(gameState.loseReason ?: "GAME OVER", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = loseTitleColor, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp))
                            Text("Flips: ${gameState.attemptCount}", fontSize = 26.sp, color = detailColor)
                            if (gameState.loseReason == "Time's Up!") {
                                Text("You ran out of time!", fontSize = 22.sp, color = detailColor)
                            } else if (gameState.loseReason == "Too many mistakes!") {
                                Text("Exceeded max mistakes!", fontSize = 22.sp, color = detailColor)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = restartCurrentGameAction,
                                shape = RectangleShape, colors = gameButtonColors, border = gameButtonBorder, elevation = gameButtonElevation, contentPadding = gameButtonPadding
                            ) {
                                Text("Try Again", fontSize = BUTTON_TEXT_SIZE)
                            }
                            Button(
                                onClick = changeDifficultyAction,
                                shape = RectangleShape, colors = gameButtonColors, border = gameButtonBorder, elevation = gameButtonElevation, contentPadding = gameButtonPadding
                            ) {
                                Text("Change Difficulty", fontSize = BUTTON_TEXT_SIZE)
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
    buttonColors: ButtonColors,
    buttonBorder: BorderStroke?,
    buttonPadding: PaddingValues,
    buttonElevation: ButtonElevation?
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Select Difficulty",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 28.dp)
            )
            difficulties.forEach { difficulty ->
                Button(
                    onClick = { onDifficultySelected(difficulty) },
                    modifier = Modifier.fillMaxWidth(0.8f),
                    shape = RectangleShape,
                    colors = buttonColors,
                    border = buttonBorder,
                    elevation = buttonElevation,
                    contentPadding = buttonPadding
                ) {
                    Text(
                        text = difficulty.displayName,
                        fontSize = BUTTON_TEXT_SIZE,
                        textAlign = TextAlign.Center
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
    isGameEnded: Boolean,
    baseBorderColor: Color
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

    val borderColor = if (card.isMatched) {
        accentColor
    } else if (card.isFlipped && animatedRotationY < 90f) {
        accentColor.copy(alpha = 0.8f)
    } else {
        baseBorderColor
    }
    val borderWidth = if (card.isMatched) 3.dp else if (card.isFlipped && animatedRotationY < 90f) 2.dp else 1.dp

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .graphicsLayer {
                rotationY = animatedRotationY
                cameraDistance = 12f * density
            }
            .clickable(onClick = onClick, enabled = clickableEnabled),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardContainerColor
        ),
        shape = RectangleShape,
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (animatedRotationY < 90f) {
                Image(
                    painter = painterResource(id = card.imageRes),
                    contentDescription = "Card Image",
                    modifier = Modifier
                        .fillMaxSize(0.8f)
                        .graphicsLayer { rotationY = 0f },
                    contentScale = ContentScale.Fit
                )
            } else {
                Image(
                    painter = painterResource(id = cardBackImageRes),
                    contentDescription = "Card Back",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationY = 180f },
                    contentScale = ContentScale.Crop
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
        val gameButtonColors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFF487B6),
            contentColor = Color.Black
        )
        val gameButtonElevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        val gameButtonBorder = BorderStroke(2.dp, Color.White.copy(alpha = 0.8f))
        val gameButtonPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
        val previewDifficulties = listOf(
            GameDifficulty(pairs = 6, columns = 3, displayName = "Easy (3x4 - 12 Cards)", cardBackResId = R.drawable.old_card_back, timeLimitSeconds = 50, maxAttempts = 4),
            GameDifficulty(pairs = 8, columns = 4, displayName = "Medium (4x4 - 16 Cards)", cardBackResId = R.drawable.card_back_red, timeLimitSeconds = 60, maxAttempts = 5)
        )
        Box {
            Image(
                painter = painterResource(id = R.drawable.purple_background),
                contentDescription = "Preview Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            DifficultySelectionContent(
                difficulties = previewDifficulties,
                onDifficultySelected = {},
                backgroundColor = Color.Black.copy(alpha = 0.7f),
                textColor = Color(0xFFE0E0E0),
                buttonColors = gameButtonColors,
                buttonBorder = gameButtonBorder,
                buttonPadding = gameButtonPadding,
                buttonElevation = gameButtonElevation
            )
        }
    }
}

