package com.example.gamehub.features.codenames.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gamehub.features.codenames.model.CodenamesCard
import com.example.gamehub.features.codenames.model.CardColor
import com.example.gamehub.ui.theme.ArcadeClassic

@Composable
fun CodenamesBoard(
    cards: List<CodenamesCard>,
    isMaster: Boolean,
    isMasterPhase: Boolean,
    currentTeam: String,
    currentTurn: String,
    winner: String?,
    onCardClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // 5x5 grid
    val gridSize = 5
    val spacing = 4.dp
    BoxWithConstraints(modifier = modifier) {
        val cardWidth = (maxWidth - spacing * (gridSize - 1)) / gridSize
        val cardHeight = (maxHeight - spacing * (gridSize - 1)) / gridSize
        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(gridSize),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            items(cards.size) { index ->
                val card = cards[index]
                val isRevealed = card.isRevealed
                Card(
                    modifier = Modifier
                        .width(cardWidth)
                        .height(cardHeight)
                        .clickable(
                            enabled = !isRevealed && !isMasterPhase && currentTeam == currentTurn && !isMaster && winner == null
                        ) { onCardClick(index) },
                    shape = RoundedCornerShape(6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isMaster -> when (card.color) {
                                CardColor.RED -> Color.Red
                                CardColor.BLUE -> Color.Blue
                                CardColor.NEUTRAL -> Color.Gray
                                CardColor.ASSASSIN -> Color.Black
                                else -> Color.White
                            }
                            isRevealed -> when (card.color) {
                                CardColor.RED -> Color.Red
                                CardColor.BLUE -> Color.Blue
                                CardColor.NEUTRAL -> Color.Gray
                                CardColor.ASSASSIN -> Color.Black
                                else -> Color.White
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
                            text = card.word,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = if (isMaster || isRevealed) Color.White else Color.Black,
                            modifier = Modifier.padding(2.dp),
                            fontFamily = ArcadeClassic
                        )
                    }
                }
            }
        }
    }
} 