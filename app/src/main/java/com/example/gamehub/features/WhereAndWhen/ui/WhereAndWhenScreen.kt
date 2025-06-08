package com.example.gamehub.features.whereandwhen.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.media.MediaPlayer
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import com.example.gamehub.R // YOUR PROJECT'S R
import com.example.gamehub.features.whereandwhen.model.WWPlayerGuess
import com.example.gamehub.features.whereandwhen.model.WWPlayerRoundResult
import com.example.gamehub.features.whereandwhen.model.WWRoundResultsContainer
import com.example.gamehub.features.whereandwhen.model.WhereAndWhenGameState
import com.example.gamehub.navigation.NavRoutes
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.ktx.Firebase
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.UUID
import kotlin.math.abs
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

// --- Font ---
val arcadeFontFamily_WhereAndWhen = FontFamily(
    Font(R.font.arcade_classic, FontWeight.Normal)
)

// --- Data Classes (Challenge) ---
data class Challenge(
    val id: String,
    val imageResId: Int,
    val correctYear: Int,
    val correctLatitude: Double,
    val correctLongitude: Double,
    val correctLocationName: String,
    val eventName: String
)

// --- Constants ---
private const val MAX_YEAR_SCORE = 1000
private const val MAX_LOCATION_SCORE = 1000
private const val MAX_YEAR_DIFFERENCE_FOR_POINTS = 35
private const val MAX_DISTANCE_KM_FOR_POINTS = 5000.0
private const val ROUND_TIME_SECONDS = 35
private const val MIN_SLIDER_YEAR = 1850f
private const val MAX_SLIDER_YEAR = 2024f

// Hardcoded list of challenges
val gameChallenges = listOf(
    Challenge("jfk", R.drawable.kennedy_assassination, 1963, 32.7790, -96.8089, "Dealey Plaza, Dallas, TX, USA", "Assassination of JFK"),
    Challenge("moon", R.drawable.moon_landing_1969, 1969, 28.5721, -80.6480, "Tranquility Base, Moon", "Apollo 11 Moon Landing"),
    Challenge("berlinwall", R.drawable.berlin_wall_fall_1989, 1989, 52.5160, 13.3777, "Brandenburg Gate, Berlin, Germany", "Fall of the Berlin Wall"),
    Challenge("titanic", R.drawable.titanic_sinking_1912, 1912, 41.726931, -49.948253, "North Atlantic Ocean (Titanic Wreck)", "Sinking of the Titanic"),
    Challenge("wright", R.drawable.wright_brothers_flight_1903, 1903, 36.0156, -75.6674, "Kitty Hawk, North Carolina, USA", "Wright Brothers' First Flight"),
    Challenge("vday", R.drawable.vj_day_kiss_1945, 1945, 40.7580, -73.9855, "Times Square, New York, USA", "V-J Day Kiss in Times Square"),
    Challenge("mandela", R.drawable.mandela_release_1990, 1990, -33.9258, 18.4232, "Cape Town, South Africa", "Nelson Mandela's Release"),
    Challenge("obama", R.drawable.obama_inauguration_2009, 2009, 38.8895, -77.0352, "Capitol Hill, Washington D.C., USA", "Barack Obama's Inauguration"),
    Challenge("9_11", R.drawable.attack_september_11_2001, 2001, 40.7115, -74.0134, "World Trade Center, New York, USA", "September 11 Attacks"),
    Challenge("hiroshima", R.drawable.hiroshima_bombing_1945, 1945, 34.3853, 132.4553, "Hiroshima, Japan", "Atomic Bombing of Hiroshima"),
    Challenge("pearlharbor", R.drawable.pearl_harbor_attack_1941, 1941, 21.3667, -157.9333, "Pearl Harbor, Hawaii, USA", "Attack on Pearl Harbor"),
    Challenge("dday", R.drawable.d_day_landing_1944, 1944, 49.3389, -0.6217, "Omaha Beach, Normandy, France", "D-Day Normandy Landings"),
    Challenge("fall_soviet", R.drawable.soviet_union_dissolution_1991, 1991, 55.7558, 37.6173, "Moscow, Russia", "Dissolution of the Soviet Union"),
    Challenge("great_depression", R.drawable.wall_street_crash_1929, 1929, 40.7069, -74.0113, "Wall Street, New York, USA", "Wall Street Crash of 1929")
)

private val TOTAL_ROUNDS = 5


