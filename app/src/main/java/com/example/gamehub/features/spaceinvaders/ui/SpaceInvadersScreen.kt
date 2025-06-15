package com.example.gamehub.features.spaceinvaders.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import android.util.Log
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gamehub.features.spaceinvaders.classes.SpaceInvadersViewModel
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.gamehub.R
import com.example.gamehub.features.spaceinvaders.classes.EnemyType
import com.example.gamehub.features.spaceinvaders.classes.GameState
import com.example.gamehub.features.spaceinvaders.classes.SoundManager
import com.example.gamehub.features.spaceinvaders.classes.SpaceInvadersViewModel.EventBus
import com.example.gamehub.features.spaceinvaders.classes.SpaceInvadersViewModel.UiEvent
import com.example.gamehub.features.spaceinvaders.classes.VibrationManager
import com.example.gamehub.navigation.NavRoutes
import kotlinx.coroutines.launch


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SpaceInvadersScreen(
    viewModel: SpaceInvadersViewModel = viewModel(),
    navController: NavController,
    name: String
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val boxWithConstraintsScope = this
        val density = LocalDensity.current
        val screenWidthPx = with(density) { boxWithConstraintsScope.maxWidth.toPx() }
        val screenHeightPx = with(density) { boxWithConstraintsScope.maxHeight.toPx() }



        val engine = viewModel.gameEngine
        val tick by viewModel.tick
        val context = LocalContext.current
        val activity = context as? Activity

        val shooterImage = ImageBitmap.imageResource(id = R.drawable.space_invaders_top)
        val middleImage = ImageBitmap.imageResource(id = R.drawable.space_invaders_middle)
        val bottomImage = ImageBitmap.imageResource(id = R.drawable.space_invaders_bottom)
        val ufoImage = ImageBitmap.imageResource(id = R.drawable.space_invaders_ufo)
        val tiltToggleImage = ImageBitmap.imageResource(id = R.drawable.tilt_icon)
        // Add player image
        val playerImage = ImageBitmap.imageResource(id = R.drawable.space_invaders_player)
        val bunkerImage = ImageBitmap.imageResource(id = R.drawable.space_invaders_bunker)

        val retroFont = FontFamily(Font(R.font.space_invaders, FontWeight.Normal))
        val greenTextColor = Color(0xFF00FF00)


        val soundManager = remember { SoundManager(context) }
        val vibrationManager = remember { VibrationManager(context) }

        LaunchedEffect(Unit) {
            EventBus.uiEvent.collect { event ->
                when (event) {
                    UiEvent.PlayShootSound -> soundManager.playSound("shoot")
                    UiEvent.PlayTakeDamageSound -> soundManager.playSound("take_damage")
                    UiEvent.PlayUFOSound -> soundManager.playSound("ufo")
                    UiEvent.PlayExplodeSound -> soundManager.playSound("explode")
                    UiEvent.Vibrate -> {
                        vibrationManager.vibrate(80)
                    }
                }
            }
        }

        LaunchedEffect(screenWidthPx, screenHeightPx) {
            viewModel.setScreenSize(screenWidthPx, screenHeightPx)
        }

        // Set screen width in engine
        LaunchedEffect(screenWidthPx) {
            engine.screenWidthPx = screenWidthPx
        }

        // Set screen height in engine
        LaunchedEffect(screenHeightPx) {
            engine.screenHeightPx = screenHeightPx
            engine.playerController.setPlayer(
                engine.player.copy(y = screenHeightPx - 100f)
            )
        }

        // Reset player Y position on game restart (tick changes)
        LaunchedEffect(tick) {
            engine.playerController.setPlayer(
                engine.player.copy(y = screenHeightPx - 100f)
            )
        }


        // Lock orientation to landscape
        DisposableEffect(Unit) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            onDispose {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }

        if (engine.gameState == GameState.GAME_OVER) {
            SpaceInvadersGameOverScreen(
                score = engine.score,
                onRestart = {
                    viewModel.submitScore(name, engine.score)
                    viewModel.restartGame(screenWidthPx, screenHeightPx)
                },
                onExit = {
                    viewModel.submitScore(name, engine.score)
                    navController.navigate(NavRoutes.GAMES_LIST)
                }
            )
        } else {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {

                // Top right image button for tilt toggle
                Image(
                    bitmap = tiltToggleImage,
                    contentDescription = if (viewModel.tiltControlEnabled) "Tilt Control ON" else "Tilt Control OFF",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(48.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { viewModel.toggleTiltControl() }
                            )
                        }
                )

                // Top left: Lives and Score
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "Lives: ${engine.playerLives}",
                            color = greenTextColor,
                            fontSize = 16.sp,
                            fontFamily = retroFont,
                        )
                        Text(
                            text = "Score: ${engine.score}",
                            color = greenTextColor,
                            fontSize = 16.sp,
                            fontFamily = retroFont,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Game Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val frame = tick // Do not remove

                    // Draw player as image
                    drawImage(
                        image = playerImage,
                        dstOffset = IntOffset(engine.player.x.toInt(), engine.player.y.toInt()),
                        dstSize = IntSize(100, 30)
                    )

                    // Draw player bullets
                    engine.playerController.playerBullets.forEach { bullet ->
                        if (bullet.isActive) {
                            drawRect(
                                color = Color.White,
                                topLeft = Offset(bullet.x, bullet.y),
                                size = Size(10f, 20f) // width, height of bullet
                            )
                        }
                    }

                    // Draw enemy bullets
                    engine.enemyController.enemyBullets.forEach { bullet ->
                        drawRect(
                            color = Color.Red,
                            topLeft = Offset(bullet.x, bullet.y),
                            size = Size(10f, 20f)
                        )
                    }


                    // Draw enemies as images
                    engine.enemyController.enemies.forEach { row ->
                        row.forEach { enemy ->
                            if (enemy.isAlive) {
                                val image = when (enemy.type) {
                                    EnemyType.SHOOTER -> shooterImage
                                    EnemyType.MIDDLE -> middleImage
                                    EnemyType.BOTTOM -> bottomImage
                                }

                                drawImage(
                                    image = image,
                                    dstOffset = IntOffset(enemy.x.toInt(), enemy.y.toInt()),
                                    dstSize = IntSize(80, 60)
                                )
                            }
                        }
                    }

                    engine.bunkers.forEach { bunker ->
                        val alpha = bunker.health / 3f

                        drawImage(
                            image = bunkerImage,
                            srcOffset = IntOffset.Zero,
                            srcSize = IntSize(bunkerImage.width, bunkerImage.height),
                            dstOffset = IntOffset(bunker.x.toInt(), bunker.y.toInt()),
                            dstSize = IntSize(bunker.width.toInt(), bunker.height.toInt()),
                            alpha = alpha
                        )
                    }


                    // Draw UFO
                    if (engine.enemyController.ufo.isActive) {
                        drawImage(
                            image = ufoImage,
                            dstOffset = IntOffset(
                                engine.enemyController.ufo.x.toInt(),
                                engine.enemyController.ufo.y.toInt()
                            ),
                            dstSize = IntSize(
                                engine.enemyController.ufo.width.toInt(),
                                engine.enemyController.ufo.height.toInt()
                            )
                        )
                    }

                }


                // Overlay control buttons
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 32.dp, bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Left Button
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(greenTextColor.copy(alpha = 0.7f), shape = CircleShape)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        engine.isMovingLeft = true
                                        tryAwaitRelease()
                                        engine.isMovingLeft = false
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("←", color = Color.Black, fontSize = 24.sp)
                    }

                    // Right Button
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(greenTextColor.copy(alpha = 0.7f), shape = CircleShape)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        engine.isMovingRight = true
                                        tryAwaitRelease()
                                        engine.isMovingRight = false
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("→", color = Color.Black, fontSize = 24.sp)
                    }
                }

                // Shoot Button in bottom right corner
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 32.dp, bottom = 32.dp)
                        .size(80.dp)
                        .background(Color.Red.copy(alpha = 0.7f), shape = CircleShape)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    engine.playerController.shootBullet()
                                    viewModel.viewModelScope.launch {
                                        soundManager.playSound("shoot")
                                        vibrationManager.vibrate(50)
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Shoot", color = Color.Black, fontFamily = retroFont, fontSize = 14.sp)
                }
            }
        }
    }
}