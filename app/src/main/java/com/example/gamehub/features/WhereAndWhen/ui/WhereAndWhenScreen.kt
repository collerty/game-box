package com.example.gamehub.features.whereandwhen.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.RectF
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay
import kotlin.random.Random
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.gamehub.R // YOUR PROJECT'S R
import android.media.MediaPlayer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings // Import this
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState


// --- Data & Constants ---
val arcadeFontFamily_WhereAndWhen = FontFamily(
    Font(R.font.arcade_classic, FontWeight.Normal)
)

private const val CORRECT_YEAR = 1963
private const val CORRECT_LATITUDE = 32.7790
private const val CORRECT_LONGITUDE = -96.8089
private const val CORRECT_LOCATION_NAME = "Dealey Plaza, Dallas, TX"
private val CURRENT_CHALLENGE_IMAGE_RES_ID = R.drawable.kennedy // Ensure kennedy.png/jpg/webp is in res/drawable

data class GuessResult(
    val guessedYear: Int, val actualYear: Int, val yearDifference: Int,
    val guessedLocation: LatLng?, val actualLocation: LatLng, val actualLocationName: String,
    val distanceInKm: Double?, val yearScore: Int, val locationScore: Int, val totalScore: Int,
    val timeRanOut: Boolean = false
)

private const val MAX_YEAR_SCORE = 100
private const val MAX_LOCATION_SCORE = 100
private const val MAX_YEAR_DIFFERENCE_FOR_POINTS = 30
private const val MAX_DISTANCE_KM_FOR_POINTS = 6000.0
private const val ROUND_TIME_SECONDS = 20


private fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadiusKm = 6371.0; val dLat = Math.toRadians(lat2 - lat1); val dLon = Math.toRadians(lon2 - lon1)
    val radLat1 = Math.toRadians(lat1); val radLat2 = Math.toRadians(lat2)
    val a = Math.sin(dLat/2)*Math.sin(dLat/2) + Math.sin(dLon/2)*Math.sin(dLon/2)*Math.cos(radLat1)*Math.cos(radLat2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)); return earthRadiusKm * c
}

private fun calculateScore(
    guessedYear: Int, actualYear: Int, guessedLocation: LatLng?, actualLocation: LatLng, timeRanOut: Boolean = false
): GuessResult {
    if (timeRanOut) {
        return GuessResult(guessedYear, actualYear, kotlin.math.abs(guessedYear - actualYear), guessedLocation, actualLocation,
            CORRECT_LOCATION_NAME, guessedLocation?.let { calculateDistanceKm(it.latitude, it.longitude, actualLocation.latitude, actualLocation.longitude) },
            0, 0, 0, true)
    }
    val yearDifference = kotlin.math.abs(guessedYear - actualYear)
    val yearScore = if (yearDifference > MAX_YEAR_DIFFERENCE_FOR_POINTS) 0 else (MAX_YEAR_SCORE * (1.0 - (yearDifference.toDouble() / MAX_YEAR_DIFFERENCE_FOR_POINTS))).toInt()
    var locationScore = 0; var distanceInKm: Double? = null
    if (guessedLocation != null) {
        distanceInKm = calculateDistanceKm(guessedLocation.latitude, guessedLocation.longitude, actualLocation.latitude, actualLocation.longitude)
        locationScore = if (distanceInKm > MAX_DISTANCE_KM_FOR_POINTS) 0 else (MAX_LOCATION_SCORE * (1.0 - (distanceInKm / MAX_DISTANCE_KM_FOR_POINTS))).toInt()
    }
    return GuessResult(guessedYear, actualYear, yearDifference, guessedLocation, actualLocation, CORRECT_LOCATION_NAME, distanceInKm,
        yearScore.coerceIn(0,MAX_YEAR_SCORE), locationScore.coerceIn(0,MAX_LOCATION_SCORE), (yearScore + locationScore).coerceIn(0, MAX_YEAR_SCORE + MAX_LOCATION_SCORE))
}

