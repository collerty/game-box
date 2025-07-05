package com.example.gamehub.features.ohpardon.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gamehub.R

@Composable
fun GameControls(
    isMyTurn: Boolean,
    currentDiceRoll: Int?,
    selectedPawnId: Int?,
    pixelFont: FontFamily,
    onRollDice: () -> Unit,
    onMovePawn: () -> Unit,
    onSkipTurn: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isMyTurn) return

    val buttonModifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .border(1.dp, Color.Black)
        .background(Color.White)

    val buttonTextStyle = TextStyle(
        fontFamily = pixelFont,
        fontSize = 16.sp,
        color = Color.Black
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth(0.8f)
    ) {
        // Roll Dice Button
        if (currentDiceRoll == null) {
            Button(
                onClick = onRollDice,
                modifier = buttonModifier,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RectangleShape,
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.dice_icon),
                    contentDescription = "Dice",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Roll Dice", style = buttonTextStyle)
            }
        }

        // Move Selected Pawn
        if (currentDiceRoll != null && selectedPawnId != null) {
            Button(
                onClick = onMovePawn,
                modifier = buttonModifier,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RectangleShape
            ) {
                Text("Move Selected Pawn", style = buttonTextStyle)
            }
        }

        // Skip Turn
        Button(
            onClick = onSkipTurn,
            modifier = buttonModifier,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            shape = RectangleShape
        ) {
            Text("Skip Turn", style = buttonTextStyle)
        }
    }
}

