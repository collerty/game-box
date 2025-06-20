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
import com.example.gamehub.features.whereandwhe.model.Challenge
import com.example.gamehub.features.whereandwhe.model.WWPlayerGuess
import com.example.gamehub.features.whereandwhe.model.WWPlayerRoundResult
import com.example.gamehub.features.whereandwhe.model.WWRoundResultsContainer
import com.example.gamehub.features.whereandwhe.model.WhereAndWhenGameState
import com.example.gamehub.features.whereandwhe.model.gameChallenges
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import com.example.gamehub.features.whereandwhen.ui.components.RoundResultsDialog
// --- Font ---
val arcadeFontFamily_WhereAndWhen = FontFamily(
    Font(R.font.arcade_classic, FontWeight.Normal)
)
val Gold = Color(0xFFFFD700)

private const val GUESSING_PHASE_DURATION_SECONDS = 35
private const val MAP_REVEAL_DURATION_MS = 6000L // 6 seconds
private const val RESULTS_DIALOG_TIMEOUT_MS = 15000L // 15 seconds for players to click "Continue"
private const val LEADERBOARD_DURATION_MS = 5000L // 5 seconds for leaderboard display
private const val MIN_SLIDER_YEAR = 1850f
private const val MAX_SLIDER_YEAR = 2024f
private val TOTAL_ROUNDS = 5

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
            RoundResultsDialog(
                show = true,
                currentRoundIndex = currentWwGameState.currentRoundIndex,
                currentChallengeName = currentChallenge.eventName,
                currentChallengeYear = currentChallenge.correctYear,
                currentChallengeLocation = currentChallenge.correctLocationName,
                allPlayerResultsInfo = currentWwGameState.roundResults.results,
                myPlayerId = myPlayerId,
                roomPlayers = roomPlayers,
                onContinue = {
                            Log.i("WW_Sync_ResultsAck_Click", "[Player $myPlayerId] Clicked 'Continue' on results dialog.")
                            db.collection("rooms").document(roomCode).update(
                                "gameState.whereandwhen.playersReadyForLeaderboard.$myPlayerId", true
                            ).addOnSuccessListener { Log.i("WW_Sync_ResultsAck_Success", "[Player $myPlayerId] Successfully set playersReadyForLeaderboard to true.") }
                                .addOnFailureListener { e -> Log.e("WW_Sync_ResultsAck_Fail", "[Player $myPlayerId] Failed to set playersReadyForLeaderboard.", e)}
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