package com.example.gamehub.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import android.provider.MediaStore
import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts


@Composable
fun MicrophoneTestScreen(navController: NavController) {
    val context = LocalContext.current

    // 1) Record sound launcher
    var audioUri by remember { mutableStateOf<Uri?>(null) }
    val recordLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        audioUri = result.data?.data
    }

    // 2) RECORD_AUDIO permission launcher
    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val intent = Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION)
            recordLauncher.launch(intent)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement   = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
                val intent = Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION)
                recordLauncher.launch(intent)
            } else {
                permLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }, Modifier.fillMaxWidth()) {
            Text("Record Audio")
        }

        Spacer(Modifier.height(16.dp))

        audioUri?.let { uri ->
            Button(onClick = {
                MediaPlayer().apply {
                    setDataSource(context, uri)
                    prepare()
                    start()
                }
            }, Modifier.fillMaxWidth()) {
                Text("Play Recording")
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(onClick = { navController.popBackStack() }, Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}