// --- Helper Functions ---
private fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadiusKm = 6371.0; val dLat = Math.toRadians(lat2 - lat1); val dLon = Math.toRadians(lon2 - lon1)
    val radLat1 = Math.toRadians(lat1); val radLat2 = Math.toRadians(lat2)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(radLat1) * Math.cos(radLat2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)); return earthRadiusKm * c
}

private fun calculatePlayerScoreForRound(playerGuess: WWPlayerGuess?, challenge: Challenge, timeRanOutForThisPlayer: Boolean): WWPlayerRoundResult {
    val actualYear = challenge.correctYear
    val actualLocation = LatLng(challenge.correctLatitude, challenge.correctLongitude)
    val guessedYearValue = playerGuess?.year ?: actualYear
    val guessedLatLngValue = if (playerGuess?.lat != null && playerGuess.lng != null) LatLng(playerGuess.lat, playerGuess.lng) else null

    if (timeRanOutForThisPlayer || playerGuess == null || (!playerGuess.submitted && !timeRanOutForThisPlayer)) {
        return WWPlayerRoundResult(guessedYearValue, 0, guessedLatLngValue?.latitude, guessedLatLngValue?.longitude, guessedLatLngValue?.let { calculateDistanceKm(it.latitude, it.longitude, actualLocation.latitude, actualLocation.longitude) }, 0, 0, true)
    }
    val yearDifference = abs(guessedYearValue - actualYear)
    val yearScore = if (yearDifference > MAX_YEAR_DIFFERENCE_FOR_POINTS) 0 else (MAX_YEAR_SCORE * (1.0 - (yearDifference.toDouble() / MAX_YEAR_DIFFERENCE_FOR_POINTS))).toInt()
    var locationScore = 0; var distanceInKm: Double? = null
    if (guessedLatLngValue != null) {
        distanceInKm = calculateDistanceKm(guessedLatLngValue.latitude, guessedLatLngValue.longitude, actualLocation.latitude, actualLocation.longitude)
        locationScore = if (distanceInKm > MAX_DISTANCE_KM_FOR_POINTS) 0 else (MAX_LOCATION_SCORE * (1.0 - (distanceInKm / MAX_DISTANCE_KM_FOR_POINTS))).toInt()
    }
    return WWPlayerRoundResult(guessedYearValue, yearScore.coerceIn(0, MAX_YEAR_SCORE), guessedLatLngValue?.latitude, guessedLatLngValue?.longitude, distanceInKm, locationScore.coerceIn(0, MAX_LOCATION_SCORE), (yearScore + locationScore).coerceIn(0, MAX_YEAR_SCORE + MAX_LOCATION_SCORE), false)
}

