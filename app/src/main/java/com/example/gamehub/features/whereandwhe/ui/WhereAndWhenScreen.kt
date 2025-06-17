// FILE: app/src/main/java/com/example/gamehub/features/whereandwhe/ui/WhereAndWhenScreen.kt
// REPLACE THE ENTIRE CONTENT OF THIS FILE

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
import androidx.navigation.compose.rememberNavController
import com.example.gamehub.R
import com.example.gamehub.features.whereandwhe.model.WWPlayerGuess
import com.example.gamehub.features.whereandwhe.model.WWPlayerRoundResult
import com.example.gamehub.features.whereandwhe.model.WWRoundResultsContainer
import com.example.gamehub.features.whereandwhe.model.WhereAndWhenGameState
import com.example.gamehub.navigation.NavRoutes
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.ktx.Firebase
import com.google.maps.android.compose.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.abs

// --- Font ---
val arcadeFontFamily_WhereAndWhen = FontFamily(
    Font(R.font.arcade_classic, FontWeight.Normal)
)
val Gold = Color(0xFFFFD700)

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

private const val GUESSING_PHASE_DURATION_SECONDS = 35
private const val MAP_REVEAL_DURATION_MS = 6000L // 6 seconds
private const val RESULTS_DIALOG_TIMEOUT_MS = 15000L // 15 seconds for players to click "Continue"
private const val LEADERBOARD_DURATION_MS = 5000L // 5 seconds for leaderboard display

private const val MIN_SLIDER_YEAR = 1850f
private const val MAX_SLIDER_YEAR = 2024f
private val TOTAL_ROUNDS = 5 // Should match the number of challenges for now

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
    Challenge("great_depression", R.drawable.wall_street_crash_1929, 1929, 40.7069, -74.0113, "Wall Street, New York, USA", "Wall Street Crash of 1929"),
    Challenge("gettysburg_battle", R.drawable.gettysburg_battle_1863, 1863, 39.8140, -77.2301, "Gettysburg, Pennsylvania, USA", "Battle of Gettysburg"),
    Challenge("suez_canal_opening", R.drawable.suez_canal_opening_1869, 1869, 30.5852, 32.2623, "Port Said, Egypt", "Opening of the Suez Canal"),
    Challenge("eiffel_tower_construction", R.drawable.eiffel_tower_construction, 1888, 48.8584, 2.2945, "Paris, France", "Construction of the Eiffel Tower"),
    Challenge("klondike_gold_rush", R.drawable.klondike_gold_rush_1897, 1897, 64.0500, -139.4333, "Dawson City, Yukon, Canada", "Klondike Gold Rush"),
    Challenge("panama_canal_opening", R.drawable.panama_canal_opening_1914, 1914, 9.0800, -79.6800, "Panama Canal, Panama", "Opening of the Panama Canal"),
    Challenge("russian_revolution_1917", R.drawable.russian_revolution_1917, 1917, 59.9371, 30.3097, "Winter Palace, St. Petersburg, Russia", "Russian Revolution (Storming of Winter Palace)"),
    Challenge("prohibition_usa_start", R.drawable.prohibition_usa_start_1920, 1920, 40.7128, -74.0060, "New York City, USA (Speakeasy imagery)", "Start of Prohibition in the USA"),
    Challenge("tutankhamun_tomb_discovery", R.drawable.tutankhamun_tomb_discovery_1922, 1922, 25.7402, 32.6014, "Valley of the Kings, Luxor, Egypt", "Discovery of Tutankhamun's Tomb"),
    Challenge("hoover_dam_completion", R.drawable.hoover_dam_completion_1936, 1936, 36.0160, -114.7377, "Hoover Dam, Nevada/Arizona, USA", "Completion of Hoover Dam"),
    Challenge("hindenburg_disaster", R.drawable.hindenburg_disaster_1937, 1937, 40.0793, -74.3293, "Lakehurst, New Jersey, USA", "Hindenburg Disaster"),
    Challenge("battle_of_britain", R.drawable.battle_of_britain_1940, 1940, 51.5074, -0.1278, "London, UK (Spitfires/Blitz imagery)", "Battle of Britain"),
    Challenge("battle_stalingrad", R.drawable.battle_stalingrad_1943, 1943, 48.7081, 44.5133, "Stalingrad (Volgograd), Russia", "Battle of Stalingrad (Turning Point)"),
    Challenge("israel_founding_1948", R.drawable.israel_founding_1948, 1948, 32.0853, 34.7818, "Tel Aviv, Israel", "Founding of the State of Israel"),
    Challenge("everest_first_ascent", R.drawable.everest_first_ascent_1953, 1953, 27.9881, 86.9250, "Mount Everest, Nepal/China", "First Ascent of Mount Everest"),
    Challenge("sputnik_launch", R.drawable.sputnik_launch_1957, 1957, 45.9647, 63.3052, "Baikonur Cosmodrome, Kazakhstan", "Launch of Sputnik 1"),
    Challenge("march_on_washington", R.drawable.march_on_washington_1963, 1963, 38.8893, -77.0502, "Lincoln Memorial, Washington D.C., USA", "March on Washington for Jobs and Freedom"),
    Challenge("woodstock_festival", R.drawable.woodstock_festival_1969, 1969, 41.7137, -74.8754, "Bethel, New York, USA", "Woodstock Music Festival"),
    Challenge("watergate_scandal_breakin", R.drawable.watergate_scandal_breakin_1972, 1972, 38.9007, -77.0506, "Watergate Complex, Washington D.C., USA", "Watergate Break-in"),
    Challenge("fall_of_saigon", R.drawable.fall_of_saigon_1975, 1975, 10.7769, 106.7009, "Ho Chi Minh City (Saigon), Vietnam", "Fall of Saigon"),
    Challenge("apple_founded_1976", R.drawable.apple_founded_1976, 1976, 37.3318, -122.0312, "Cupertino, California, USA (Garage imagery)", "Founding of Apple Computer"),
    Challenge("challenger_disaster_1986", R.drawable.challenger_disaster_1986, 1986, 28.6084, -80.6043, "Cape Canaveral, Florida, USA", "Space Shuttle Challenger Disaster"),
    Challenge("tiananmen_square_protests", R.drawable.tiananmen_square_protests_1989, 1989, 39.9075, 116.3972, "Tiananmen Square, Beijing, China", "Tiananmen Square Protests (Tank Man)"),
    Challenge("dolly_the_sheep_cloned", R.drawable.dolly_the_sheep_cloned_1996, 1996, 55.9291, -3.2122, "Roslin Institute, Scotland, UK", "Cloning of Dolly the Sheep"),
    Challenge("hong_kong_handover_1997", R.drawable.hong_kong_handover_1997, 1997, 22.2793, 114.1628, "Hong Kong Convention and Exhibition Centre", "Handover of Hong Kong to China"),
    Challenge("indian_ocean_tsunami_2004", R.drawable.indian_ocean_tsunami_2004, 2004, 3.3166, 95.8536, "Banda Aceh, Indonesia (epicenter proxy)", "Indian Ocean Tsunami"),
    Challenge("hurricane_katrina_2005", R.drawable.hurricane_katrina_2005, 2005, 29.9511, -90.0715, "New Orleans, Louisiana, USA", "Hurricane Katrina"),
    Challenge("iphone_launch_2007", R.drawable.iphone_launch_2007, 2007, 37.7749, -122.4194, "San Francisco, CA (Moscone Center)", "Launch of the first iPhone"),
    Challenge("financial_crisis_2008", R.drawable.financial_crisis_2008, 2008, 40.7069, -74.0113, "Wall Street, New York, USA", "Global Financial Crisis (Lehman Brothers collapse)"),
    Challenge("arab_spring_egypt_2011", R.drawable.arab_spring_egypt_2011, 2011, 30.0444, 31.2357, "Tahrir Square, Cairo, Egypt", "Egyptian Revolution (Arab Spring)"),
    Challenge("crimea_annexation_2014", R.drawable.crimea_annexation_2014, 2014, 44.9521, 34.1024, "Simferopol, Crimea", "Annexation of Crimea by Russia"),
    Challenge("paris_agreement_climate", R.drawable.paris_agreement_climate_2015, 2015, 48.8566, 2.3522, "Paris, France", "Paris Agreement on Climate Change Signed"),
    Challenge("notre_dame_fire_2019", R.drawable.notre_dame_fire_2019, 2019, 48.8530, 2.3499, "Notre-Dame Cathedral, Paris, France", "Notre-Dame Cathedral Fire"),
    Challenge("us_capitol_attack_2021", R.drawable.us_capitol_attack_2021, 2021, 38.8899, -77.0091, "U.S. Capitol, Washington D.C., USA", "January 6th Capitol Attack"),
    Challenge("ukraine_invasion_2022", R.drawable.ukraine_invasion_2022, 2022, 50.4501, 30.5234, "Kyiv, Ukraine", "Start of Full-Scale Ukraine Invasion")
)

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
    val guessedYearValue = playerGuess?.year ?: actualYear // Default to actual year if no guess (0 points)
    val guessedLatLngValue = if (playerGuess?.lat != null && playerGuess.lng != null) LatLng(playerGuess.lat, playerGuess.lng) else null

    // If player ran out of time, or somehow didn't submit a guess (which timeout should prevent for playerGuesses map)
    if (timeRanOutForThisPlayer || playerGuess == null || (!playerGuess.submitted && !timeRanOutForThisPlayer) ) {
        val distanceForNoGuess = guessedLatLngValue?.let { calculateDistanceKm(it.latitude, it.longitude, actualLocation.latitude, actualLocation.longitude) }
        return WWPlayerRoundResult(guessedYearValue, 0, guessedLatLngValue?.latitude, guessedLatLngValue?.longitude, distanceForNoGuess, 0, 0, true)
    }

    val yearDifference = abs(guessedYearValue - actualYear)
    val yearScore = if (yearDifference > MAX_YEAR_DIFFERENCE_FOR_POINTS) 0 else (MAX_YEAR_SCORE * (1.0 - (yearDifference.toDouble() / MAX_YEAR_DIFFERENCE_FOR_POINTS))).toInt()

    var locationScore = 0
    var distanceInKm: Double? = null
    if (guessedLatLngValue != null) {
        distanceInKm = calculateDistanceKm(guessedLatLngValue.latitude, guessedLatLngValue.longitude, actualLocation.latitude, actualLocation.longitude)
        locationScore = if (distanceInKm > MAX_DISTANCE_KM_FOR_POINTS) 0 else (MAX_LOCATION_SCORE * (1.0 - (distanceInKm / MAX_DISTANCE_KM_FOR_POINTS))).toInt()
    }

    return WWPlayerRoundResult(
        guessedYear = guessedYearValue,
        yearScore = yearScore.coerceIn(0, MAX_YEAR_SCORE),
        guessedLat = guessedLatLngValue?.latitude,
        guessedLng = guessedLatLngValue?.longitude,
        distanceKm = distanceInKm,
        locationScore = locationScore.coerceIn(0, MAX_LOCATION_SCORE),
        roundScore = (yearScore + locationScore).coerceIn(0, MAX_YEAR_SCORE + MAX_LOCATION_SCORE),
        timeRanOut = false // If we reached here, it means a guess was submitted (not due to global timeout forcing a zero score)
    )
}

