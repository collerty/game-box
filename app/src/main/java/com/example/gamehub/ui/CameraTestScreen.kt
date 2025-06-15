package com.example.gamehub.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.gamehub.R
import com.example.gamehub.ui.components.NinePatchBorder

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CameraTestScreen(navController: NavController) {
    val context = LocalContext.current

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val picLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp -> bitmap = bmp }

    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            picLauncher.launch(null)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Pixel-art background
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

            // Camera image at top (replace with your actual resource!)
            Image(
                painter = painterResource(id = R.drawable.camera),
                contentDescription = "Camera Test",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .height(90.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Central bordered box for "Take Picture" and preview
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight()
            ) {
                NinePatchBorder(
                    modifier = Modifier.matchParentSize(),
                    drawableRes = R.drawable.game_list_border // use your real border resource
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SpriteMenuButton(
                        text = "Take Picture",
                        onClick = {
                            if (ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                picLauncher.launch(null)
                            } else {
                                permLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(0.88f),
                        normalRes = R.drawable.menu_button_long,
                        pressedRes = R.drawable.menu_button_long_pressed
                    )

                    Spacer(Modifier.height(16.dp))

                    bitmap?.let { bmp ->
                        Image(
                            bmp.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            SpriteMenuButton(
                text = "Back",
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .fillMaxWidth(0.95f),
                normalRes = R.drawable.menu_button_long,
                pressedRes = R.drawable.menu_button_long_pressed
            )

            Spacer(Modifier.height(20.dp))
        }
    }
}
