import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.ImageLoader
import coil.decode.GifDecoder
import com.example.gamehub.R
import com.example.gamehub.navigation.NavRoutes
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import com.example.gamehub.ui.SpriteMenuButton
@Composable
fun MainMenu(navController: NavController) {
    val activity = (LocalContext.current as? Activity)
    val context = LocalContext.current

    // Ensure GIFs animate (important!)
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                add(GifDecoder.Factory())
            }
            .build()
    }

    LaunchedEffect(Unit) {
        context.stopService(Intent(context, com.example.gamehub.MusicService::class.java))
    }
    LaunchedEffect(Unit) {
        if (FirebaseAuth.getInstance().currentUser == null) {
            FirebaseAuth.getInstance().signInAnonymously()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background image
        Image(
            painter = painterResource(id = R.drawable.game_box_bg1),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(50.dp)) // <-- push the logo and buttons down

            // Looping GIF logo
            AsyncImage(
                model = "file:///android_asset/game_box_logo.gif",
                contentDescription = null,
                modifier = Modifier
                    .size(220.dp) // adjust as needed
                    .align(Alignment.CenterHorizontally),
                imageLoader = imageLoader
            )

            Spacer(Modifier.height(60.dp)) // <-- more space between logo and buttons

            // Buttons
            SpriteMenuButton(
                text = "Play",
                onClick = { navController.navigate(NavRoutes.GAMES_LIST) }
            )
            Spacer(Modifier.height(16.dp))
            SpriteMenuButton(
                text = "Settings",
                onClick = { navController.navigate(NavRoutes.SETTINGS) }
            )
            Spacer(Modifier.height(16.dp))
            SpriteMenuButton(
                text = "Test Sensors",
                onClick = { navController.navigate(NavRoutes.TEST_SENSORS) }
            )
            Spacer(Modifier.height(16.dp))
            SpriteMenuButton(
                text = "Exit",
                onClick = { activity?.finish() }
            )
        }
    }
}
