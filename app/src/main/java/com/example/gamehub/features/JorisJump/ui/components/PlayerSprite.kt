package com.example.gamehub.features.JorisJump.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.gamehub.R
import com.example.gamehub.features.JorisJump.model.PLAYER_WIDTH_DP
import com.example.gamehub.features.JorisJump.model.PLAYER_HEIGHT_DP
import androidx.compose.ui.layout.ContentScale


@Composable
fun PlayerSprite(x: Float, y: Float, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.joris_doodler),
        contentDescription = "Joris the Doodler",
        modifier = modifier
            .absoluteOffset(x = x.dp, y = y.dp)
            .size(PLAYER_WIDTH_DP.dp, PLAYER_HEIGHT_DP.dp),
        contentScale = ContentScale.Fit
    )
} 