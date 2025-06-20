package com.example.gamehub.features.JorisJump.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.gamehub.R
import com.example.gamehub.features.JorisJump.model.PLATFORM_WIDTH_DP
import com.example.gamehub.features.JorisJump.model.PLATFORM_HEIGHT_DP
import com.example.gamehub.features.JorisJump.model.SPRING_VISUAL_WIDTH_FACTOR
import com.example.gamehub.features.JorisJump.model.SPRING_VISUAL_HEIGHT_FACTOR
import com.example.gamehub.features.JorisJump.model.PlatformState

@Composable
fun PlatformSprite(
    platform: PlatformState,
    totalScrollOffsetDp: Float,
    showHitbox: Boolean = false,
    modifier: Modifier = Modifier
) {
    val visualCloudScaleFactor = 4f
    val currentPlatformLogicalWidthDp = PLATFORM_WIDTH_DP
    val currentPlatformLogicalHeightDp = PLATFORM_HEIGHT_DP
    val visualCloudWidthDp = currentPlatformLogicalWidthDp * visualCloudScaleFactor
    val visualCloudHeightDp = currentPlatformLogicalHeightDp * visualCloudScaleFactor
    val visualCloudOffsetX = (currentPlatformLogicalWidthDp - visualCloudWidthDp) / 2f
    val visualCloudOffsetY = (currentPlatformLogicalHeightDp - visualCloudHeightDp) / 2f

    Image(
        painter = painterResource(id = R.drawable.cloud_platform),
        contentDescription = "Cloud Platform",
        modifier = modifier
            .absoluteOffset(
                x = (platform.x + visualCloudOffsetX).dp,
                y = ((platform.y - totalScrollOffsetDp) + visualCloudOffsetY).dp
            )
            .size(visualCloudWidthDp.dp, visualCloudHeightDp.dp)
    )
    if (platform.hasSpring) {
        val springVisualWidth = currentPlatformLogicalWidthDp * SPRING_VISUAL_WIDTH_FACTOR
        val springVisualHeight = currentPlatformLogicalHeightDp * SPRING_VISUAL_HEIGHT_FACTOR
        val springX_onPlatform = platform.x + (currentPlatformLogicalWidthDp / 2f) - (springVisualWidth / 2f)
        val springY_onPlatform = platform.y - springVisualHeight + (currentPlatformLogicalHeightDp * 0.2f)
        Image(
            painter = painterResource(id = R.drawable.spring_mushroom),
            contentDescription = "Spring Mushroom",
            modifier = Modifier
                .absoluteOffset(x = springX_onPlatform.dp, y = (springY_onPlatform - totalScrollOffsetDp).dp)
                .size(springVisualWidth.dp, springVisualHeight.dp)
        )
        if (showHitbox) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.absoluteOffset(x = springX_onPlatform.dp, y = (springY_onPlatform - totalScrollOffsetDp).dp).size(springVisualWidth.dp, springVisualHeight.dp).background(Color.Cyan.copy(alpha = 0.4f))
            )
        }
    }
    if (showHitbox) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.absoluteOffset(x = platform.x.dp, y = (platform.y - totalScrollOffsetDp).dp).size(PLATFORM_WIDTH_DP.dp, PLATFORM_HEIGHT_DP.dp).background(Color.Red.copy(alpha = 0.3f))
        )
    }
} 