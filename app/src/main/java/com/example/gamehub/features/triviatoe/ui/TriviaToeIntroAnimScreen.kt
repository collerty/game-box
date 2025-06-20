import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.gamehub.R
import kotlinx.coroutines.delay
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.runtime.remember
import coil.ImageLoader
import coil.decode.GifDecoder
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

@Composable
fun TriviatoeIntroAnimScreen(
    navController: NavController,
    code: String,
    userName: String
) {
    val context = LocalContext.current
    var animationFinished by remember { mutableStateOf(false) }
    var soundFinished by remember { mutableStateOf(false) }

    // Play sound and detect when done
    LaunchedEffect(Unit) {

        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(VibratorManager::class.java)
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(120)
            }
        }

        val mediaPlayer = MediaPlayer.create(context, R.raw.trivia_start)
        mediaPlayer.setOnCompletionListener {
            it.release()
            soundFinished = true
        }
        mediaPlayer.start()
    }

    // Wait for GIF duration (or the shorter of GIF/sound)
    LaunchedEffect(Unit) {
        delay(2200) // Set this to your new GIF's duration if you re-encode it!
        animationFinished = true
    }

    // Only navigate after **both** finish
    LaunchedEffect(animationFinished, soundFinished) {
        if (animationFinished && soundFinished) {
            navController.navigate("triviatoe/$code/${Uri.encode(userName)}/xo") {
                popUpTo("triviatoe/$code/${Uri.encode(userName)}/intro") { inclusive = true }
            }
        }
    }

    // UI
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background
        Image(
            painter = painterResource(id = R.drawable.triviatoe_bg1),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        // Centered GIF
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val context = LocalContext.current
            val imageLoader = remember {
                ImageLoader.Builder(context)
                    .components {
                        add(GifDecoder.Factory())
                    }
                    .build()
            }

            AsyncImage(
                model = "file:///android_asset/triviatoe_anim.gif",
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth(),   // Use all available width
                contentScale = ContentScale.Fit,
                imageLoader = imageLoader
            )
        }
    }
}
