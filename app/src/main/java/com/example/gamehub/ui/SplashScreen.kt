package com.example.gamehub.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.ImageLoader
import coil.decode.GifDecoder
import com.example.gamehub.R
import kotlinx.coroutines.delay
import android.media.MediaPlayer
import androidx.compose.runtime.DisposableEffect
import com.example.gamehub.audio.SoundManager

@Composable
fun SplashScreen(navController: NavController) {
    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                add(GifDecoder.Factory())
            }
            .build()
    }

    // --- AUDIO LOGIC ---
    // Hold both media players in remember so we can clean them up
    val arcadePlayer = remember { MediaPlayer.create(context, R.raw.game_start) }
    val boxPlayer = remember { MediaPlayer.create(context, R.raw.game_box) }
    val glitchPlayer = remember { MediaPlayer.create(context, R.raw.glitch) }
    val paperPlayer = remember { MediaPlayer.create(context, R.raw.paper_flying) }

    // Play both sounds in sequence
    LaunchedEffect(Unit) {
        SoundManager.playEffect(context, R.raw.glitch)
        delay(500)
        SoundManager.playEffect(context, R.raw.game_start)
        delay(1000)
        SoundManager.playEffect(context, R.raw.game_box)
        delay(1500)
        SoundManager.playEffect(context, R.raw.paper_flying)
    }

    // Ensure MediaPlayers are released when the composable leaves composition
    DisposableEffect(Unit) {
        onDispose {
            arcadePlayer.release()
            boxPlayer.release()
        }
    }

    // Play for ~1.5 seconds (adjust to match your GIF)
    LaunchedEffect(Unit) {
        delay(4600)
        navController.navigate("mainMenu") {
            popUpTo("splash") { inclusive = true }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background
        Image(
            painter = painterResource(id = R.drawable.game_box_bg1),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop // or FillBounds for "stretch"
        )
        // Centered animated GIF
        AsyncImage(
            model = "file:///android_asset/game_box.gif",
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .size(320.dp),
            imageLoader = imageLoader
        )
    }
}
