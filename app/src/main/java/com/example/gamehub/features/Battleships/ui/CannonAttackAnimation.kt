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

    import android.media.MediaPlayer
    import androidx.compose.runtime.Composable
    import androidx.compose.ui.platform.LocalContext

    @Composable
    fun CannonAttackAnimation(
        boardOffset: Offset,
        cell: Cell,
        cellSizePx: Float,
        isHit: Boolean?,
        onFinished: () -> Unit,
        vibrateOnHit: () -> Unit // <<--- ADD THIS PARAM!
    ) {
        LaunchedEffect(boardOffset, cell, cellSizePx) {
            println("DEBUG: CANNON: boardOffset=$boardOffset, cell=$cell, cellSizePx=$cellSizePx")
        }

        val cannonballSizePx = with(LocalDensity.current) { 72.dp.toPx() }
        val explosionSizePx = with(LocalDensity.current) { 42.dp.toPx() }

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

        val correctionY = -cellSizePx // negative = animation will move up by one cell
        val end = Offset(
            boardOffset.x + cell.col * cellSizePx + cellSizePx / 2f,
            boardOffset.y + cell.row * cellSizePx + cellSizePx / 2f + correctionY
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

        var didVibrate by remember { mutableStateOf(false) }

        val cellCenter = Offset(
            boardOffset.x + cell.col * cellSizePx + cellSizePx / 2f,
            boardOffset.y + cell.row * cellSizePx + cellSizePx / 2f
        )

        LaunchedEffect(animStage) {
            // When hit animation starts (i.e. after cannonball flying)
            if (animStage == 1 && isHit == true && !didVibrate) {
                vibrateOnHit()
                didVibrate = true
            }
        }

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
            for (f in 0..5) {
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
                   if (animStage == 0 && fraction == 0f) {
                       playSound(com.example.gamehub.R.raw.cannon)
                   }
                    val t = fraction
                    val x = (1 - t) * (1 - t) * start.x + 2 * (1 - t) * t * control.x + t * t * end.x
                    val y = (1 - t) * (1 - t) * start.y + 2 * (1 - t) * t * control.y + t * t * end.y
                    val scale = 1.3f - 0.7f * t
                    val cannonballSizePx = with(LocalDensity.current) { 72.dp.toPx() }

                    Image(
                        painter = painterResource(id = com.example.gamehub.R.drawable.cannonball),
                        contentDescription = "Cannonball",
                        modifier = Modifier
                            .graphicsLayer(
                                translationX = x - cannonballSizePx / 2,
                                translationY = y - cannonballSizePx / 2,
                                scaleX = scale,
                                scaleY = scale
                            )
                            .size(72.dp)
                    )
                }
                // --- Animate PNG hit/miss sequence ---
                else if (animStage == 1) {
                    if (animStage == 1 && hitFrame == 0) {
                        if (isHit == true) playSound(com.example.gamehub.R.raw.hit) else playSound(com.example.gamehub.R.raw.miss)
                    }
                    val centerX = end.x  // adjust for image centering
                    val centerY = end.y
                    val frameRes = if (isHit == true)
                        hitFrameResId(hitFrame)
                    else
                        missFrameResId(hitFrame)
                    val explosionSizePx = with(LocalDensity.current) { 42.dp.toPx() }

                    Image(
                        painter = painterResource(id = frameRes),
                        contentDescription = if (isHit == true) "Hit" else "Miss",
                        modifier = Modifier
                            .graphicsLayer(
                                translationX = centerX - explosionSizePx / 2,
                                translationY = centerY - explosionSizePx / 2
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
        5 -> com.example.gamehub.R.drawable.miss_5 // 5th frame is the same as 4th
        else -> com.example.gamehub.R.drawable.miss_4
    }

    @Composable
    fun playSound(resId: Int) {
        val context = LocalContext.current
        LaunchedEffect(resId) {
            val mediaPlayer = MediaPlayer.create(context, resId)
            mediaPlayer.setOnCompletionListener { it.release() }
            mediaPlayer.start()
        }
    }