@SuppressLint("UnusedBoxWithConstraintsScope") // Added to suppress warning for BoxWithConstraints if not using its constraints directly
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhereAndWhenScreen() {
    val context = LocalContext.current; val view = LocalView.current
    DisposableEffect(Unit) { /* Immersive Mode */
        val window = (view.context as? Activity)?.window; val wc = window?.let { WindowInsetsControllerCompat(it, view) }
        if (window != null && wc != null) { WindowCompat.setDecorFitsSystemWindows(window, false); wc.hide(WindowInsetsCompat.Type.systemBars()); wc.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE }
        onDispose { if (window != null && wc != null) { WindowCompat.setDecorFitsSystemWindows(window, true); wc.show(WindowInsetsCompat.Type.systemBars()) } }
    }

    var timeUpSoundPlayer by remember { mutableStateOf<MediaPlayer?>(null) } // New sound player

    DisposableEffect(Unit) { // Sounds
        try {

            timeUpSoundPlayer = MediaPlayer.create(context, R.raw.times_up_sound) // Your new sound
        } catch (e: Exception) {
            Log.e("WhereAndWhen_Sound", "Error creating MediaPlayers", e)
        }
        onDispose {
            timeUpSoundPlayer?.release(); timeUpSoundPlayer = null
            Log.d("WhereAndWhen_Sound", "MediaPlayers released")
        }
    }

    fun playSound(player: MediaPlayer?) {
        try {
            player?.let {
                if (it.isPlaying) { it.stop(); it.prepare() }
                it.start()
            }
        } catch (e: Exception) { Log.e("WhereAndWhen_Sound", "Error playing sound", e) }
    }

    var selectedYear by remember { mutableStateOf(1950f) }; val minYear = 1860f; val maxYear = 2025f
    var selectedLatLng by remember { mutableStateOf<LatLng?>(null) }
    val cameraPositionState = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(LatLng(20.0, 0.0), 2f) }
    var mapReady by remember { mutableStateOf(false) }
    var showResultsDialog by remember { mutableStateOf(false) }; var currentGuessResult by remember { mutableStateOf<GuessResult?>(null) }
    val currentChallengeImageRes by remember { mutableStateOf(CURRENT_CHALLENGE_IMAGE_RES_ID) }

    var timeLeftInSeconds by remember { mutableStateOf(ROUND_TIME_SECONDS) }
    var roundTimerActive by remember { mutableStateOf(false) }
    var gameRunning by remember { mutableStateOf(true) } // Used to control overall game flow, including timer pauses

    fun performGameReset() {
        Log.d("WhereAndWhen_Game", "Performing Game Reset / Next Round Setup")
        selectedLatLng = null; selectedYear = (minYear + maxYear) / 2f
        currentGuessResult = null; showResultsDialog = false
        timeLeftInSeconds = ROUND_TIME_SECONDS
        roundTimerActive = true
        gameRunning = true
        cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(20.0, 0.0), 2f)
        Log.d("WhereAndWhen_Game", "Game Reset. Timer started for $timeLeftInSeconds s.")
    }

    var initialSetupDone by remember { mutableStateOf(false) }
    BoxWithConstraints {
        if (!initialSetupDone && maxWidth > 0.dp) {
            performGameReset()
            initialSetupDone = true
        }
    }

    LaunchedEffect(roundTimerActive, gameRunning, initialSetupDone) {
        if (!roundTimerActive || !gameRunning || !initialSetupDone) {
            Log.d("WhereAndWhen_Timer", "Timer launch effect skipped. Active:$roundTimerActive, Running:$gameRunning, Init:$initialSetupDone")
            return@LaunchedEffect
        }
        Log.d("WhereAndWhen_Timer", "Timer countdown initiated for $timeLeftInSeconds seconds.")
        var currentTimerValue = timeLeftInSeconds
        while (currentTimerValue > 0 && roundTimerActive && gameRunning) {
            delay(1000)
            if (!roundTimerActive || !gameRunning) break
            currentTimerValue--
            timeLeftInSeconds = currentTimerValue
        }
        if (currentTimerValue == 0 && roundTimerActive && gameRunning) {
            Log.d("WhereAndWhen_Timer", "Time's UP!")
            playSound(timeUpSoundPlayer) // <<< PLAY TIME'S UP SOUND
            currentGuessResult = calculateScore(
                guessedYear = selectedYear.toInt(), actualYear = CORRECT_YEAR,
                guessedLocation = selectedLatLng, actualLocation = LatLng(CORRECT_LATITUDE, CORRECT_LONGITUDE),
                timeRanOut = true
            )
            showResultsDialog = true
            roundTimerActive = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("                                    🌍 Where & When ⏳", fontFamily = arcadeFontFamily_WhereAndWhen) },
                actions = {
                    if (initialSetupDone && gameRunning && !showResultsDialog && roundTimerActive) {
                        Text(text = "$timeLeftInSeconds", style = TextStyle(fontFamily = arcadeFontFamily_WhereAndWhen, fontSize = 24.sp,
                            color = if (timeLeftInSeconds <= 5 && timeLeftInSeconds % 2 == 0) Color.Red else Color.White,
                            fontWeight = FontWeight.Bold), modifier = Modifier.padding(end = 16.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black.copy(alpha = 0.2f), titleContentColor = Color.White, actionIconContentColor = Color.White)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues).fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.DarkGray.copy(alpha=0.2f), RoundedCornerShape(12.dp)).padding(8.dp), contentAlignment = Alignment.Center) {
                Image(painter = painterResource(id = currentChallengeImageRes), contentDescription = "Historical Image", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Guess the Year: ${selectedYear.toInt()}", style = TextStyle(fontFamily = arcadeFontFamily_WhereAndWhen, fontSize = 24.sp))
                Spacer(modifier = Modifier.height(8.dp))
                Slider(value = selectedYear, onValueChange = { selectedYear = it }, valueRange = minYear..maxYear, steps = (maxYear-minYear).toInt()-1, modifier = Modifier.fillMaxWidth(0.9f), enabled = gameRunning && !showResultsDialog && roundTimerActive)
                Row(modifier = Modifier.fillMaxWidth(0.9f), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(minYear.toInt().toString(), style = TextStyle(fontFamily = arcadeFontFamily_WhereAndWhen, fontSize = 12.sp)); Text(maxYear.toInt().toString(), style = TextStyle(fontFamily = arcadeFontFamily_WhereAndWhen, fontSize = 12.sp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.weight(1.5f).fillMaxWidth().background(Color.Black.copy(alpha = 0.1f), RoundedCornerShape(12.dp))) {
                GoogleMap(modifier = Modifier.fillMaxSize(), cameraPositionState = cameraPositionState, onMapLoaded = { mapReady = true },
                    uiSettings = MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false),
                    onMapClick = { latLng -> if (gameRunning && !showResultsDialog && roundTimerActive) selectedLatLng = latLng }
                ) { selectedLatLng?.let { Marker(state = rememberMarkerState(position = it), title = "Your Guess", snippet = "Lat: ${"%.2f".format(it.latitude)}, Lng: ${"%.2f".format(it.longitude)}") } }
                if (!mapReady) { Box(modifier = Modifier.fillMaxSize().background(Color.Gray.copy(alpha=0.7f)), contentAlignment=Alignment.Center){ Column(horizontalAlignment=Alignment.CenterHorizontally){CircularProgressIndicator();Spacer(Modifier.height(8.dp));Text("Loading Map...",color=Color.White)}} }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    if (!gameRunning || !roundTimerActive) return@Button // Prevent guess if timer already stopped or game over
                    roundTimerActive = false
                    currentGuessResult = calculateScore(selectedYear.toInt(), CORRECT_YEAR, selectedLatLng, LatLng(CORRECT_LATITUDE, CORRECT_LONGITUDE))
                    showResultsDialog = true
                },
                modifier = Modifier.fillMaxWidth(0.7f).height(56.dp), shape = RoundedCornerShape(12.dp),
                enabled = selectedLatLng != null && gameRunning && !showResultsDialog && roundTimerActive
            ) { Text("GUESS!", fontSize = 20.sp, fontFamily = arcadeFontFamily_WhereAndWhen, fontWeight = FontWeight.Bold) }
        }

        if (showResultsDialog && currentGuessResult != null) {
            val result = currentGuessResult!!
            AlertDialog(
                onDismissRequest = { showResultsDialog = false; performGameReset() },
                title = { Text("Results", fontFamily = arcadeFontFamily_WhereAndWhen, fontSize = 28.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        if (result.timeRanOut) { Text("TIME'S UP!", style = TextStyle(fontFamily = arcadeFontFamily_WhereAndWhen, fontSize = 24.sp, color = Color.Red, fontWeight = FontWeight.Bold)); Spacer(modifier = Modifier.height(8.dp)) }
                        Text("THE EVENT: JFK Assassination", fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center); Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Your Year:", fontWeight = FontWeight.SemiBold); Text("${result.guessedYear}") }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Actual Year:", fontWeight = FontWeight.SemiBold); Text("${result.actualYear}") }
                        Text("Off by ${result.yearDifference} year(s).", style = MaterialTheme.typography.bodyMedium)
                        Text("Year Score: ${result.yearScore}/$MAX_YEAR_SCORE", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold); Spacer(modifier = Modifier.height(16.dp))
                        Text("Your Location Guess:", fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        if (result.guessedLocation != null && result.distanceInKm != null) { Text("${"%.0f".format(result.distanceInKm)} km away from", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center) }
                        else if (!result.timeRanOut) { Text("No location selected.", style = MaterialTheme.typography.bodyMedium) }
                        Text(result.actualLocationName, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                        Text("Location Score: ${result.locationScore}/$MAX_LOCATION_SCORE", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold); Spacer(modifier = Modifier.height(24.dp))
                        Text("Total Score: ${result.totalScore}/${MAX_YEAR_SCORE + MAX_LOCATION_SCORE}", style = TextStyle(fontFamily = arcadeFontFamily_WhereAndWhen, fontSize = 24.sp, fontWeight = FontWeight.Bold))
                    }
                },
                confirmButton = { Button(onClick = { showResultsDialog = false; performGameReset() }) { Text("Next Image", fontFamily = arcadeFontFamily_WhereAndWhen) } }
            )
        }
    }
}

@Preview(showBackground = true, device = "id:pixel_5")
@Composable
fun WhereAndWhenScreenPreview() {
    MaterialTheme { WhereAndWhenScreen() }
}

