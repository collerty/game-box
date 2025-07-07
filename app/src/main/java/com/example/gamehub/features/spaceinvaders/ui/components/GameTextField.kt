package com.example.gamehub.features.spaceinvaders.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.example.gamehub.features.spaceinvaders.ui.theme.SpaceInvadersTheme

@Composable
fun GameTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = {
            Text(
                label,
                color = SpaceInvadersTheme.greenTextColor,
                fontFamily = SpaceInvadersTheme.gameBoxFont,
                fontSize = SpaceInvadersTheme.FontSizes.small
            )
        },
        textStyle = TextStyle(
            color = SpaceInvadersTheme.greenTextColor,
            fontFamily = SpaceInvadersTheme.gameBoxFont,
            fontSize = SpaceInvadersTheme.FontSizes.small
        ),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = SpaceInvadersTheme.backgroundColor,
            unfocusedContainerColor = SpaceInvadersTheme.backgroundColor,
            focusedLabelColor = SpaceInvadersTheme.greenTextColor,
            unfocusedLabelColor = SpaceInvadersTheme.greenTextColor,
            focusedIndicatorColor = SpaceInvadersTheme.greenTextColor,
            unfocusedIndicatorColor = SpaceInvadersTheme.greenTextColor,
            cursorColor = SpaceInvadersTheme.greenTextColor
        )
    )
}

