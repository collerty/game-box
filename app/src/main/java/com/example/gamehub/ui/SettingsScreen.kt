package com.example.gamehub.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState


@Composable
fun SettingsScreen(viewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val musicVolume by viewModel.musicVolume.observeAsState(0.5f)
    val soundVolume by viewModel.soundVolume.observeAsState(0.5f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Music Volume")
        Slider(
            value = musicVolume,
            onValueChange = { viewModel.setMusicVolume(it) },
            valueRange = 0f..1f
        )

        Text("Sound Effects Volume")
        Slider(
            value = soundVolume,
            onValueChange = { viewModel.setSoundVolume(it) },
            valueRange = 0f..1f
        )
    }
}