private suspend fun proceedToNextRoundOrEndGame(
    db: FirebaseFirestore,
    roomCode: String,
    currentWwGameState: WhereAndWhenGameState?,
    totalRounds: Int,
    myPlayerIdForLog: String
) {
    Log.i("WW_Host_Proc_Entry", "[HOST $myPlayerIdForLog] Entering proceedToNextRoundOrEndGame. CurrentRound: ${currentWwGameState?.currentRoundIndex}")
    if (currentWwGameState == null) {
        Log.e("WW_Host_Proc_Error", "[HOST $myPlayerIdForLog] Cannot proceed, wwGameState is null.")
        return
    }
    val currentRoundIdx = currentWwGameState.currentRoundIndex
    if (currentRoundIdx + 1 < totalRounds) {
        val nextRoundIdx = currentRoundIdx + 1
        Log.i("WW_Host_Proc_NextRound", "[HOST $myPlayerIdForLog] Starting next round: ${nextRoundIdx + 1}")

        // Use the existing challengeOrder from the game state
        val currentChallengeOrder = currentWwGameState.challengeOrder
        val nextChallengeId = currentChallengeOrder.getOrNull(nextRoundIdx)
            ?: run { // Fallback if order is too short or empty (shouldn't happen if initialized correctly)
                Log.e("WW_Host_Proc_Error", "[HOST $myPlayerIdForLog] Challenge order issue! Order size: ${currentChallengeOrder.size}, next index: $nextRoundIdx. Falling back.")
                gameChallenges.map { it.id }.filterNot { it == currentWwGameState.currentChallengeId }.shuffled().firstOrNull()
                    ?: gameChallenges.first().id // Absolute fallback
            }

        val updates = mapOf(
            "gameState.whereandwhen.currentRoundIndex" to nextRoundIdx,
            "gameState.whereandwhen.currentChallengeId" to nextChallengeId,
            "gameState.whereandwhen.roundStartTimeMillis" to System.currentTimeMillis(),
            "gameState.whereandwhen.roundStatus" to WhereAndWhenGameState.STATUS_GUESSING,
            "gameState.whereandwhen.playerGuesses" to emptyMap<String, Any>(),
            "gameState.whereandwhen.roundResults" to WWRoundResultsContainer(),
            "gameState.whereandwhen.mapRevealStartTimeMillis" to 0L,
            "gameState.whereandwhen.resultsDialogStartTimeMillis" to 0L,
            "gameState.whereandwhen.leaderboardStartTimeMillis" to 0L,
            "gameState.whereandwhen.playersReadyForResultsDialog" to emptyMap<String, Boolean>(),
            "gameState.whereandwhen.playersReadyForLeaderboard" to emptyMap<String, Boolean>(),
            "gameState.whereandwhen.playersReadyForNextRound" to emptyMap<String, Boolean>()
            // challengeOrder remains the same
        )
        Log.d("WW_Host_Proc_Update", "[HOST $myPlayerIdForLog] Firestore update for next round: $updates")
        db.collection("rooms").document(roomCode).update(updates)
            .addOnSuccessListener { Log.i("WW_Host_Proc_Success", "[HOST $myPlayerIdForLog] Firestore updated for next round successfully.") }
            .addOnFailureListener { e -> Log.e("WW_Host_Proc_Fail", "[HOST $myPlayerIdForLog] Error starting next round", e) }
    } else {
        Log.i("WW_Host_Proc_EndGame", "[HOST $myPlayerIdForLog] All rounds complete. Setting game status to ended.")
        db.collection("rooms").document(roomCode).update("status", "ended")
            .addOnSuccessListener { Log.i("WW_Host_Proc_Success", "[HOST $myPlayerIdForLog] Game status set to 'ended'.")}
            .addOnFailureListener { e -> Log.e("WW_Host_Proc_Fail", "[HOST $myPlayerIdForLog] Error setting game status to 'ended'", e)}
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhereAndWhenScreen(
    navController: NavController,
    roomCode: String,
    currentUserName: String
) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val db = FirebaseFirestore.getInstance()
    val myPlayerId = Firebase.auth.currentUser?.uid ?: UUID.randomUUID().toString()

    var wwGameState by remember { mutableStateOf<WhereAndWhenGameState?>(null) }
    var roomDocSnapshot by remember { mutableStateOf<Map<String, Any>?>(null) }

    val roomPlayers = remember(roomDocSnapshot) {
        @Suppress("UNCHECKED_CAST")
        roomDocSnapshot?.get("players") as? List<Map<String, Any>> ?: emptyList()
    }
    val amIHost = remember(roomDocSnapshot, myPlayerId) {
        roomDocSnapshot?.get("hostUid") == myPlayerId
    }

    var selectedYear by remember { mutableStateOf((MIN_SLIDER_YEAR + MAX_SLIDER_YEAR) / 2f) }
    var selectedLatLng by remember { mutableStateOf<LatLng?>(null) }
    val playerGuessMarkerState = rememberMarkerState()
    val cameraPositionState = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(LatLng(20.0, 0.0), 1.5f) }
    var mapReady by remember { mutableStateOf(false) }
    var hasSubmittedGuessThisRound by remember { mutableStateOf(false) }
    var newRoundSignalId by remember { mutableStateOf<String?>(null) }

    var timeLeftInSeconds by remember { mutableStateOf(GUESSING_PHASE_DURATION_SECONDS) }
    val showGuessingUI by remember(wwGameState, hasSubmittedGuessThisRound) { derivedStateOf { wwGameState?.roundStatus == WhereAndWhenGameState.STATUS_GUESSING && !hasSubmittedGuessThisRound } }
    val showWaitingForOthersUI by remember(wwGameState, hasSubmittedGuessThisRound) { derivedStateOf { wwGameState?.roundStatus == WhereAndWhenGameState.STATUS_GUESSING && hasSubmittedGuessThisRound } }
    val showMapRevealUI by remember(wwGameState) { derivedStateOf { wwGameState?.roundStatus == WhereAndWhenGameState.STATUS_SHOWING_MAP_REVEAL } }
    val showResultsDialogUI by remember(wwGameState) { derivedStateOf { wwGameState?.roundStatus == WhereAndWhenGameState.STATUS_RESULTS } }
    val showLeaderboardUI by remember(wwGameState) { derivedStateOf { wwGameState?.roundStatus == WhereAndWhenGameState.STATUS_SHOWING_LEADERBOARD } }
    var showFinalResultsDialog by remember { mutableStateOf(false) }

    var revealedGuessedLatLngMap by remember { mutableStateOf<Map<String, LatLng?>>(emptyMap()) }
    var revealedActualLatLng by remember { mutableStateOf<LatLng?>(null) }

    val currentChallenge = remember(wwGameState?.currentChallengeId) {
        Log.d("WW_Debug_Challenge_Memo", "[Player $myPlayerId] Re-evaluating currentChallenge. wwGameState ChallengeId: ${wwGameState?.currentChallengeId}")
        gameChallenges.find { it.id == wwGameState?.currentChallengeId }
    }

    LaunchedEffect(Unit) {
        Log.i("WW_Screen_Init", "WhereAndWhenScreen Composed. Player: $myPlayerId, Room: $roomCode, IsHost: $amIHost")
    }

    Log.d("WW_Recompose_INFO", "Screen Recomposed. Player: $myPlayerId | Status: ${wwGameState?.roundStatus} | ChallengeID: ${wwGameState?.currentChallengeId} | CurrentChallengeObj: ${currentChallenge?.eventName} | mapReady: $mapReady | newRoundSignal: $newRoundSignalId | submittedThisRound: $hasSubmittedGuessThisRound")

    var timeUpSoundPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    DisposableEffect(Unit) {
        try { timeUpSoundPlayer = MediaPlayer.create(context, R.raw.times_up_sound) }
        catch (e: Exception) { Log.e("WW_Sound_Error", "Error creating MediaPlayer for timeUpSound", e) }
        onDispose { timeUpSoundPlayer?.release(); timeUpSoundPlayer = null }
    }
    fun playSound(player: MediaPlayer?) {
        try { player?.let { if (it.isPlaying) { it.stop(); it.prepare() }; it.start() } }
        catch (e: Exception) { Log.e("WW_Sound_Error", "Error playing sound", e) }
    }

    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window; val wc = window?.let { WindowInsetsControllerCompat(it, view) }
        if (window != null && wc != null) { WindowCompat.setDecorFitsSystemWindows(window, false); wc.hide(WindowInsetsCompat.Type.systemBars()); wc.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE }
        onDispose { if (window != null && wc != null) { WindowCompat.setDecorFitsSystemWindows(window, true); wc.show(WindowInsetsCompat.Type.systemBars()) } }
    }

    DisposableEffect(roomCode, lifecycleOwner) {
        var roomListenerReg: ListenerRegistration? = null
        val roomDocRef = db.collection("rooms").document(roomCode)
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                Log.i("WW_Listener_Life", "Attaching Firestore listener for room: $roomCode, Player: $myPlayerId")
                roomListenerReg = roomDocRef.addSnapshotListener { snapshot, error ->
                    if (error != null) { Log.e("WW_Listener_Error", "Room listen error for $roomCode", error); return@addSnapshotListener }
                    if (snapshot != null && snapshot.exists()) {
                        roomDocSnapshot = snapshot.data
                        @Suppress("UNCHECKED_CAST")
                        val rawGameState = snapshot.get("gameState.whereandwhen") as? Map<String, Any>
                        if (rawGameState != null) {
                            try {
                                val previousWwGameState = wwGameState
                                val newState = WhereAndWhenGameState(
                                    currentRoundIndex = (rawGameState["currentRoundIndex"] as? Long)?.toInt() ?: 0,
                                    currentChallengeId = rawGameState["currentChallengeId"] as? String ?: "",
                                    roundStartTimeMillis = rawGameState["roundStartTimeMillis"] as? Long ?: 0L,
                                    roundStatus = rawGameState["roundStatus"] as? String ?: WhereAndWhenGameState.STATUS_GUESSING,
                                    playerGuesses = (rawGameState["playerGuesses"] as? Map<String, Map<String, Any>>)?.mapValues { entry -> WWPlayerGuess( year = (entry.value["year"] as? Long)?.toInt(), lat = entry.value["lat"] as? Double, lng = entry.value["lng"] as? Double, submitted = entry.value["submitted"] as? Boolean ?: false, timeTakenMs = entry.value["timeTakenMs"] as? Long ) } ?: emptyMap(),
                                    roundResults = (rawGameState["roundResults"] as? Map<String, Any>)?.let { rrMap -> WWRoundResultsContainer( challengeId = rrMap["challengeId"] as? String ?: "", results = (rrMap["results"] as? Map<String, Map<String, Any>>)?.mapValues { entry -> WWPlayerRoundResult( guessedYear = (entry.value["guessedYear"] as? Long)?.toInt() ?: 0, yearScore = (entry.value["yearScore"] as? Long)?.toInt() ?: 0, guessedLat = entry.value["guessedLat"] as? Double, guessedLng = entry.value["guessedLng"] as? Double, distanceKm = entry.value["distanceKm"] as? Double, locationScore = (entry.value["locationScore"] as? Long)?.toInt() ?: 0, roundScore = (entry.value["roundScore"] as? Long)?.toInt() ?: 0, timeRanOut = entry.value["timeRanOut"] as? Boolean ?: false ) } ?: emptyMap() ) } ?: WWRoundResultsContainer(),
                                    mapRevealStartTimeMillis = rawGameState["mapRevealStartTimeMillis"] as? Long ?: 0L,
                                    resultsDialogStartTimeMillis = rawGameState["resultsDialogStartTimeMillis"] as? Long ?: 0L,
                                    leaderboardStartTimeMillis = rawGameState["leaderboardStartTimeMillis"] as? Long ?: 0L,
                                    playersReadyForResultsDialog = rawGameState["playersReadyForResultsDialog"] as? Map<String, Boolean> ?: emptyMap(),
                                    playersReadyForLeaderboard = rawGameState["playersReadyForLeaderboard"] as? Map<String, Boolean> ?: emptyMap(),
                                    playersReadyForNextRound = rawGameState["playersReadyForNextRound"] as? Map<String, Boolean> ?: emptyMap(),
                                    challengeOrder = rawGameState["challengeOrder"] as? List<String> ?: emptyList()
                                )
                                wwGameState = newState

                                Log.i("WW_Listener_STATE_UPDATE", "[Player $myPlayerId] PrevStatus: ${previousWwGameState?.roundStatus}, NewStatus: ${newState.roundStatus} | PrevChallenge: ${previousWwGameState?.currentChallengeId}, NewChallenge: ${newState.currentChallengeId} | PrevRoundIdx: ${previousWwGameState?.currentRoundIndex}, NewRoundIdx: ${newState.currentRoundIndex}")

                                val justStartedNewGuessingRound =
                                    (newState.roundStatus == WhereAndWhenGameState.STATUS_GUESSING) &&
                                            ( (previousWwGameState?.roundStatus != WhereAndWhenGameState.STATUS_GUESSING) ||
                                                    (previousWwGameState?.currentRoundIndex != newState.currentRoundIndex) )

                                if (justStartedNewGuessingRound) {
                                    Log.i("WW_UI_Reset_Signal", "Firestore listener: New guessing round SIGNAL set for Challenge: ${newState.currentChallengeId}. Player: $myPlayerId")
                                    newRoundSignalId = newState.currentChallengeId // This triggers the UI reset effect
                                    hasSubmittedGuessThisRound = false // Reset submission status for the new round
                                }
                            } catch (e: Exception) { Log.e("WW_Listener_Error", "Error PARSING game state for $roomCode", e) }
                        }
                        if (snapshot.getString("status") == "ended") {
                            Log.i("WW_Listener_GameEnd", "[Player $myPlayerId] Received game ended status from Firestore.")
                            showFinalResultsDialog = true
                        }
                    } else {
                        Log.w("WW_Listener_Error", "[Player $myPlayerId] Room $roomCode does not exist or was deleted.")
                        if (!amIHost) {
                            Toast.makeText(context, "Host closed the room or room data missing.", Toast.LENGTH_LONG).show()
                            navController.navigate(NavRoutes.LOBBY_MENU.replace("{gameId}", "whereandwhen")) {
                                popUpTo(NavRoutes.MAIN_MENU) { inclusive = false }; launchSingleTop = true
                            }
                        }
                        wwGameState = null; roomDocSnapshot = null
                    }
                }
            }
            override fun onStop(owner: LifecycleOwner) { roomListenerReg?.remove(); Log.i("WW_Listener_Life", "Firestore listener REMOVED for $roomCode, Player $myPlayerId") }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer); roomListenerReg?.remove() }
    }

    LaunchedEffect(newRoundSignalId, mapReady) {
        if (newRoundSignalId != null && mapReady) {
            Log.i("WW_UI_Reset_EFFECT", "[Player $myPlayerId] Effect: Processing new round signal for challenge $newRoundSignalId. Map IS ready.")
            selectedYear = (MIN_SLIDER_YEAR + MAX_SLIDER_YEAR) / 2f
            selectedLatLng = null
            playerGuessMarkerState.position = LatLng(0.0,0.0) // Reset marker to avoid stale display
            playerGuessMarkerState.hideInfoWindow()
            try {
                Log.i("WW_Camera_EFFECT_Reset", "[Player $myPlayerId] Resetting camera for new round $newRoundSignalId. Current Cam Pos: ${cameraPositionState.position}")
                cameraPositionState.move(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.fromLatLngZoom(LatLng(20.0, 0.0), 1.5f)
                    )
                )
            } catch (e: Exception) { Log.e("WW_Camera_EFFECT_Reset", "[Player $myPlayerId] Error resetting camera for new round: ${e.message}") }
            newRoundSignalId = null // Consume the signal
        } else if (newRoundSignalId != null && !mapReady) {
            Log.w("WW_UI_Reset_EFFECT", "[Player $myPlayerId] Effect: New round signal $newRoundSignalId, but map is NOT ready. Will retry when mapReady.")
        }
    }

    // Local timer for each client's guessing phase
    LaunchedEffect(wwGameState?.roundStartTimeMillis, wwGameState?.roundStatus, wwGameState?.currentRoundIndex, hasSubmittedGuessThisRound) {
        val startTime = wwGameState?.roundStartTimeMillis ?: 0L
        if (hasSubmittedGuessThisRound || startTime == 0L || wwGameState?.roundStatus != WhereAndWhenGameState.STATUS_GUESSING) {
            timeLeftInSeconds = if (wwGameState?.roundStatus != WhereAndWhenGameState.STATUS_GUESSING && !hasSubmittedGuessThisRound) 0 else GUESSING_PHASE_DURATION_SECONDS
            return@LaunchedEffect
        }
        Log.d("WW_Timer", "[Player $myPlayerId] Local guessing timer active. Submitted: $hasSubmittedGuessThisRound. Current Round: ${wwGameState?.currentRoundIndex}")
        while (wwGameState?.roundStatus == WhereAndWhenGameState.STATUS_GUESSING && !hasSubmittedGuessThisRound && isActive) {
            val elapsedMillis = System.currentTimeMillis() - startTime
            val currentRemaining = GUESSING_PHASE_DURATION_SECONDS - (elapsedMillis / 1000).toInt()
            timeLeftInSeconds = currentRemaining.coerceAtLeast(0)
            if (timeLeftInSeconds == 0) {
                if (!hasSubmittedGuessThisRound) { // Double check, might have submitted just before timer ticked to 0
                    playSound(timeUpSoundPlayer)
                    Log.i("WW_Timer", "[Player $myPlayerId] Local GUESSING timer ran out. Auto-submitting.")
                    hasSubmittedGuessThisRound = true // Optimistically set, Firestore success will confirm
                    val guessToSubmit = WWPlayerGuess(selectedYear.toInt(), selectedLatLng?.latitude, selectedLatLng?.longitude, true, (GUESSING_PHASE_DURATION_SECONDS * 1000L))
                    db.collection("rooms").document(roomCode).update("gameState.whereandwhen.playerGuesses.$myPlayerId", guessToSubmit)
                        .addOnSuccessListener { Log.i("WW_Submit", "[Player $myPlayerId] Auto-submitted guess (timeout).") }
                        .addOnFailureListener { e -> Log.e("WW_Submit", "[Player $myPlayerId] Error auto-submitting guess (timeout)", e); hasSubmittedGuessThisRound = false /* Rollback on failure */ }
                }
                break
            }
            delay(500) // Check every half second
        }
    }

    // --- HOST LOGIC FOR STATE TRANSITIONS ---
    // 1. Guessing -> Map Reveal (Host only)
    LaunchedEffect(wwGameState?.playerGuesses, roomPlayers.size, amIHost, wwGameState?.roundStatus, wwGameState?.roundStartTimeMillis) {
        if (!amIHost || wwGameState?.roundStatus != WhereAndWhenGameState.STATUS_GUESSING) return@LaunchedEffect

        val activePlayerIds = roomPlayers.mapNotNull { it["uid"] as? String }
        if (activePlayerIds.isEmpty() || (roomPlayers.isNotEmpty() && activePlayerIds.size != roomPlayers.size)) {
            Log.d("WW_Host_Flow_GuessEnd", "[HOST $myPlayerId] Guessing: Waiting for all player UIDs. Found ${activePlayerIds.size}/${roomPlayers.size}.")
            return@LaunchedEffect
        }

        val guesses = wwGameState?.playerGuesses ?: emptyMap()
        val allHaveSubmitted = activePlayerIds.all { guesses[it]?.submitted == true }
        val roundTimeMillis = wwGameState?.roundStartTimeMillis ?: 0L
        val isTimeUpGlobally = roundTimeMillis > 0 && (System.currentTimeMillis() - roundTimeMillis) >= (GUESSING_PHASE_DURATION_SECONDS * 1000 + 2000L) // 2s buffer

        if (allHaveSubmitted || isTimeUpGlobally) {
            Log.i("WW_Host_Flow_GuessEnd", "[HOST $myPlayerId] Guessing ended. Transitioning to Map Reveal. AllSubmitted=$allHaveSubmitted, TimeUpGlobal=$isTimeUpGlobally. Players: ${activePlayerIds.joinToString()}, Guesses: $guesses")
            db.collection("rooms").document(roomCode).update(mapOf(
                "gameState.whereandwhen.roundStatus" to WhereAndWhenGameState.STATUS_SHOWING_MAP_REVEAL,
                "gameState.whereandwhen.mapRevealStartTimeMillis" to System.currentTimeMillis()
            )).addOnFailureListener { e -> Log.e("WW_Host_Flow_GuessEnd", "[HOST $myPlayerId] Error setting status to SHOWING_MAP_REVEAL", e) }
        }
    }

    // 2. Map Reveal -> Results (Host only)
    LaunchedEffect(amIHost, wwGameState?.roundStatus, wwGameState?.mapRevealStartTimeMillis, wwGameState?.currentChallengeId) {
        val effectIsHost = amIHost
        val effectRoundStatus = wwGameState?.roundStatus
        val effectMapRevealStartTime = wwGameState?.mapRevealStartTimeMillis ?: 0L
        val initialEffectChallengeId = wwGameState?.currentChallengeId

        val initialEffectChallengeObject = initialEffectChallengeId?.let { chalId -> gameChallenges.find { it.id == chalId } }

        Log.d("WW_Host_MR_Entry", "[Host $myPlayerId] Entering MapReveal->Results Effect. Host: $effectIsHost, Status: $effectRoundStatus, MapRevealStart: $effectMapRevealStartTime, ChallengeID: $initialEffectChallengeId, ChallengeObj: ${initialEffectChallengeObject?.eventName}")

        if (!effectIsHost || effectRoundStatus != WhereAndWhenGameState.STATUS_SHOWING_MAP_REVEAL) {
            return@LaunchedEffect
        }
        if (effectMapRevealStartTime == 0L) {
            Log.d("WW_Host_MR_Exit", "[Host $myPlayerId] Map Reveal: mapRevealStartTimeMillis is 0, waiting.")
            return@LaunchedEffect
        }
        if (initialEffectChallengeObject == null) {
            Log.e("WW_Host_MR_Error", "[Host $myPlayerId] Map Reveal: initialEffectChallengeObject is null! ChallengeID was $initialEffectChallengeId. Cannot proceed.")
            return@LaunchedEffect
        }

        val timeSinceMapRevealStarted = System.currentTimeMillis() - effectMapRevealStartTime
        val timeRemainingForMapReveal = MAP_REVEAL_DURATION_MS - timeSinceMapRevealStarted

        if (timeRemainingForMapReveal > 0) {
            Log.d("WW_Host_MR_Waiting", "[Host $myPlayerId] Map Reveal: Duration not yet ended for ${initialEffectChallengeObject.id}. Time left: ${timeRemainingForMapReveal}ms. Delaying...")
            delay(timeRemainingForMapReveal)
        }

        val currentWwGameStateAfterDelay = wwGameState
        if (currentWwGameStateAfterDelay?.roundStatus != WhereAndWhenGameState.STATUS_SHOWING_MAP_REVEAL ||
            currentWwGameStateAfterDelay.mapRevealStartTimeMillis != effectMapRevealStartTime ||
            currentWwGameStateAfterDelay.currentChallengeId != initialEffectChallengeId) {
            Log.w("WW_Host_MR_Stale", "[Host $myPlayerId] Map Reveal: State changed during delay for challenge ${initialEffectChallengeObject.id}. Aborting. Status: ${currentWwGameStateAfterDelay?.roundStatus}, StartTime: ${currentWwGameStateAfterDelay?.mapRevealStartTimeMillis}, ChalId: ${currentWwGameStateAfterDelay?.currentChallengeId}")
            return@LaunchedEffect
        }

        val currentChallengeAfterDelay = initialEffectChallengeId.let { chalId -> gameChallenges.find { it.id == chalId } }
        if (currentChallengeAfterDelay == null) {
            Log.e("WW_Host_MR_Error_PostDelay", "[Host $myPlayerId] Map Reveal: currentChallengeAfterDelay is null post-delay! ChallengeID was $initialEffectChallengeId.")
            return@LaunchedEffect
        }

        Log.i("WW_Host_Flow_MapRevealEnd", "[HOST $myPlayerId] Map Reveal DURATION ENDED for challenge: ${currentChallengeAfterDelay.id}. Preparing to calculate results.")
        val guesses = currentWwGameStateAfterDelay.playerGuesses
        val activePlayerIds = roomPlayers.mapNotNull { it["uid"] as? String }

        val roundResultsMap = activePlayerIds.associateWith { playerId ->
            val playerGuess = guesses[playerId]
            val timeTaken = playerGuess?.timeTakenMs ?: (GUESSING_PHASE_DURATION_SECONDS * 1000L + 1000L)
            val timeRanOutForPlayer = playerGuess?.submitted == false || timeTaken >= GUESSING_PHASE_DURATION_SECONDS * 1000L
            calculatePlayerScoreForRound(playerGuess, currentChallengeAfterDelay, timeRanOutForPlayer)
        }

        val resultsContainer = WWRoundResultsContainer(currentChallengeAfterDelay.id, roundResultsMap)
        val updatedRoomPlayersData = roomPlayers.map { playerMap ->
            val uid = playerMap["uid"] as? String ?: ""
            val currentTotalScore = (playerMap["totalScore"] as? Long)?.toInt() ?: 0
            playerMap.toMutableMap().apply { this["totalScore"] = currentTotalScore + (roundResultsMap[uid]?.roundScore ?: 0) }
        }

        db.collection("rooms").document(roomCode).update(mapOf(
            "gameState.whereandwhen.roundResults" to resultsContainer,
            "gameState.whereandwhen.roundStatus" to WhereAndWhenGameState.STATUS_RESULTS,
            "gameState.whereandwhen.resultsDialogStartTimeMillis" to System.currentTimeMillis(),
            "gameState.whereandwhen.playersReadyForLeaderboard" to emptyMap<String, Boolean>(),
            "players" to updatedRoomPlayersData
        )).addOnSuccessListener {
            Log.i("WW_Host_Flow_MapRevealEnd", "[HOST $myPlayerId] Successfully posted round results & updated state to RESULTS for challenge ${currentChallengeAfterDelay.id}")
        }.addOnFailureListener { e -> Log.e("WW_Host_Flow_MapRevealEnd", "[HOST $myPlayerId] Error posting round results for challenge ${currentChallengeAfterDelay.id}", e) }
    }

    // 3. Results -> Leaderboard (Host only)
    LaunchedEffect(amIHost, wwGameState?.roundStatus, wwGameState?.resultsDialogStartTimeMillis, wwGameState?.playersReadyForLeaderboard?.hashCode(), wwGameState?.currentChallengeId) {
        val effectIsHost = amIHost
        val effectRoundStatus = wwGameState?.roundStatus
        val effectDialogStartTime = wwGameState?.resultsDialogStartTimeMillis ?: 0L
        val effectReadyPlayers = wwGameState?.playersReadyForLeaderboard ?: emptyMap()
        val effectChallengeId = wwGameState?.currentChallengeId

        if (!effectIsHost || effectRoundStatus != WhereAndWhenGameState.STATUS_RESULTS) {
            return@LaunchedEffect
        }

        val activePlayerIds = roomPlayers.mapNotNull { it["uid"] as? String }
        if (activePlayerIds.isEmpty() || (roomPlayers.isNotEmpty() && activePlayerIds.size != roomPlayers.size)) {
            Log.d("WW_Host_ResToEnd", "[HOST $myPlayerId] Results: Waiting for all player UIDs. Found ${activePlayerIds.size}/${roomPlayers.size}.")
            return@LaunchedEffect
        }

        val allReadyForLeaderboard = activePlayerIds.all { effectReadyPlayers[it] == true }

        if (allReadyForLeaderboard) {
            Log.i("WW_Host_Flow_ResultsEnd", "[HOST $myPlayerId] All players ready for leaderboard. Transitioning.")
            db.collection("rooms").document(roomCode).update(mapOf(
                "gameState.whereandwhen.roundStatus" to WhereAndWhenGameState.STATUS_SHOWING_LEADERBOARD,
                "gameState.whereandwhen.leaderboardStartTimeMillis" to System.currentTimeMillis()
            )).addOnFailureListener { e -> Log.e("WW_Host_Flow_ResultsEnd", "[HOST $myPlayerId] Error setting status to SHOWING_LEADERBOARD (all ready)", e) }
            return@LaunchedEffect
        }

        if (effectDialogStartTime > 0L) {
            val timeSinceDialogStart = System.currentTimeMillis() - effectDialogStartTime
            val timeRemainingForDialog = RESULTS_DIALOG_TIMEOUT_MS - timeSinceDialogStart

            if (timeRemainingForDialog > 0) {
                Log.d("WW_Host_ResToEnd_Wait", "[HOST $myPlayerId] Results: Not all ready. Waiting for dialog timeout or all ready. Time left: ${timeRemainingForDialog}ms.")
                delay(timeRemainingForDialog)

                val freshWwGameState = wwGameState
                if (freshWwGameState?.roundStatus != WhereAndWhenGameState.STATUS_RESULTS ||
                    freshWwGameState.resultsDialogStartTimeMillis != effectDialogStartTime ||
                    freshWwGameState.currentChallengeId != effectChallengeId) {
                    Log.w("WW_Host_ResToEnd_Stale", "[HOST $myPlayerId] Results: State changed during timeout. Aborting. Status: ${freshWwGameState?.roundStatus}, DialogStart: ${freshWwGameState?.resultsDialogStartTimeMillis}, ChalId: ${freshWwGameState?.currentChallengeId}")
                    return@LaunchedEffect
                }
                val freshReadyPlayers = freshWwGameState.playersReadyForLeaderboard ?: emptyMap()
                if (activePlayerIds.all { freshReadyPlayers[it] == true }) {
                    Log.i("WW_Host_Flow_ResultsEnd", "[HOST $myPlayerId] All players became ready during timeout delay. Transitioning.")
                } else {
                    Log.i("WW_Host_Flow_ResultsEnd", "[HOST $myPlayerId] Results dialog TIMED OUT. Transitioning to Leaderboard.")
                }
            } else {
                Log.i("WW_Host_Flow_ResultsEnd", "[HOST $myPlayerId] Results dialog TIMEOUT condition met on effect entry. Transitioning.")
            }
            db.collection("rooms").document(roomCode).update(mapOf(
                "gameState.whereandwhen.roundStatus" to WhereAndWhenGameState.STATUS_SHOWING_LEADERBOARD,
                "gameState.whereandwhen.leaderboardStartTimeMillis" to System.currentTimeMillis()
            )).addOnFailureListener { e -> Log.e("WW_Host_Flow_ResultsEnd", "[HOST $myPlayerId] Error setting status to SHOWING_LEADERBOARD (timeout/post-delay)", e) }
        } else {
            Log.d("WW_Host_ResToEnd_NoStart", "[HOST $myPlayerId] Results: Dialog start time not set, cannot proceed with timeout.")
        }
    }


    // 4. Leaderboard -> Next Round / End Game (Host only)
    LaunchedEffect(amIHost, wwGameState?.roundStatus, wwGameState?.leaderboardStartTimeMillis, wwGameState?.currentChallengeId) {
        val effectIsHost = amIHost
        val effectRoundStatus = wwGameState?.roundStatus
        val effectLeaderboardStartTime = wwGameState?.leaderboardStartTimeMillis ?: 0L
        val effectChallengeId = wwGameState?.currentChallengeId

        if (!effectIsHost || effectRoundStatus != WhereAndWhenGameState.STATUS_SHOWING_LEADERBOARD) {
            return@LaunchedEffect
        }
        if (effectLeaderboardStartTime == 0L) {
            Log.d("WW_Host_LdrToEnd_NoStart", "[HOST $myPlayerId] Leaderboard: leaderboardStartTimeMillis is 0, waiting.")
            return@LaunchedEffect
        }

        val timeSinceLeaderboardStart = System.currentTimeMillis() - effectLeaderboardStartTime
        val timeRemainingForLeaderboard = LEADERBOARD_DURATION_MS - timeSinceLeaderboardStart

        if (timeRemainingForLeaderboard > 0) {
            Log.d("WW_Host_LdrToEnd_Wait", "[HOST $myPlayerId] Leaderboard: Duration not yet ended. Time left: ${timeRemainingForLeaderboard}ms. Delaying...")
            delay(timeRemainingForLeaderboard)

            val freshWwGameState = wwGameState
            if (freshWwGameState?.roundStatus != WhereAndWhenGameState.STATUS_SHOWING_LEADERBOARD ||
                freshWwGameState.leaderboardStartTimeMillis != effectLeaderboardStartTime ||
                freshWwGameState.currentChallengeId != effectChallengeId) {
                Log.w("WW_Host_LdrToEnd_Stale", "[HOST $myPlayerId] Leaderboard: State changed during delay. Aborting. Status: ${freshWwGameState?.roundStatus}, LdrStart: ${freshWwGameState?.leaderboardStartTimeMillis}, ChalId: ${freshWwGameState?.currentChallengeId}")
                return@LaunchedEffect
            }
        }
        Log.i("WW_Host_Flow_LeaderboardEnd", "[HOST $myPlayerId] Leaderboard duration ended. Proceeding to next round or ending game.")
        val gameStateForProceed = wwGameState
        scope.launch { proceedToNextRoundOrEndGame(db, roomCode, gameStateForProceed, TOTAL_ROUNDS, myPlayerId) }
    }

    // Client side: Update camera for Map Reveal
    LaunchedEffect(showMapRevealUI, wwGameState?.playerGuesses, currentChallenge?.id, mapReady, cameraPositionState) {
        if (showMapRevealUI && currentChallenge != null && mapReady) {
            if (cameraPositionState.isMoving) {
                Log.d("WW_Camera_EFFECT_Reveal", "[Player $myPlayerId] Map Reveal: Camera is already moving, delaying new animation attempt.")
                delay(300)
                if (!isActive) return@LaunchedEffect
            }
            Log.i("WW_Camera_EFFECT_Reveal", "[Player $myPlayerId] Map Reveal: Map is ready. Preparing camera animation. Current Challenge: ${currentChallenge.eventName}")
            val guesses = wwGameState?.playerGuesses ?: emptyMap()
            revealedGuessedLatLngMap = guesses.mapValues { entry ->
                entry.value.lat?.let { lat -> entry.value.lng?.let { lng -> LatLng(lat, lng) } }
            }
            revealedActualLatLng = LatLng(currentChallenge.correctLatitude, currentChallenge.correctLongitude)
            val boundsBuilder = LatLngBounds.builder()
            var pointsIncluded = 0
            revealedActualLatLng?.let { boundsBuilder.include(it); pointsIncluded++; Log.d("WW_Camera_EFFECT_Reveal", "[Player $myPlayerId] Including actual: $it") }
            revealedGuessedLatLngMap.values.filterNotNull().forEach { boundsBuilder.include(it); pointsIncluded++; Log.d("WW_Camera_EFFECT_Reveal", "[Player $myPlayerId] Including guess: $it") }

            if (pointsIncluded > 0) {
                scope.launch {
                    try {
                        val bounds = boundsBuilder.build()
                        Log.i("WW_Camera_EFFECT_Reveal", "[Player $myPlayerId] Animating to bounds: $bounds")
                        cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 100), 1000)
                    } catch (e: IllegalStateException) {
                        Log.e("WW_Camera_EFFECT_Reveal", "[Player $myPlayerId] Map Reveal: Error building LatLngBounds (points: $pointsIncluded): ${e.message}. Falling back.")
                        revealedActualLatLng?.let {
                            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it, 5f), 1000)
                        } ?: cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(20.0,0.0), 1.5f), 1000)
                    }
                }
            } else {
                Log.w("WW_Camera_EFFECT_Reveal", "[Player $myPlayerId] Map Reveal: No valid points to build bounds. Resetting to default.")
                scope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(20.0, 0.0), 1.5f), 1000) }
            }
        }
    }

    LaunchedEffect(showResultsDialogUI) {
        if (showResultsDialogUI) {
            Log.i("WW_Client_UI_Flow", "[Player $myPlayerId] showResultsDialogUI became TRUE. Current status: ${wwGameState?.roundStatus}")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val gameStatus = wwGameState?.roundStatus
                    val challengeIdForTitle = if (gameStatus == WhereAndWhenGameState.STATUS_GUESSING || gameStatus == WhereAndWhenGameState.STATUS_SHOWING_MAP_REVEAL) {
                        wwGameState?.currentChallengeId
                    } else {
                        wwGameState?.roundResults?.challengeId?.takeIf { it.isNotBlank() } ?: wwGameState?.currentChallengeId
                    }
                    val eventNameString = challengeIdForTitle?.let { cid -> gameChallenges.find { it.id == cid }?.eventName } ?: "Loading..."
                    Text(
                        "Where & When | Round ${ (wwGameState?.currentRoundIndex ?: 0) + 1}/${TOTAL_ROUNDS} | $eventNameString",
                        fontFamily = arcadeFontFamily_WhereAndWhen, fontSize = 16.sp, maxLines = 1,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
                    )
                },
                actions = { if (showGuessingUI) { Text("$timeLeftInSeconds", style = TextStyle(fontFamily = arcadeFontFamily_WhereAndWhen, fontSize = 24.sp, color = if (timeLeftInSeconds <= 5 && timeLeftInSeconds % 2 == 0) Color.Red else Color.White, fontWeight = FontWeight.Bold), modifier = Modifier.padding(end = 16.dp)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black.copy(alpha = 0.7f), titleContentColor = Color.White, actionIconContentColor = Color.White)
            )
        }
    ) { paddingValues ->
        val currentWwGameState = wwGameState

        if (currentWwGameState == null || (currentChallenge == null && currentWwGameState.roundStatus == WhereAndWhenGameState.STATUS_GUESSING)) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues).background(Color(0xFF2C3E50)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White); Spacer(Modifier.height(8.dp))
                    Text("Loading Challenge...", color = Color.White, fontFamily = arcadeFontFamily_WhereAndWhen)
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier.padding(paddingValues).fillMaxSize().background(Color(0xFF2C3E50)).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween
        ) {
            key(currentWwGameState.roundStatus, currentWwGameState.currentChallengeId) { // Recompose content when status or challenge changes
                if (showGuessingUI && currentChallenge != null) {
                    Box( modifier = Modifier.weight(2.5f).fillMaxWidth().background(Color.Black, RoundedCornerShape(12.dp)).padding(4.dp), contentAlignment = Alignment.Center ) {
                        Image(painter = painterResource(id = currentChallenge.imageResId), contentDescription = "Event: ${currentChallenge.eventName}", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("Year: ${selectedYear.toInt()}", style = TextStyle(fontFamily = arcadeFontFamily_WhereAndWhen, fontSize = 22.sp, color = Color.White))
                        Slider(value = selectedYear, onValueChange = { selectedYear = it }, valueRange = MIN_SLIDER_YEAR..MAX_SLIDER_YEAR, steps = (MAX_SLIDER_YEAR - MIN_SLIDER_YEAR).toInt() - 1, modifier = Modifier.fillMaxWidth(0.95f), colors = SliderDefaults.colors(thumbColor = Color(0xFFE74C3C), activeTrackColor = Color(0xFFE74C3C).copy(alpha = 0.7f), inactiveTrackColor = Color.Gray.copy(alpha = 0.5f)))
                        Row(modifier = Modifier.fillMaxWidth(0.95f), horizontalArrangement = Arrangement.SpaceBetween) { Text(MIN_SLIDER_YEAR.toInt().toString(), style = TextStyle(fontFamily = arcadeFontFamily_WhereAndWhen, fontSize = 12.sp, color = Color.LightGray)); Text(MAX_SLIDER_YEAR.toInt().toString(), style = TextStyle(fontFamily = arcadeFontFamily_WhereAndWhen, fontSize = 12.sp, color = Color.LightGray)) }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Box( modifier = Modifier.weight(if (showGuessingUI) 3f else 1f).fillMaxWidth().background(Color.DarkGray, RoundedCornerShape(12.dp)).padding(1.dp).clip(RoundedCornerShape(11.dp)) ) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        onMapLoaded = {
                            Log.i("WW_Map_Composable", "onMapLoaded successfully fired from GoogleMap. Player: $myPlayerId")
                            mapReady = true
                        },
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = true, mapToolbarEnabled = false, myLocationButtonEnabled = false,
                            scrollGesturesEnabled = showGuessingUI || showMapRevealUI,
                            zoomGesturesEnabled = showGuessingUI || showMapRevealUI,
                            tiltGesturesEnabled = false
                        ),
                        onMapClick = { latLng -> if (showGuessingUI) { selectedLatLng = latLng; playerGuessMarkerState.position = latLng; playerGuessMarkerState.showInfoWindow() } }
                    ) {
                        if (showGuessingUI && selectedLatLng != null) {
                            Marker(state = playerGuessMarkerState, title = "Your Guess")
                        }
                        if (showMapRevealUI && currentChallenge != null) {
                            revealedActualLatLng?.let { Marker(state = rememberMarkerState(position = it), title = currentChallenge.correctLocationName, icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN), zIndex = 1f) }
                            revealedGuessedLatLngMap.forEach { (pId, guessLatLng) ->
                                guessLatLng?.let {
                                    val pName = roomPlayers.find { it["uid"] == pId }?.get("name") as? String ?: "Guess"
                                    Marker(state = rememberMarkerState(position = it), title = pName, icon = BitmapDescriptorFactory.defaultMarker(if (pId == myPlayerId) BitmapDescriptorFactory.HUE_AZURE else BitmapDescriptorFactory.HUE_RED), zIndex = if (pId == myPlayerId) 2f else 0f)
                                    revealedActualLatLng?.let { actual -> Polyline(points = listOf(it, actual), color = if (pId == myPlayerId) Color.Cyan.copy(alpha=0.7f) else Color.Magenta.copy(alpha = 0.5f), width = 3f) }
                                }
                            }
                        }
                    }
                    if (!mapReady && (showGuessingUI || showMapRevealUI)) { Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally){CircularProgressIndicator(color = Color.White);Spacer(Modifier.height(8.dp));Text("Loading Map...",color=Color.White, fontFamily = arcadeFontFamily_WhereAndWhen)} } }
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (showGuessingUI) {
                    Button(
                        onClick = {
                            if (selectedLatLng == null) { Toast.makeText(context, "Please select a location on the map.", Toast.LENGTH_SHORT).show(); return@Button }
                            hasSubmittedGuessThisRound = true
                            val guessToSubmit = WWPlayerGuess(selectedYear.toInt(), selectedLatLng?.latitude, selectedLatLng?.longitude, true, (GUESSING_PHASE_DURATION_SECONDS - timeLeftInSeconds) * 1000L)
                            Log.i("WW_Submit_Attempt", "[Player $myPlayerId] Attempting to submit guess: $guessToSubmit")
                            db.collection("rooms").document(roomCode).update("gameState.whereandwhen.playerGuesses.$myPlayerId", guessToSubmit)
                                .addOnSuccessListener { Log.i("WW_Submit_Success", "[Player $myPlayerId] Guess successfully submitted.") }
                                .addOnFailureListener { e -> Log.e("WW_Submit_Fail", "[Player $myPlayerId] Error submitting guess.", e); hasSubmittedGuessThisRound = false }
                        },
                        modifier = Modifier.fillMaxWidth(0.8f).height(60.dp), shape = RoundedCornerShape(16.dp),
                        enabled = selectedLatLng != null,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27AE60), contentColor = Color.White, disabledContainerColor = Color.Gray.copy(alpha = 0.5f), disabledContentColor = Color.White.copy(alpha = 0.7f))
                    ) { Text("SUBMIT GUESS", fontSize = 24.sp, fontFamily = arcadeFontFamily_WhereAndWhen, fontWeight = FontWeight.Bold, color = Color.White) }
                }

                if (showWaitingForOthersUI) {
                    Box(modifier = Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                        Text("WAITING FOR OTHER PLAYERS...", fontFamily = arcadeFontFamily_WhereAndWhen, color = Color.Yellow, fontSize = 18.sp)
                    }
                }
            }
        }

        if (showResultsDialogUI && currentWwGameState.roundStatus == WhereAndWhenGameState.STATUS_RESULTS && currentWwGameState.roundResults.results.isNotEmpty() && currentChallenge != null) {
            Log.i("WW_Client_UI_Dialog", "[Player $myPlayerId] Displaying Results Dialog for challenge: ${currentChallenge.eventName}")
            val actualChallengeInfo = currentChallenge
            val allPlayerResultsInfo = currentWwGameState.roundResults.results
            val myResultData = allPlayerResultsInfo[myPlayerId]
            val iWonThisRound = myResultData != null && allPlayerResultsInfo.isNotEmpty() && myResultData.roundScore >= (allPlayerResultsInfo.values.maxOfOrNull { it.roundScore } ?: 0) && myResultData.roundScore > 0
            val dialogBorderColor = if (iWonThisRound) Color(0xFF27AE60) else Color(0xFFE74C3C)

            AlertDialog(
                onDismissRequest = { Log.w("WW_Client_UI_Dialog", "[Player $myPlayerId] Attempted to dismiss Results Dialog (not allowed).") },
                shape = RoundedCornerShape(0.dp),
                containerColor = Color(0xFFDCDCDC),
                titleContentColor = Color.Black,
                textContentColor = Color.Black,
                modifier = Modifier.border(BorderStroke(4.dp, dialogBorderColor)),
                title = { Text("ROUND ${currentWwGameState.currentRoundIndex + 1} RESULTS", fontFamily = arcadeFontFamily_WhereAndWhen, fontSize = 28.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp)) {
                        Text("EVENT: ${actualChallengeInfo.eventName.uppercase()}", fontFamily = arcadeFontFamily_WhereAndWhen, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))
                        Text("ACTUAL: ${actualChallengeInfo.correctYear} - ${actualChallengeInfo.correctLocationName}", fontFamily = arcadeFontFamily_WhereAndWhen, fontSize = 14.sp, textAlign = TextAlign.Center, color = Color.DarkGray, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))
                        Divider(color = Color.Gray, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                        val sortedPlayerResults = allPlayerResultsInfo.entries.sortedByDescending { it.value.roundScore }
                        sortedPlayerResults.forEachIndexed { index, entry ->
                            val pId = entry.key; val result = entry.value
                            val playerName = roomPlayers.find { it["uid"] == pId }?.get("name") as? String ?: "Player"
                            val isCurrentUser = pId == myPlayerId
                            val playerPrefix = if (isCurrentUser) "YOUR" else playerName.uppercase()
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).then(if (isCurrentUser) Modifier.background(Color.Yellow.copy(alpha = 0.15f), RoundedCornerShape(4.dp)).padding(4.dp) else Modifier)) {
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("$playerPrefix SCORE:", fontFamily = arcadeFontFamily_WhereAndWhen, fontSize = 20.sp, fontWeight = if (isCurrentUser) FontWeight.ExtraBold else FontWeight.Bold)
                                    Text("${result.roundScore} PTS", fontFamily = arcadeFontFamily_WhereAndWhen, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = if (result.roundScore > 0) Color(0xFF27AE60) else Color.Black)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth()) { Text("  YEAR: ${result.guessedYear} (ACTUAL: ${actualChallengeInfo.correctYear})", fontFamily = arcadeFontFamily_WhereAndWhen, fontSize = 15.sp, modifier = Modifier.weight(1f)); Text("-> ${result.yearScore} PTS", fontFamily = arcadeFontFamily_WhereAndWhen, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
                                Row(modifier = Modifier.fillMaxWidth()) { val distKmStr = result.distanceKm?.let { "%.0f KM".format(it) } ?: "N/A"; Text("  LOCATION: $distKmStr", fontFamily = arcadeFontFamily_WhereAndWhen, fontSize = 15.sp, modifier = Modifier.weight(1f)); Text("-> ${result.locationScore} PTS", fontFamily = arcadeFontFamily_WhereAndWhen, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
                                if (result.timeRanOut) { Text("  (TIME RAN OUT!)", fontFamily = arcadeFontFamily_WhereAndWhen, color = Color.Red.copy(alpha = 0.9f), fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp)) }
                            }
                            if (index < sortedPlayerResults.size - 1) { Divider(color = Color.LightGray, thickness = 1.dp, modifier = Modifier.padding(vertical = 6.dp)) }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            Log.i("WW_Sync_ResultsAck_Click", "[Player $myPlayerId] Clicked 'Continue' on results dialog.")
                            db.collection("rooms").document(roomCode).update(
                                "gameState.whereandwhen.playersReadyForLeaderboard.$myPlayerId", true
                            ).addOnSuccessListener { Log.i("WW_Sync_ResultsAck_Success", "[Player $myPlayerId] Successfully set playersReadyForLeaderboard to true.") }
                                .addOnFailureListener { e -> Log.e("WW_Sync_ResultsAck_Fail", "[Player $myPlayerId] Failed to set playersReadyForLeaderboard.", e)}
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333), contentColor = Color.White),
                        border = BorderStroke(2.dp, Color.Black)
                    ) { Text("Continue", fontFamily = arcadeFontFamily_WhereAndWhen ) }
                }
            )
        }

        if (showLeaderboardUI && currentWwGameState.roundStatus == WhereAndWhenGameState.STATUS_SHOWING_LEADERBOARD) {
            Log.i("WW_Client_UI_Leaderboard", "[Player $myPlayerId] Displaying Leaderboard.")
            val resultsForLeaderboard = currentWwGameState.roundResults.results
            if (resultsForLeaderboard.isNotEmpty()) {
                RoundLeaderboardScreen(
                    playerResults = resultsForLeaderboard,
                    roomPlayers = roomPlayers,
                    onFinished = {
                        Log.i("WW_Client_Sync_Ldrbrd_Finish", "[Player $myPlayerId] Leaderboard local animation/display time finished.")
                    }
                )
            } else {
                Log.w("WW_Client_UI_Leaderboard", "[Player $myPlayerId] Tried to show leaderboard, but resultsForLeaderboard is empty.")
            }
        }

        if (showFinalResultsDialog) {
            Log.i("WW_Client_UI_Final", "[Player $myPlayerId] Displaying Final Results Dialog.")
            val finalScoresText = roomPlayers.sortedByDescending { (it["totalScore"] as? Long)?.toInt() ?: 0 }.joinToString("\n") { playerMap ->
                val name = playerMap["name"] as? String ?: "Player"; val score = (playerMap["totalScore"] as? Long)?.toInt() ?: 0
                "$name: $score points"
            }
            AlertDialog(
                onDismissRequest = { /* Non-dismissable */ },
                shape = RoundedCornerShape(0.dp),
                containerColor = Color(0xFFECF0F1),
                titleContentColor = Color.Black,
                textContentColor = Color.Black,
                modifier = Modifier.border(BorderStroke(4.dp, Color.DarkGray)),
                title = { Text("GAME OVER!", fontFamily = arcadeFontFamily_WhereAndWhen, fontSize = 30.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), color = Color(0xFFE74C3C)) },
                text = { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Final Standings:", style = TextStyle(fontFamily = arcadeFontFamily_WhereAndWhen, fontSize = 24.sp, fontWeight = FontWeight.Bold)); Text(finalScoresText, textAlign = TextAlign.Center, fontSize = 18.sp, lineHeight = 22.sp, fontFamily = arcadeFontFamily_WhereAndWhen)
                }},
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                if (amIHost) {
                                    Log.i("WW_GameEnd_Host", "[HOST $myPlayerId] Deleting room $roomCode.")
                                    db.collection("rooms").document(roomCode).delete()
                                        .addOnSuccessListener { Log.i("WW_GameEnd_Host", "[HOST $myPlayerId] Successfully deleted room $roomCode.")}
                                        .addOnFailureListener { e -> Log.e("WW_GameEnd_Host", "[HOST $myPlayerId] Error deleting room $roomCode", e)}
                                }
                                Log.i("WW_GameEnd_Nav", "[Player $myPlayerId] Navigating to Lobby Menu.")
                                navController.navigate(NavRoutes.LOBBY_MENU.replace("{gameId}", "whereandwhen")) {
                                    popUpTo(NavRoutes.MAIN_MENU) { inclusive = false }; launchSingleTop = true
                                }
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.Yellow),
                        border = BorderStroke(2.dp, Color.DarkGray)
                    ) { Text("Exit to Lobby", fontFamily = arcadeFontFamily_WhereAndWhen) }
                }
            )
        }
    }
}


@Preview(showBackground = true, device = "id:pixel_5", showSystemUi = true)
@Composable
fun WhereAndWhenScreenPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val navController = rememberNavController()
            WhereAndWhenScreen(
                navController = navController,
                roomCode = "previewRoom",
                currentUserName = "PreviewUser"
            )
        }
    }
}