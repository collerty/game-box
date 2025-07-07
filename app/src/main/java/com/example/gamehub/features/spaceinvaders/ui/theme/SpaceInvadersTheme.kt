package com.example.gamehub.features.spaceinvaders.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.gamehub.R

object SpaceInvadersTheme {
    val gameBoxFont = FontFamily(Font(R.font.gamebox_font, FontWeight.Bold))
    val greenTextColor = Color(0xFF00FF00)
    val backgroundColor = Color.Black

    object FontSizes {
        val large = 48.sp
        val medium = 32.sp
        val normal = 24.sp
        val small = 22.sp
        val extraLarge = 26.sp
    }
}

