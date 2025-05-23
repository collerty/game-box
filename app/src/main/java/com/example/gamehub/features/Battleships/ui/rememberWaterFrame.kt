package com.example.gamehub.features.battleships.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Animates an integer index [0..totalFrames-1] at the specified fps.
 *
 * @param spriteRes     Unused here, kept so the API matches your call site.
 * @param framesPerRow  Unused here.
 * @param totalFrames   Total number of frames in the animation.
 * @param fps           Desired frames-per-second for playback.
 *
 * @return the current frame index [0..totalFrames-1], looping forever.
 */
@Composable
fun rememberWaterFrame(
    @DrawableRes spriteRes: Int,
    framesPerRow: Int = 8,
    totalFrames: Int = 16,
    fps: Int = 16
): Int {
    // ms per frame
    val perFrameMs = remember(fps) { 1000 / fps }
    // total loop duration = frames × ms/frame
    val loopDurationMs = remember(perFrameMs, totalFrames) {
        perFrameMs * totalFrames
    }

    val transition = rememberInfiniteTransition()
    return transition.animateValue(
        initialValue  = 0,
        targetValue   = totalFrames - 1,
        typeConverter = Int.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = loopDurationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    ).value
}
