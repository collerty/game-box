package com.example.gamehub.features.spaceinvaders.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.example.gamehub.features.spaceinvaders.ui.theme.SpaceInvadersTheme

@Composable
fun GameButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fontSize: TextUnit = SpaceInvadersTheme.FontSizes.normal,
    containerColor: Color = SpaceInvadersTheme.backgroundColor,
    contentColor: Color = SpaceInvadersTheme.greenTextColor
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.padding(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Text(
            text = text,
            color = contentColor,
            fontFamily = SpaceInvadersTheme.gameBoxFont,
            fontSize = fontSize
        )
    }
}

