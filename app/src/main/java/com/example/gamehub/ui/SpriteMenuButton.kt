package com.example.gamehub.ui

import GameBoxFontFamily
import android.media.MediaPlayer
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gamehub.R
import androidx.compose.material3.Text
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import com.example.gamehub.audio.SoundManager

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SpriteMenuButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    normalRes: Int = R.drawable.menu_button_long,
    pressedRes: Int = R.drawable.menu_button_long_pressed,
    textStyle: TextStyle = TextStyle(
        fontSize = 22.sp,
        fontFamily = GameBoxFontFamily
    )
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val textOffset = if (isPressed) 0.dp else (-5).dp

    val buttonRes = if (isPressed) pressedRes else normalRes

    Box(
        modifier = modifier
            .height(64.dp)
            .widthIn(min = 250.dp)
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    SoundManager.playEffect(context, R.raw.menu_button_press)
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(buttonRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
        Text(
            text = text,
            style = textStyle,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center).offset(y = textOffset),
            color = androidx.compose.ui.graphics.Color(0xFFc08cdc)
        )
    }
}
