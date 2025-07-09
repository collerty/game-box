package com.example.gamehub.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.gamehub.R
import com.example.gamehub.ui.GameBoxFontFamily
import com.example.gamehub.ui.components.NinePatchBorder

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MicrophoneTestScreen(navController: NavController) {
    val context = LocalContext.current

    var audioUri by remember { mutableStateOf<Uri?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    val recordLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        audioUri = result.data?.data
    }

    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val intent = Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION)
            recordLauncher.launch(intent)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.game_box_bg1),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))

            // Microphone image at top
            Image(
                painter = painterResource(id = R.drawable.microphone),
                contentDescription = "Microphone Test",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .height(90.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Bordered area for record/play, now centered vertically
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight()
            ) {
                NinePatchBorder(
                    modifier = Modifier.matchParentSize(),
                    drawableRes = R.drawable.game_list_border
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Record Button
                    SpriteMenuButton(
                        text = "Record Audio",
                        onClick = {
                            mediaPlayer?.let {
                                if (it.isPlaying) it.stop()
                                it.release()
                                mediaPlayer = null
                            }
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                                == PackageManager.PERMISSION_GRANTED) {
                                val intent = Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION)
                                recordLauncher.launch(intent)
                            } else {
                                permLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(0.88f),
                        normalRes = R.drawable.menu_button_long,
                        pressedRes = R.drawable.menu_button_long_pressed
                    )

                    Spacer(Modifier.height(16.dp))

                    // Play button if audio recorded
                    audioUri?.let { uri ->
                        SpriteMenuButton(
                            text = "Play Recording",
                            onClick = {
                                mediaPlayer?.let {
                                    if (it.isPlaying) it.stop()
                                    it.release()
                                }
                                val mp = MediaPlayer().apply {
                                    setDataSource(context, uri)
                                    prepare()
                                    start()
                                    setOnCompletionListener {
                                        it.release()
                                        mediaPlayer = null
                                    }
                                }
                                mediaPlayer = mp
                            },
                            modifier = Modifier.fillMaxWidth(0.88f),
                            normalRes = R.drawable.menu_button_long,
                            pressedRes = R.drawable.menu_button_long_pressed,
                            textStyle = TextStyle(fontSize = 15.sp, fontFamily = GameBoxFontFamily)
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            SpriteMenuButton(
                text = "Back",
                onClick = {
                    mediaPlayer?.let {
                        if (it.isPlaying) it.stop()
                        it.release()
                        mediaPlayer = null
                    }
                    navController.popBackStack()
                },
                modifier = Modifier
                    .fillMaxWidth(0.95f),
                normalRes = R.drawable.menu_button_long,
                pressedRes = R.drawable.menu_button_long_pressed
            )

            Spacer(Modifier.height(20.dp))
        }
    }
}

