package com.example.gamehub.features.battleships.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

/**
 * Renders one frame of an 8×2 water spritesheet at the given size.
 */
@Composable
fun AnimatedWaterCell(
    frame: Int,
    @DrawableRes spriteRes: Int,
    framesPerRow: Int = 8,
    totalFrames: Int = 16,
    size: Dp
) {
    // load the full sheet
    val bitmap = ImageBitmap.imageResource(id = spriteRes)
    val frameW = bitmap.width  / framesPerRow
    val frameH = bitmap.height / (totalFrames / framesPerRow)

    Canvas(Modifier.size(size)) {
        val srcX = (frame % framesPerRow) * frameW
        val srcY = (frame / framesPerRow) * frameH
        drawImage(
            image     = bitmap,
            srcOffset = IntOffset(srcX, srcY),
            srcSize   = IntSize(frameW, frameH),
            dstSize   = IntSize(size.toPx().roundToInt(), size.toPx().roundToInt())
        )
    }
}