@SuppressLint("UnusedBoxWithConstraintsScope", "CoroutineCreationDuringComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhereAndWhenScreen(navController: NavController, roomCode: String, currentUserName: String) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val db = FirebaseFirestore.getInstance()
    val myPlayerId = Firebase.auth.currentUser?.uid ?: UUID.randomUUID().toString()

    var wwGameState by remember { mutableStateOf<WhereAndWhenGameState?>(null) }
    var roomDocSnapshot by remember { mutableStateOf<Map<String, Any>?>(null) } // Store full room doc for hostUid

    val roomPlayers = remember(roomDocSnapshot) {
        @Suppress("UNCHECKED_CAST")
        roomDocSnapshot?.get("players") as? List<Map<String, Any>> ?: emptyList()
    }
    val amIHost = remember(roomDocSnapshot, myPlayerId) {
        roomDocSnapshot?.get("hostUid") == myPlayerId
    }


    var selectedYear by remember { mutableStateOf((MIN_SLIDER_YEAR + MAX_SLIDER_YEAR) / 2f) }
    var selectedLatLng by remember { mutableStateOf<LatLng?>(null) }
    val markerState = rememberMarkerState()
    val cameraPositionState = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(LatLng(20.0, 0.0), 1.5f) }
    var mapReady by remember { mutableStateOf(false) }
    var hasSubmittedGuess by remember { mutableStateOf(false) }

    var timeLeftInSeconds by remember { mutableStateOf(ROUND_TIME_SECONDS) }
    var showRoundResultsDialog by remember { mutableStateOf(false) }
    var showFinalResultsDialog by remember { mutableStateOf(false) }
    var timeUpSoundPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    val currentChallenge = remember(wwGameState?.currentChallengeId) {
        gameChallenges.find { it.id == wwGameState?.currentChallengeId }
    }

    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window; val wc = window?.let { WindowInsetsControllerCompat(it, view) }
        if (window != null && wc != null) { WindowCompat.setDecorFitsSystemWindows(window, false); wc.hide(WindowInsetsCompat.Type.systemBars()); wc.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE }
        onDispose { if (window != null && wc != null) { WindowCompat.setDecorFitsSystemWindows(window, true); wc.show(WindowInsetsCompat.Type.systemBars()) } }
    }
    DisposableEffect(Unit) {
        try { timeUpSoundPlayer = MediaPlayer.create(context, R.raw.times_up_sound) }
        catch (e: Exception) { Log.e("WW_Sound", "Error MediaPlayer", e) }
        onDispose { timeUpSoundPlayer?.release(); timeUpSoundPlayer = null }
    }
    fun playSound(player: MediaPlayer?) {
        try { player?.let { if (it.isPlaying) { it.stop(); it.prepare() }; it.start() } }
        catch (e: Exception) { Log.e("WW_Sound", "Error playing sound", e) }
    }

    DisposableEffect(roomCode, lifecycleOwner) {
        var roomListenerReg: ListenerRegistration? = null
        val roomDocRef = db.collection("rooms").document(roomCode)

        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                roomListenerReg = roomDocRef.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("WW_Firestore", "Room listen error", error)
                        // Optionally, navigate away on persistent error
                        // navController.popBackStack() // Or to a general error screen
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        // --- Existing logic for when snapshot exists ---
                        roomDocSnapshot = snapshot.data // Store the whole room document data
                        val rawGameState = snapshot.get("gameState.whereandwhen") as? Map<String, Any>
                        if (rawGameState != null) {
                            try {
                                val newState = WhereAndWhenGameState(
                                    currentRoundIndex = (rawGameState["currentRoundIndex"] as? Long)?.toInt() ?: 0,
                                    currentChallengeId = rawGameState["currentChallengeId"] as? String ?: "",
                                    roundStartTimeMillis = rawGameState["roundStartTimeMillis"] as? Long ?: 0L,
                                    roundStatus = rawGameState["roundStatus"] as? String ?: WhereAndWhenGameState.STATUS_GUESSING,
                                    playerGuesses = (rawGameState["playerGuesses"] as? Map<String, Map<String, Any>>)?.mapValues { entry -> WWPlayerGuess( year = (entry.value["year"] as? Long)?.toInt(), lat = entry.value["lat"] as? Double, lng = entry.value["lng"] as? Double, submitted = entry.value["submitted"] as? Boolean ?: false, timeTakenMs = entry.value["timeTakenMs"] as? Long ) } ?: emptyMap(),
                                    roundResults = (rawGameState["roundResults"] as? Map<String, Any>)?.let { rrMap -> WWRoundResultsContainer( challengeId = rrMap["challengeId"] as? String ?: "", results = (rrMap["results"] as? Map<String, Map<String, Any>>)?.mapValues { entry -> WWPlayerRoundResult( guessedYear = (entry.value["guessedYear"] as? Long)?.toInt() ?: 0, yearScore = (entry.value["yearScore"] as? Long)?.toInt() ?: 0, guessedLat = entry.value["guessedLat"] as? Double, guessedLng = entry.value["guessedLng"] as? Double, distanceKm = entry.value["distanceKm"] as? Double, locationScore = (entry.value["locationScore"] as? Long)?.toInt() ?: 0, roundScore = (entry.value["roundScore"] as? Long)?.toInt() ?: 0, timeRanOut = entry.value["timeRanOut"] as? Boolean ?: false ) } ?: emptyMap() ) } ?: WWRoundResultsContainer(),
                                    playersReadyForNextRound = rawGameState["playersReadyForNextRound"] as? Map<String, Boolean> ?: emptyMap(),
                                    challengeOrder = rawGameState["challengeOrder"] as? List<String> ?: emptyList()
                                )

                                val previousChallengeId = wwGameState?.currentChallengeId
                                val previousRoundStatus = wwGameState?.roundStatus
                                wwGameState = newState

                                val isNewRoundStartCondition = (previousRoundStatus != WhereAndWhenGameState.STATUS_GUESSING && newState.roundStatus == WhereAndWhenGameState.STATUS_GUESSING) ||
                                        (previousChallengeId != newState.currentChallengeId && newState.roundStatus == WhereAndWhenGameState.STATUS_GUESSING)

                                if (isNewRoundStartCondition) {
                                    Log.d("WW_UI_Reset", "New round detected ($previousChallengeId -> ${newState.currentChallengeId}, $previousRoundStatus -> ${newState.roundStatus}). Resetting local UI for $myPlayerId.")
                                    selectedYear = (MIN_SLIDER_YEAR + MAX_SLIDER_YEAR) / 2f
                                    selectedLatLng = null; markerState.position = LatLng(0.0,0.0); markerState.hideInfoWindow()
                                    hasSubmittedGuess = false
                                    cameraPositionState.move(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(LatLng(20.0, 0.0), 1.5f)))
                                }
                                showRoundResultsDialog = newState.roundStatus == WhereAndWhenGameState.STATUS_RESULTS
                            } catch (e: Exception) { Log.e("WW_Firestore", "Error parsing game state", e) }
                        } else { Log.w("WW_Firestore", "gameState.whereandwhen is null or not a map") }

                        if (snapshot.getString("status") == "ended" && !amIHost) { // Check for amIHost here
                            // If game ended and I am not the host, it means the host might have triggered the end.
                            // Show final results for guests too if the host has ended the game.
                            // The host's exit button would have already deleted the room if they were the one ending.
                            // This ensures guests see final scores if host ends game normally *before* deleting.
                            // If host *deletes* room abruptly, the `else` block below handles it.
                            showFinalResultsDialog = true
                        }

                    } else {
                        // --- THIS IS THE NEW/MODIFIED PART ---
                        // Room document does not exist (e.g., host deleted it or network issue confirmed deletion)
                        Log.w("WW_Firestore", "Room document $roomCode does not exist or was deleted.")
                        if (!amIHost) { // Only guests should be auto-navigated
                            Toast.makeText(context, "Host closed the room.", Toast.LENGTH_LONG).show()
                            // Navigate back to the lobby menu
                            navController.navigate(NavRoutes.LOBBY_MENU.replace("{gameId}", "whereandwhen")) {
                                popUpTo(NavRoutes.MAIN_MENU) { inclusive = false }
                                launchSingleTop = true
                            }
                        } else {
                            // If I am the host and the room doesn't exist, it means I already deleted it.
                            // This can happen if navigation from final results dialog is slower than listener.
                            // No action needed here for the host as they initiated the deletion/navigation.
                            Log.d("WW_Firestore", "Host: Room already deleted by me. Listener confirming.")
                        }
                        // For safety, clear local state that might depend on the room
                        wwGameState = null
                        roomDocSnapshot = null
                        // (context as? Activity)?.finish() // DO NOT finish the activity directly for guests
                    }
                }
            }
            override fun onStop(owner: LifecycleOwner) { roomListenerReg?.remove() }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer); roomListenerReg?.remove() }
    }

    LaunchedEffect(wwGameState?.roundStartTimeMillis, wwGameState?.roundStatus, wwGameState?.currentRoundIndex, hasSubmittedGuess) {
        val startTime = wwGameState?.roundStartTimeMillis ?: 0L
        if (hasSubmittedGuess || startTime == 0L || wwGameState?.roundStatus != WhereAndWhenGameState.STATUS_GUESSING) {
            if (wwGameState?.roundStatus != WhereAndWhenGameState.STATUS_GUESSING && !hasSubmittedGuess) { timeLeftInSeconds = 0 }
            else if (wwGameState?.roundStatus != WhereAndWhenGameState.STATUS_GUESSING) { timeLeftInSeconds = ROUND_TIME_SECONDS }
            return@LaunchedEffect
        }
        Log.d("WW_Timer", "Local timer active for round ${wwGameState?.currentRoundIndex}. MyPlayerId: $myPlayerId. Submitted: $hasSubmittedGuess. StartTime: $startTime")
        while (wwGameState?.roundStatus == WhereAndWhenGameState.STATUS_GUESSING && !hasSubmittedGuess && isActive) {
            val elapsedMillis = System.currentTimeMillis() - startTime
            val currentRemaining = ROUND_TIME_SECONDS - (elapsedMillis / 1000).toInt()
            timeLeftInSeconds = currentRemaining.coerceAtLeast(0)
            if (timeLeftInSeconds == 0) {
                if (!hasSubmittedGuess) {
                    playSound(timeUpSoundPlayer)
                    Log.d("WW_Timer", "Local timer ran out for player $myPlayerId. Auto-submitting.")
                    hasSubmittedGuess = true // Lock UI locally first
                    val guessToSubmit = WWPlayerGuess(selectedYear.toInt(), selectedLatLng?.latitude, selectedLatLng?.longitude, true)
                    db.collection("rooms").document(roomCode).update("gameState.whereandwhen.playerGuesses.$myPlayerId", guessToSubmit)
                        .addOnSuccessListener { Log.d("WW_Submit", "Auto-submitted guess for $myPlayerId due to timeout (timer).") }
                        .addOnFailureListener { e -> Log.e("WW_Submit", "Error auto-submitting guess (timer)", e); hasSubmittedGuess = false }
                }
                break
            }
            delay(500)
        }
    }

    LaunchedEffect(wwGameState?.playerGuesses, roomPlayers, amIHost, wwGameState?.roundStatus, wwGameState?.roundStartTimeMillis) {
        if (!amIHost || wwGameState?.roundStatus != WhereAndWhenGameState.STATUS_GUESSING) return@LaunchedEffect
        val activePlayerIds = roomPlayers.mapNotNull { it["uid"] as? String }; if (activePlayerIds.isEmpty()) return@LaunchedEffect
        val guesses = wwGameState?.playerGuesses ?: emptyMap()
        val allHaveSubmitted = activePlayerIds.all { guesses[it]?.submitted == true }
        val roundTimeMillis = wwGameState?.roundStartTimeMillis ?: 0L
        val isTimeUpGlobally = roundTimeMillis > 0 && (System.currentTimeMillis() - roundTimeMillis) >= (ROUND_TIME_SECONDS * 1000 + 2000)

        if (allHaveSubmitted || isTimeUpGlobally) {
            Log.d("WW_Host", "Condition met for round end: allSubmitted=$allHaveSubmitted, isTimeUpGlobally=$isTimeUpGlobally. Current Round: ${wwGameState?.currentRoundIndex}")
            val currentChallengeForCalc = gameChallenges.find { it.id == wwGameState?.currentChallengeId } ?: run { Log.e("WW_Host", "Host: currentChallengeForCalc is null!"); return@LaunchedEffect }
            val roundResultsMap = activePlayerIds.associateWith { playerId ->
                val playerGuess = guesses[playerId]
                val timeRanOutPlayer = (playerGuess?.submitted != true && isTimeUpGlobally)
                calculatePlayerScoreForRound(playerGuess, currentChallengeForCalc, timeRanOutPlayer)
            }
            val resultsContainer = WWRoundResultsContainer(currentChallengeForCalc.id, roundResultsMap)
            val updatedRoomPlayersData = roomPlayers.map { playerMap ->
                val uid = playerMap["uid"] as? String ?: ""
                val currentTotalScore = (playerMap["totalScore"] as? Long)?.toInt() ?: 0
                playerMap.toMutableMap().apply { this["totalScore"] = currentTotalScore + (roundResultsMap[uid]?.roundScore ?: 0) }
            }
            db.collection("rooms").document(roomCode).update(mapOf(
                "gameState.whereandwhen.roundResults" to resultsContainer,
                "gameState.whereandwhen.roundStatus" to WhereAndWhenGameState.STATUS_RESULTS,
                "gameState.whereandwhen.playersReadyForNextRound" to emptyMap<String, Boolean>(),
                "players" to updatedRoomPlayersData
            )).addOnSuccessListener { Log.d("WW_Host", "Round results posted by host.") }
                .addOnFailureListener { e -> Log.e("WW_Host", "Error posting round results by host", e) }
        }
    }

    LaunchedEffect(wwGameState?.playersReadyForNextRound, roomPlayers, amIHost, wwGameState?.roundStatus) {
        if (!amIHost || wwGameState?.roundStatus != WhereAndWhenGameState.STATUS_RESULTS) return@LaunchedEffect
        val activePlayerIds = roomPlayers.mapNotNull { it["uid"] as? String }; if (activePlayerIds.isEmpty()) return@LaunchedEffect
        val readyPlayers = wwGameState?.playersReadyForNextRound ?: emptyMap()
        if (activePlayerIds.all { readyPlayers[it] == true }) {
            val currentRoundIdx = wwGameState?.currentRoundIndex ?: 0
            if (currentRoundIdx + 1 < TOTAL_ROUNDS) {
                val nextRoundIdx = currentRoundIdx + 1
                Log.d("WW_Host", "All players ready. Host starting next round: ${nextRoundIdx + 1}")

                val currentChallengeOrder = wwGameState?.challengeOrder ?: emptyList()
                val nextChallengeId = currentChallengeOrder.getOrNull(nextRoundIdx)
                    ?: run {
                        Log.e("WW_Host", "Error: Could not get next challenge ID from challengeOrder. Order size: ${currentChallengeOrder.size}, nextRoundIdx: $nextRoundIdx. Fallback needed.")
                        // Fallback: Pick a random challenge not recently used, or a default. This shouldn't happen if TOTAL_ROUNDS <= challengeOrder.size
                        gameChallenges.map { it.id }.filterNot { it == wwGameState?.currentChallengeId }.shuffled().firstOrNull() ?: gameChallenges.first().id
                    }

                db.collection("rooms").document(roomCode).update(mapOf(
                    "gameState.whereandwhen.currentRoundIndex" to nextRoundIdx,
                    "gameState.whereandwhen.currentChallengeId" to nextChallengeId, // Use ID from challengeOrder
                    "gameState.whereandwhen.roundStartTimeMillis" to System.currentTimeMillis(),
                    "gameState.whereandwhen.roundStatus" to WhereAndWhenGameState.STATUS_GUESSING,
                    "gameState.whereandwhen.playerGuesses" to emptyMap<String, Any>(),
                    "gameState.whereandwhen.roundResults" to WWRoundResultsContainer(),
                    "gameState.whereandwhen.playersReadyForNextRound" to emptyMap<String, Boolean>()
                )).addOnFailureListener { e -> Log.e("WW_Host", "Error starting next round by host", e) }
            } else {
                Log.d("WW_Host", "All rounds complete. Host setting game status to ended.")
                db.collection("rooms").document(roomCode).update("status", "ended")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Where & When | Round ${ (wwGameState?.currentRoundIndex ?: 0) + 1}/${TOTAL_ROUNDS}", fontFamily = arcadeFontFamily_WhereAndWhen, fontSize = 16.sp, maxLines = 1, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                actions = { if (wwGameState?.roundStatus == WhereAndWhenGameState.STATUS_GUESSING && !hasSubmittedGuess) { Text("$timeLeftInSeconds", style = TextStyle(fontFamily = arcadeFontFamily_WhereAndWhen, fontSize = 24.sp, color = if (timeLeftInSeconds <= 5 && timeLeftInSeconds % 2 == 0) Color.Red else Color.White, fontWeight = FontWeight.Bold), modifier = Modifier.padding(end = 16.dp)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black.copy(alpha = 0.7f), titleContentColor = Color.White, actionIconContentColor = Color.White)
            )
        }
    ) { paddingValues ->
        if (currentChallenge == null || wwGameState == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues).background(Color(0xFF2C3E50)), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(color = Color.White); Spacer(Modifier.height(8.dp)); Text("Loading Game...", color = Color.White, fontFamily = arcadeFontFamily_WhereAndWhen) } }
            return@Scaffold
        }
        val interactionEnabled = wwGameState?.roundStatus == WhereAndWhenGameState.STATUS_GUESSING && !hasSubmittedGuess && timeLeftInSeconds > 0

        Column(
            modifier = Modifier.padding(paddingValues).fillMaxSize().background(Color(0xFF2C3E50)).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box( modifier = Modifier.weight(2.5f).fillMaxWidth().background(Color.Black, RoundedCornerShape(12.dp)).padding(4.dp), contentAlignment = Alignment.Center ) {
                Image(painter = painterResource(id = currentChallenge.imageResId), contentDescription = "Event: ${currentChallenge.eventName}", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Year: ${selectedYear.toInt()}", style = TextStyle(fontFamily = arcadeFontFamily_WhereAndWhen, fontSize = 22.sp, color = Color.White))
                Slider(value = selectedYear, onValueChange = { selectedYear = it }, valueRange = MIN_SLIDER_YEAR..MAX_SLIDER_YEAR, steps = (MAX_SLIDER_YEAR - MIN_SLIDER_YEAR).toInt() - 1, modifier = Modifier.fillMaxWidth(0.95f), enabled = interactionEnabled, colors = SliderDefaults.colors(thumbColor = Color(0xFFE74C3C), activeTrackColor = Color(0xFFE74C3C).copy(alpha = 0.7f), inactiveTrackColor = Color.Gray.copy(alpha = 0.5f)))
                Row(modifier = Modifier.fillMaxWidth(0.95f), horizontalArrangement = Arrangement.SpaceBetween) { Text(MIN_SLIDER_YEAR.toInt().toString(), style = TextStyle(fontFamily = arcadeFontFamily_WhereAndWhen, fontSize = 12.sp, color = Color.LightGray)); Text(MAX_SLIDER_YEAR.toInt().toString(), style = TextStyle(fontFamily = arcadeFontFamily_WhereAndWhen, fontSize = 12.sp, color = Color.LightGray)) }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Box( modifier = Modifier.weight(3f).fillMaxWidth().background(Color.DarkGray, RoundedCornerShape(12.dp)).padding(1.dp).clip(RoundedCornerShape(11.dp)) ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(), cameraPositionState = cameraPositionState, onMapLoaded = { mapReady = true },
                    uiSettings = MapUiSettings(zoomControlsEnabled = true, mapToolbarEnabled = false, myLocationButtonEnabled = false, scrollGesturesEnabled = interactionEnabled, zoomGesturesEnabled = interactionEnabled, tiltGesturesEnabled = false),
                    onMapClick = { latLng -> if (interactionEnabled) { selectedLatLng = latLng; markerState.position = latLng; markerState.showInfoWindow() } }
                ) { if (selectedLatLng != null) { Marker(state = markerState, title = "Your Guess", snippet = "Lat: ${"%.2f".format(markerState.position.latitude)}, Lng: ${"%.2f".format(markerState.position.longitude)}") } }
                if (!mapReady) { Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally){CircularProgressIndicator(color = Color.White);Spacer(Modifier.height(8.dp));Text("Loading Map...",color=Color.White, fontFamily = arcadeFontFamily_WhereAndWhen)} } }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (!interactionEnabled) return@Button
                    hasSubmittedGuess = true // Lock UI locally FIRST
                    val guessToSubmit = WWPlayerGuess(selectedYear.toInt(), selectedLatLng?.latitude, selectedLatLng?.longitude, true, (ROUND_TIME_SECONDS - timeLeftInSeconds) * 1000L)
                    db.collection("rooms").document(roomCode).update("gameState.whereandwhen.playerGuesses.$myPlayerId", guessToSubmit)
                        .addOnSuccessListener { Log.d("WW_Submit", "Guess successfully submitted by $myPlayerId.") }
                        .addOnFailureListener { e -> Log.e("WW_Submit", "Error submitting guess by $myPlayerId", e); hasSubmittedGuess = false /* Unlock on failure */ }
                },
                modifier = Modifier.fillMaxWidth(0.8f).height(60.dp), shape = RoundedCornerShape(16.dp), enabled = selectedLatLng != null && interactionEnabled,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27AE60), disabledContainerColor = Color.Gray.copy(alpha = 0.5f))
            ) { Text(if(hasSubmittedGuess) "WAITING..." else "SUBMIT GUESS", fontSize = 24.sp, fontFamily = arcadeFontFamily_WhereAndWhen, fontWeight = FontWeight.Bold, color = Color.White) }
        }

        if (showRoundResultsDialog && wwGameState?.roundStatus == WhereAndWhenGameState.STATUS_RESULTS && wwGameState?.roundResults?.results?.isNotEmpty() == true && currentChallenge != null) {
            val actualChallengeInfo = currentChallenge; val allPlayerResultsInfo = wwGameState!!.roundResults.results
            val myRoundResult = allPlayerResultsInfo[myPlayerId]
            var iWonThisRound = false
            if (myRoundResult != null && allPlayerResultsInfo.isNotEmpty()) {
                val maxScoreThisRound = allPlayerResultsInfo.values.maxOfOrNull { it.roundScore } ?: 0
                iWonThisRound = myRoundResult.roundScore >= maxScoreThisRound && myRoundResult.roundScore > 0 // Check if my score is >= max and > 0
            }

            val dialogBorderColor = if (iWonThisRound) {
                Color(0xFF27AE60) // Green for win
            } else {
                Color(0xFFE74C3C) // Red for loss/not win
            }
            AlertDialog(
                onDismissRequest = { /* Non-dismissable */ },
                shape = RoundedCornerShape(0.dp), // SHARP CORNERS
                containerColor = Color(0xFFC0C0C0), // Another flat, retro-ish gray
                titleContentColor = Color.Black,
                textContentColor = Color.Black,
                modifier = Modifier.border(BorderStroke(4.dp, dialogBorderColor)),
                title = {
                    Text(
                        "Round ${(wwGameState?.currentRoundIndex ?: 0) + 1} Results",
                        fontFamily = arcadeFontFamily_WhereAndWhen, // APPLY FONT
                        fontSize = 26.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text(
                            "Event: ${actualChallengeInfo.eventName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp, // You might want to adjust font size for arcade font
                            fontFamily = arcadeFontFamily_WhereAndWhen, // APPLY FONT
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(bottom=12.dp)
                        )
                        allPlayerResultsInfo.forEach { (pId, result) ->
                            val playerName = roomPlayers.find { it["uid"] == pId }?.get("name") as? String ?: "Player"
                            val prefix = if (pId == myPlayerId) "Your" else "$playerName's"
                            Text(
                                "$prefix Score: ${result.roundScore}",
                                fontWeight = if (pId == myPlayerId) FontWeight.ExtraBold else FontWeight.Bold,
                                fontSize = 16.sp, // Adjust
                                fontFamily = arcadeFontFamily_WhereAndWhen // APPLY FONT
                            )
                            if (pId == myPlayerId || allPlayerResultsInfo.size <= 2) { // Show details for self or if few players
                                Text(
                                    "  Year: ${result.guessedYear} (Actual: ${actualChallengeInfo.correctYear}) -> ${result.yearScore} pts",
                                    fontSize = 14.sp, // Adjust
                                    fontFamily = arcadeFontFamily_WhereAndWhen // APPLY FONT
                                )
                                val distKmStr = result.distanceKm?.let { "%.0f km".format(it) } ?: "N/A"
                                Text(
                                    "  Location: $distKmStr -> ${result.locationScore} pts",
                                    fontSize = 14.sp, // Adjust
                                    fontFamily = arcadeFontFamily_WhereAndWhen // APPLY FONT
                                )
                                if(result.timeRanOut) Text(
                                    "  (Time ran out for $playerName)",
                                    color = Color.Red.copy(alpha = 0.8f),
                                    fontSize = 13.sp, // Adjust
                                    fontFamily = arcadeFontFamily_WhereAndWhen // APPLY FONT
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            db.collection("rooms").document(roomCode).update("gameState.whereandwhen.playersReadyForNextRound.$myPlayerId", true)
                                .addOnSuccessListener { Log.d("WW_Ready", "$myPlayerId is ready for next round.")}
                        },
                        enabled = wwGameState?.playersReadyForNextRound?.get(myPlayerId) != true
                    ) {
                        Text(
                            if (wwGameState?.playersReadyForNextRound?.get(myPlayerId) == true) "Waiting..."
                            else if ((wwGameState?.currentRoundIndex ?: 0) + 1 < TOTAL_ROUNDS) "Next Round"
                            else "Final Scores",
                            fontFamily = arcadeFontFamily_WhereAndWhen // APPLY FONT
                        )
                    }
                },
            )
        }

        if (showFinalResultsDialog) {
            val finalScoresText = roomPlayers.sortedByDescending { (it["totalScore"] as? Long)?.toInt() ?: 0 }.joinToString("\n") { playerMap ->
                val name = playerMap["name"] as? String ?: "Player"; val score = (playerMap["totalScore"] as? Long)?.toInt() ?: 0
                // Format each line for the arcade font if needed, though applying to the parent Text might be enough
                "$name: $score points"
            }
            AlertDialog(
                onDismissRequest = { /* ... */ },
                title = {
                    Text(
                        "GAME OVER!",
                        fontFamily = arcadeFontFamily_WhereAndWhen, // APPLY FONT
                        fontSize = 30.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFE74C3C)
                    )
                },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Final Standings:",
                            style = TextStyle(
                                fontFamily = arcadeFontFamily_WhereAndWhen, // APPLY FONT
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            finalScoresText,
                            textAlign = TextAlign.Center,
                            fontSize = 18.sp, // Adjust
                            lineHeight = 22.sp, // Adjust for arcade font
                            fontFamily = arcadeFontFamily_WhereAndWhen // APPLY FONT
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        scope.launch {
                            if (amIHost) {
                                db.collection("rooms").document(roomCode).delete()
                                    .addOnSuccessListener { Log.d("WW_FinalExit", "Host deleted room $roomCode.") }
                                    .addOnFailureListener { e -> Log.e("WW_FinalExit", "Error host deleting room $roomCode.", e) }
                            }
                            navController.navigate(NavRoutes.LOBBY_MENU.replace("{gameId}", "whereandwhen")) {
                                popUpTo(NavRoutes.MAIN_MENU) { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    }) {
                        Text(
                            "Exit to Lobby",
                            fontFamily = arcadeFontFamily_WhereAndWhen // APPLY FONT
                        )
                    }
                },
                containerColor = Color(0xFFECF0F1)
            )
        }
    }
}

@Preview(showBackground = true, device = "id:pixel_5", showSystemUi = true)
@Composable
fun WhereAndWhenScreenPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            // Create a dummy NavController for the preview
            val navController = rememberNavController()
            WhereAndWhenScreen(
                navController = navController, // Pass the dummy NavController
                roomCode = "previewRoom",
                currentUserName = "PreviewUser"
            )
    }}
}