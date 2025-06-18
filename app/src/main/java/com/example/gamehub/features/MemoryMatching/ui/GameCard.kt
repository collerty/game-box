package com.example.gamehub.features.MemoryMatching.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.gamehub.features.MemoryMatching.model.MemoryCard

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

    val borderColor = when {
        card.isMatched -> accentColor
        card.isFlipped && animatedRotationY < 90f -> accentColor.copy(alpha = 0.8f)
        else -> baseBorderColor
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

