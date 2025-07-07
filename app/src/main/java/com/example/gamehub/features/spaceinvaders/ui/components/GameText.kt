package com.example.gamehub.features.spaceinvaders.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import com.example.gamehub.features.spaceinvaders.ui.theme.SpaceInvadersTheme

@Composable
fun GameText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = SpaceInvadersTheme.FontSizes.normal,
    color: Color = SpaceInvadersTheme.greenTextColor
) {
    Text(
        text = text,
        color = color,
        fontFamily = SpaceInvadersTheme.gameBoxFont,
        fontSize = fontSize,
        modifier = modifier
    )
}

