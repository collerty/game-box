package com.example.gamehub.features.ScreamOSaur.ui.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.example.gamehub.features.ScreamOSaur.model.GameState
import com.example.gamehub.features.ScreamOSaur.model.ScreamOSaurUiState

@Composable
fun GameCanvas(
    state: ScreamOSaurUiState,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawGround(state, Color(0xFF808080), Color(0xFF606060))
            drawObstacles(state, Color(0xFF808080), Color(0xFF606060))
        }

        Dinosaur(state)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGround(
    state: ScreamOSaurUiState,
    groundColor: Color,
    groundDetailColor: Color
) {
    val groundTopYCanvas = size.height - state.groundHeightPx
    drawRect(
        color = groundColor,
        topLeft = Offset(0f, groundTopYCanvas),
        size = Size(size.width, state.groundHeightPx)
    )
    val numGroundPatches = 30
    for (i in 0..numGroundPatches) {
        val patchY = groundTopYCanvas + (kotlin.random.Random.nextFloat() * (state.groundHeightPx - 4.dp.toPx()) + 2.dp.toPx())
        val patchX = kotlin.random.Random.nextFloat() * size.width
        val patchWidth = kotlin.random.Random.nextFloat() * 20.dp.toPx() + 10.dp.toPx()
        val patchHeight = kotlin.random.Random.nextFloat() * 2.dp.toPx() + 1.dp.toPx()
        drawRect(
            color = groundDetailColor.copy(alpha = kotlin.random.Random.nextFloat() * 0.3f + 0.2f),
            topLeft = Offset(patchX, patchY),
            size = Size(patchWidth, patchHeight)
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawObstacles(
    state: ScreamOSaurUiState,
    cactusColor: Color,
    cactusDarkerColor: Color
) {
    state.obstacles.forEach { obstacle ->
        val obX = obstacle.xPosition
        val obY = state.gameHeightPx - state.groundHeightPx - obstacle.height
        val obW = obstacle.width
        val obH = obstacle.height
        val bodyW = obW * 0.4f
        val bodyH = obH
        val bodyX = obX + (obW - bodyW) / 2
        val bodyBaseY = obY
        drawRoundRect(color = cactusColor, topLeft = Offset(bodyX, bodyBaseY), size = Size(bodyW, bodyH), cornerRadius = androidx.compose.ui.geometry.CornerRadius(bodyW * 0.25f))
        val armBaseHeight = bodyH * 0.4f
        val armBaseWidth = bodyW * 0.8f
        if (obH > 30.dp.toPx()) {
            val armAttachY = bodyBaseY + bodyH * 0.2f
            val horizontalSegmentW = armBaseWidth * 0.5f
            val horizontalSegmentH = armBaseHeight * 0.3f
            val verticalSegmentW = armBaseWidth * 0.3f
            val verticalSegmentH = armBaseHeight * 0.8f
            val rArmHorizX = bodyX + bodyW - horizontalSegmentW * 0.2f
            drawRoundRect(color = cactusColor, topLeft = Offset(rArmHorizX, armAttachY), size = Size(horizontalSegmentW, horizontalSegmentH), cornerRadius = androidx.compose.ui.geometry.CornerRadius(horizontalSegmentH * 0.5f))
            val rArmVertX = rArmHorizX + horizontalSegmentW - verticalSegmentW * 0.5f
            val rArmVertY = armAttachY - verticalSegmentH + horizontalSegmentH
            drawRoundRect(color = cactusColor, topLeft = Offset(rArmVertX, rArmVertY), size = Size(verticalSegmentW, verticalSegmentH), cornerRadius = androidx.compose.ui.geometry.CornerRadius(verticalSegmentW * 0.5f))
        }
        if (obH > 40.dp.toPx() && obW > 20.dp.toPx()) {
            val armAttachY = bodyBaseY + bodyH * 0.45f
            val horizontalSegmentW = armBaseWidth * 0.5f
            val horizontalSegmentH = armBaseHeight * 0.3f
            val verticalSegmentW = armBaseWidth * 0.3f
            val verticalSegmentH = armBaseHeight * 0.8f
            val lArmHorizX = bodyX - horizontalSegmentW + horizontalSegmentW * 0.2f
            drawRoundRect(color = cactusColor, topLeft = Offset(lArmHorizX, armAttachY), size = Size(horizontalSegmentW, horizontalSegmentH), cornerRadius = androidx.compose.ui.geometry.CornerRadius(horizontalSegmentH * 0.5f))
            val lArmVertX = lArmHorizX + horizontalSegmentW * 0.5f - verticalSegmentW * 0.5f
            val lArmVertY = armAttachY - verticalSegmentH + horizontalSegmentH
            drawRoundRect(color = cactusColor, topLeft = Offset(lArmVertX, lArmVertY), size = Size(verticalSegmentW, verticalSegmentH), cornerRadius = androidx.compose.ui.geometry.CornerRadius(verticalSegmentW * 0.5f))
        }
        val numLines = (bodyW / 6.dp.toPx()).toInt().coerceIn(2, 5)
        if (numLines > 1) {
            for (i in 1 until numLines) {
                val lineX = bodyX + (bodyW / numLines) * i
                drawLine(color = cactusDarkerColor, start = Offset(lineX, bodyBaseY + bodyH * 0.05f), end = Offset(lineX, bodyBaseY + bodyH * 0.95f), strokeWidth = 1.dp.toPx())
            }
        }
    }
}

@Composable
private fun Dinosaur(state: ScreamOSaurUiState) {
    val density = LocalDensity.current
    val verticalOffset = 8.dp
    val currentDinoTopPx = state.dinoTopYOnGroundPx - (state.jumpAnimValue * state.jumpMagnitudePx)
    val currentDinoTopYDp = with(density) { currentDinoTopPx.toDp() } + verticalOffset
    val dinosaurSizeDp = with(density) { state.dinosaurSizePx.toDp() }
    val dinosaurVisualXOffsetDp = with(density) { state.dinosaurVisualXPositionPx.toDp() }

    Box(
        modifier = Modifier
            .size(dinosaurSizeDp)
            .offset(x = dinosaurVisualXOffsetDp, y = currentDinoTopYDp)
    ) {
        val imageLoader = coil.ImageLoader.Builder(LocalContext.current)
            .components {
                add(ImageDecoderDecoder.Factory())
            }
            .build()

        val painter = rememberAsyncImagePainter(
            model = ImageRequest.Builder(LocalContext.current)
                .data("file:///android_asset/dino_model.gif")
                .diskCacheKey(state.gameState.name)
                .build(),
            imageLoader = imageLoader,
            onState = { painterState ->
                if (painterState is coil.compose.AsyncImagePainter.State.Success) {
                    val drawable = painterState.result.drawable
                    if (drawable is android.graphics.drawable.Animatable) {
                        if (state.gameState == GameState.PLAYING) {
                            drawable.start()
                        } else {
                            drawable.stop()
                        }
                    }
                }
            }
        )
        Image(
            painter = painter,
            contentDescription = "Dinosaur",
            modifier = Modifier.fillMaxSize()
        )
    }
}

