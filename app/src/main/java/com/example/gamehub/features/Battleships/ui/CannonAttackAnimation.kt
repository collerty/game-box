package com.example.gamehub.features.battleships.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import com.example.gamehub.features.battleships.model.Cell
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun CannonAttackAnimation(
    boardOffset: Offset,
    cell: Cell,
    cellSizePx: Float,
    isHit: Boolean?,
    onFinished: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }

    val start = remember {
        when ((0..3).random()) {
            0 -> Offset(-100f, Random.nextFloat() * screenHeight)
            1 -> Offset(Random.nextFloat() * screenWidth, -100f)
            2 -> Offset(screenWidth + 100f, Random.nextFloat() * screenHeight)
            else -> Offset(Random.nextFloat() * screenWidth, screenHeight + 100f)
        }
    }

    val end = Offset(
        boardOffset.x + cell.col * cellSizePx - cellSizePx/6,
        boardOffset.y + cell.row * cellSizePx - cellSizePx/4
    )

    // --- 3. Curved trajectory control point ---
    val curveAmount = remember { Random.nextFloat() * (0.6f - 0.3f) + 0.3f }
    val control = Offset(
        (start.x + end.x) / 2 + (end.y - start.y) * curveAmount,
        (start.y + end.y) / 2 - 120 * curveAmount
    )

    // --- 4. Animation stages ---
    var fraction by remember { mutableStateOf(0f) }
    var animStage by remember { mutableStateOf(0) } // 0 = flying, 1 = hit/miss anim, 2 = done
    var hitFrame by remember { mutableStateOf(0) }

    // --- 5. Animate: Ball flies in, then hit/miss sequence ---
    LaunchedEffect(cell) {
        println("Starting animation for $cell")

        // Ball flies
        for (i in 0..30) {
            fraction = i / 30f
            delay(14)
        }
        fraction = 1f
        animStage = 1
        // Play hit/miss sequence
        for (f in 0..4) {
            hitFrame = f
            delay(80)
        }
        animStage = 2
        delay(120)
        onFinished()
    }

    if (animStage < 2) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopStart
        ) {
            // --- Animate cannonball flying along a quadratic Bezier curve ---
            if (animStage == 0) {
                val t = fraction
                val x = (1 - t) * (1 - t) * start.x + 2 * (1 - t) * t * control.x + t * t * end.x
                val y = (1 - t) * (1 - t) * start.y + 2 * (1 - t) * t * control.y + t * t * end.y
                val scale = 1.3f - 0.7f * t
                Image(
                    painter = painterResource(id = com.example.gamehub.R.drawable.cannonball),
                    contentDescription = "Cannonball",
                    modifier = Modifier
                        .graphicsLayer(
                            translationX = x,
                            translationY = y,
                            scaleX = scale,
                            scaleY = scale
                        )
                        .size(72.dp)
                )
            }
            // --- Animate PNG hit/miss sequence ---
            else if (animStage == 1) {
                val centerX = end.x  // adjust for image centering
                val centerY = end.y
                val frameRes = if (isHit == true)
                    hitFrameResId(hitFrame)
                else
                    missFrameResId(hitFrame)
                Image(
                    painter = painterResource(id = frameRes),
                    contentDescription = if (isHit == true) "Hit" else "Miss",
                    modifier = Modifier
                        .graphicsLayer(
                            translationX = centerX,
                            translationY = centerY
                        )
                        .size(42.dp)
                )
            }
        }
    }
}

// Helper: get frame resource for hit and miss
fun hitFrameResId(frame: Int): Int = when (frame) {
    0 -> com.example.gamehub.R.drawable.hit_0
    1 -> com.example.gamehub.R.drawable.hit_1
    2 -> com.example.gamehub.R.drawable.hit_2
    3 -> com.example.gamehub.R.drawable.hit_3
    4 -> com.example.gamehub.R.drawable.hit_4
    else -> com.example.gamehub.R.drawable.hit_4
}
fun missFrameResId(frame: Int): Int = when (frame) {
    0 -> com.example.gamehub.R.drawable.miss_0
    1 -> com.example.gamehub.R.drawable.miss_1
    2 -> com.example.gamehub.R.drawable.miss_2
    3 -> com.example.gamehub.R.drawable.miss_3
    4 -> com.example.gamehub.R.drawable.miss_4
    else -> com.example.gamehub.R.drawable.miss_4
}
