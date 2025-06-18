package com.example.gamehub.features.MemoryMatching.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gamehub.features.MemoryMatching.model.GameDifficulty
import com.example.gamehub.features.MemoryMatching.model.MemoryMatchingState

private val STATS_FONT_SIZE = 26.sp
private val BUTTON_TEXT_SIZE = 18.sp
private val GRID_SPACING = 8.dp

@Composable
fun PlayingScreen(
    gameState: MemoryMatchingState,
    difficulty: GameDifficulty,
    onCardClick: (Int) -> Unit,
    onRestart: () -> Unit,
    onChangeDifficulty: () -> Unit,
    cardFrontColor: Color,
    accentColor: Color,
    panelBackgroundColor: Color,
    panelBorderColor: Color,
    textColor: Color,
    loseTitleColor: Color,
    gameButtonColors: ButtonColors,
    gameButtonBorder: BorderStroke?,
    gameButtonElevation: ButtonElevation?,
    gameButtonPadding: PaddingValues
) {
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
                itemsIndexed(items = gameState.cards, key = { _, card -> card.id }) { index, card ->
                    GameCardItem(
                        card = card,
                        onClick = { onCardClick(index) },
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
                    onClick = onRestart,
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
                    onClick = onChangeDifficulty,
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
}
