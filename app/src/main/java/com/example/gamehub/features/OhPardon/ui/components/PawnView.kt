package com.example.gamehub.features.ohpardon.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gamehub.R
import com.example.gamehub.features.ohpardon.ui.PawnForUI

@Composable
fun PawnView(
    pawn: PawnForUI,
    cellSize: Dp,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    val pawnModifier = if (isSelected) {
        modifier
            .size(cellSize)
            .shadow(8.dp, CircleShape, spotColor = Color(0xFFF57C00))
            .background(Color.LightGray, shape = CircleShape)
            .border(4.dp, Color(0xFFF57C00), shape = CircleShape)
    } else {
        modifier
            .size(cellSize)
            .background(Color.LightGray, shape = CircleShape)
            .border(1.dp, Color.DarkGray, shape = CircleShape)
    }

    Box(
        modifier = pawnModifier,
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = getPawnImageRes(pawn.color)),
            contentDescription = "Pawn",
            modifier = Modifier.size(cellSize * 0.85f)
        )
    }
}

@Composable
private fun getPawnImageRes(color: Color?): Int {
    return when (color) {
        Color.Red -> R.drawable.pawn_red
        Color.Blue -> R.drawable.pawn_blue
        Color.Green -> R.drawable.pawn_green
        Color.Yellow -> R.drawable.pawn_yellow
        else -> R.drawable.pawn_default
    }
}

