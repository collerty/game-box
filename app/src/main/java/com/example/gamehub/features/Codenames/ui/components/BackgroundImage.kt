package com.example.gamehub.features.codenames.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

@Composable
fun BackgroundImage(
    resId: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.FillBounds
) {
    Image(
        painter = painterResource(id = resId),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    )
} 