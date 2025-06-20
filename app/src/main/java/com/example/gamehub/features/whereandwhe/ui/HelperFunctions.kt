package com.example.gamehub.features.whereandwhen.ui

import android.util.Log
import com.example.gamehub.features.whereandwhe.model.Challenge
import com.example.gamehub.features.whereandwhe.model.WWPlayerGuess
import com.example.gamehub.features.whereandwhe.model.WWPlayerRoundResult
import com.example.gamehub.features.whereandwhe.model.WWRoundResultsContainer
import com.example.gamehub.features.whereandwhe.model.WhereAndWhenGameState
import com.example.gamehub.features.whereandwhe.model.gameChallenges
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.abs

/**
 * Calculates the distance in kilometers between two latitude/longitude points using the Haversine formula.
 */
fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadiusKm = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val radLat1 = Math.toRadians(lat1)
    val radLat2 = Math.toRadians(lat2)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(radLat1) * Math.cos(radLat2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return earthRadiusKm * c
}

/**
 * Calculates the player's score for a round based on their guess and the actual challenge values.
 */
fun calculatePlayerScoreForRound(
    playerGuess: WWPlayerGuess?,
    challenge: Challenge,
    timeRanOutForThisPlayer: Boolean
): WWPlayerRoundResult {
    val MAX_YEAR_SCORE = 1000
    val MAX_LOCATION_SCORE = 1000
    val MAX_YEAR_DIFFERENCE_FOR_POINTS = 35
    val MAX_DISTANCE_KM_FOR_POINTS = 5000.0

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

/**
 * Advances the game to the next round or ends the game if all rounds are complete. (Host only)
 *
 * @param db Firestore instance
 * @param roomCode Room code
 * @param currentWwGameState Current game state
 * @param totalRounds Total number of rounds
 * @param myPlayerIdForLog Player ID for logging
 */
suspend fun proceedToNextRoundOrEndGame(
    db: FirebaseFirestore,
    roomCode: String,
    currentWwGameState: WhereAndWhenGameState?,
    totalRounds: Int,
    myPlayerIdForLog: String
) {
    val MAX_YEAR_SCORE = 1000
    val MAX_LOCATION_SCORE = 1000
    val MAX_YEAR_DIFFERENCE_FOR_POINTS = 35
    val MAX_DISTANCE_KM_FOR_POINTS = 5000.0

    Log.i("WW_Host_Proc_Entry", "[HOST $myPlayerIdForLog] Entering proceedToNextRoundOrEndGame. CurrentRound: [${currentWwGameState?.currentRoundIndex}")
    if (currentWwGameState == null) {
        Log.e("WW_Host_Proc_Error", "[HOST $myPlayerIdForLog] Cannot proceed, wwGameState is null.")
        return
    }
    val currentRoundIdx = currentWwGameState.currentRoundIndex
    if (currentRoundIdx + 1 < totalRounds) {
        val nextRoundIdx = currentRoundIdx + 1
        Log.i("WW_Host_Proc_NextRound", "[HOST $myPlayerIdForLog] Starting next round: [${nextRoundIdx + 1}")

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