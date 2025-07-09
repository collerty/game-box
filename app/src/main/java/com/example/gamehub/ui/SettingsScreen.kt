package com.example.gamehub.ui

import com.example.gamehub.ui.GameBoxFontFamily
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gamehub.R
import com.example.gamehub.ui.components.NinePatchBorder

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SettingsScreen(
    navController: androidx.navigation.NavController? = null,
    viewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val musicVolume by viewModel.musicVolume.observeAsState(0.5f)
    val soundVolume by viewModel.soundVolume.observeAsState(0.5f)

    Box(modifier = Modifier.fillMaxSize()) {
        // Background
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

            // Settings image at top
            Image(
                painter = painterResource(id = R.drawable.settings), // your settings icon
                contentDescription = "Settings",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .height(90.dp)
            )

            Spacer(Modifier.height(42.dp))

            // Bordered box for sliders and buttons
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight()
            ) {
                NinePatchBorder(
                    modifier = Modifier.matchParentSize(),
                    drawableRes = R.drawable.game_list_border // use your border resource
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Music Volume",
                        fontFamily = GameBoxFontFamily,
                        color = androidx.compose.ui.graphics.Color(0xFFc08cdc),
                        fontSize = 20.sp
                    )
                    Spacer(Modifier.height(14.dp))
                    // Double size slider
                    PixelSlider(
                        value = musicVolume,
                        onValueChange = { viewModel.setMusicVolume(it) },
                        valueRange = 0f..1f,
                        modifier = Modifier
                            .fillMaxWidth(0.96f)
                            .height(38.dp) // double the height
                    )
                    Spacer(Modifier.height(12.dp))
                    // Double size mute/unmute
                    SpriteMenuButton(
                        text = if (musicVolume == 0f) "Unmute" else "Mute",
                        onClick = { viewModel.toggleMuteMusic(musicVolume) },
                        modifier = Modifier
                            .fillMaxWidth(0.82f)
                            .height(70.dp), // double the height
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = GameBoxFontFamily,
                            fontSize = 24.sp // larger font
                        )
                    )

                    Spacer(Modifier.height(36.dp))

                    Text(
                        "Sound Effects Volume",
                        fontFamily = GameBoxFontFamily,
                        color = androidx.compose.ui.graphics.Color(0xFFc08cdc),
                        fontSize = 20.sp
                    )
                    Spacer(Modifier.height(14.dp))
                    PixelSlider(
                        value = soundVolume,
                        onValueChange = { viewModel.setSoundVolume(it) },
                        valueRange = 0f..1f,
                        modifier = Modifier
                            .fillMaxWidth(0.96f)
                            .height(38.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    SpriteMenuButton(
                        text = if (soundVolume == 0f) "Unmute" else "Mute",
                        onClick = { viewModel.toggleMuteSound(soundVolume) },
                        modifier = Modifier
                            .fillMaxWidth(0.82f)
                            .height(70.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = GameBoxFontFamily,
                            fontSize = 24.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            SpriteMenuButton(
                text = "Back",
                onClick = { navController?.popBackStack() },
                modifier = Modifier
                    .fillMaxWidth(0.95f),
                normalRes = R.drawable.menu_button_long,
                pressedRes = R.drawable.menu_button_long_pressed
            )

            Spacer(Modifier.height(20.dp))
        }
    }
}
