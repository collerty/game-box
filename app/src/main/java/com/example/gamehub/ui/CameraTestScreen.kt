package com.example.gamehub.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import android.graphics.Bitmap
import androidx.activity.result.contract.ActivityResultContracts

@Composable
fun CameraTestScreen(navController: NavController) {
    val context = LocalContext.current

    // 1) Picture preview launcher
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val picLauncher = rememberLauncherForActivityResult(
        contract  = ActivityResultContracts.TakePicturePreview(),
        onResult  = { bmp -> bitmap = bmp }
    )

    // 2) Permission launcher
    val permLauncher = rememberLauncherForActivityResult(
        contract  = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            picLauncher.launch(null)    // <— note the null here
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
            if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                picLauncher.launch(null)     // <— null again
            } else {
                permLauncher.launch(Manifest.permission.CAMERA)
            }
        }, Modifier.fillMaxWidth()) {
            Text("Take Picture")
        }

        Spacer(Modifier.height(16.dp))

        bitmap?.let { bmp ->
            Image(
                bmp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )
        }

        Spacer(Modifier.height(32.dp))

        Button(onClick = { navController.popBackStack() }, Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}
