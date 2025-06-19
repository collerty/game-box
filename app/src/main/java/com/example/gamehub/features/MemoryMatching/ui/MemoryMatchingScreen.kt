package com.example.gamehub.features.MemoryMatching.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gamehub.R
import com.example.gamehub.features.MemoryMatching.model.GameDifficulty
import com.example.gamehub.features.MemoryMatching.model.GameScreen
import com.example.gamehub.features.MemoryMatching.model.MemoryMatchingViewModel
import com.example.gamehub.features.MemoryMatching.ui.DifficultySelectionContent
import com.example.gamehub.features.MemoryMatching.ui.EndGameOverlay
import com.example.gamehub.features.MemoryMatching.ui.PlayingScreen

@Composable
fun MemoryMatchingScreen(viewModel: MemoryMatchingViewModel = viewModel()) {
    val gameState by viewModel.gameState.collectAsState()
    val haptic = LocalHapticFeedback.current

    val startGameAction: (GameDifficulty) -> Unit = {
        viewModel.startGame(it)
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
        if (gameState.showLoseScreen || gameState.allPairsMatched) {
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
                    onDifficultySelected = startGameAction,
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
                PlayingScreen(
                    gameState = gameState,
                    difficulty = difficulty,
                    onCardClick = { viewModel.onCardClicked(it) },
                    onRestart = restartCurrentGameAction,
                    onChangeDifficulty = changeDifficultyAction,
                    cardFrontColor = cardFrontColor,
                    accentColor = accentColor,
                    panelBackgroundColor = panelBackgroundColor,
                    panelBorderColor = panelBorderColor,
                    textColor = textColor,
                    loseTitleColor = loseTitleColor,
                    gameButtonColors = gameButtonColors,
                    gameButtonBorder = gameButtonBorder,
                    gameButtonElevation = gameButtonElevation,
                    gameButtonPadding = gameButtonPadding
                )

                if (gameState.allPairsMatched || gameState.showLoseScreen) {
                    EndGameOverlay(
                        isWin = gameState.allPairsMatched,
                        loseReason = gameState.loseReason,
                        flips = gameState.attemptCount,
                        timeLeft = gameState.timeLeftInSeconds,
                        onRestart = restartCurrentGameAction,
                        onChangeDifficulty = changeDifficultyAction,
                        overlayColor = overlayColor,
                        winTitleColor = congratsTitleColor,
                        loseTitleColor = loseTitleColor,
                        detailColor = detailColor,
                        gameButtonColors = gameButtonColors,
                        gameButtonBorder = gameButtonBorder,
                        gameButtonElevation = gameButtonElevation,
                        gameButtonPadding = gameButtonPadding
                    )
                }
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

