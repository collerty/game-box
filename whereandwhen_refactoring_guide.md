# Where and When - Repository Pattern Refactoring Guide

## Status: Repository Implementation Complete ✅

The `WhereAndWhenRepository` has been successfully implemented with all necessary methods.

## Completed Changes:

### 1. ✅ WhereAndWhenRepository.kt
- Implemented all CRUD operations
- Added listener for real-time updates
- Methods: `listenToRoomState`, `submitPlayerGuess`, `updateGameState`, `transitionToMapReveal`, `transitionToLeaderboard`, `markPlayerReadyForLeaderboard`, `deleteRoom`

### 2. ✅ HelperFunctions.kt
- Changed `proceedToNextRoundOrEndGame` to accept `WhereAndWhenRepository` instead of `FirebaseFirestore`
- Updated database calls to use `repository.updateGameState()`

## Remaining Changes Needed in WhereAndWhenScreen.kt:

### Step 1: Update Imports
**Line 53:** Remove `import com.google.firebase.firestore.FirebaseFirestore`
**Add:** `import com.example.gamehub.repository.implementations.WhereAndWhenRepository`

### Step 2: Replace Firestore Instance with Repository
**Line 89:** Replace:
```kotlin
val db = FirebaseFirestore.getInstance()
```
With:
```kotlin
val repository = remember { WhereAndWhenRepository() }
```

### Step 3: Update Snapshot Listener (Lines 150-216)
**Current (Line 152):**
```kotlin
val roomDocRef = db.collection("rooms").document(roomCode)
```
**Replace with:**
```kotlin
// Remove roomDocRef line
```

**Current (Line 156):**
```kotlin
roomListenerReg = roomDocRef.addSnapshotListener { snapshot, error ->
    if (error != null) { Log.e("WW_Listener_Error", "Room listen error for $roomCode", error); return@addSnapshotListener }
    if (snapshot != null && snapshot.exists()) {
        roomDocSnapshot = snapshot.data
```
**Replace with:**
```kotlin
roomListenerReg = repository.listenToRoomState(
    roomCode = roomCode,
    onDataChange = { snapshot ->
        if (snapshot.isNotEmpty()) {
            roomDocSnapshot = snapshot
```

**Current (Line 208):**
```kotlin
                    }
                }
            }
```
**Replace with:**
```kotlin
                    }
                },
                onError = { error -> Log.e("WW_Listener_Error", "Room listen error for $roomCode", error) }
            )
```

### Step 4: Update Auto-Submit Guess (Line 257)
**Current:**
```kotlin
db.collection("rooms").document(roomCode).update("gameState.whereandwhen.playerGuesses.$myPlayerId", guessToSubmit)
    .addOnSuccessListener { Log.i("WW_Submit", "[Player $myPlayerId] Auto-submitted guess (timeout).") }
    .addOnFailureListener { e -> Log.e("WW_Submit", "[Player $myPlayerId] Error auto-submitting guess (timeout)", e); hasSubmittedGuessThisRound = false }
```
**Replace with:**
```kotlin
repository.submitPlayerGuess(
    roomCode = roomCode,
    playerId = myPlayerId,
    guess = guessToSubmit,
    onSuccess = { Log.i("WW_Submit", "[Player $myPlayerId] Auto-submitted guess (timeout).") },
    onError = { e -> Log.e("WW_Submit", "[Player $myPlayerId] Error auto-submitting guess (timeout)", e); hasSubmittedGuessThisRound = false }
)
```

### Step 5: Update Transition to Map Reveal (Line 285)
**Current:**
```kotlin
db.collection("rooms").document(roomCode).update(mapOf(
    "gameState.whereandwhen.roundStatus" to WhereAndWhenGameState.STATUS_SHOWING_MAP_REVEAL,
    "gameState.whereandwhen.mapRevealStartTimeMillis" to System.currentTimeMillis()
)).addOnFailureListener { e -> Log.e("WW_Host_Flow_GuessEnd", "[HOST $myPlayerId] Error setting status to SHOWING_MAP_REVEAL", e) }
```
**Replace with:**
```kotlin
repository.updateGameState(
    roomCode = roomCode,
    updates = mapOf(
        "gameState.whereandwhen.roundStatus" to WhereAndWhenGameState.STATUS_SHOWING_MAP_REVEAL,
        "gameState.whereandwhen.mapRevealStartTimeMillis" to System.currentTimeMillis()
    ),
    onSuccess = {},
    onError = { e -> Log.e("WW_Host_Flow_GuessEnd", "[HOST $myPlayerId] Error setting status to SHOWING_MAP_REVEAL", e) }
)
```

### Step 6: Update Post Results (Line 355)
**Current:**
```kotlin
db.collection("rooms").document(roomCode).update(mapOf(
    "gameState.whereandwhen.roundResults" to resultsContainer,
    "gameState.whereandwhen.roundStatus" to WhereAndWhenGameState.STATUS_RESULTS,
    "gameState.whereandwhen.resultsDialogStartTimeMillis" to System.currentTimeMillis(),
    "gameState.whereandwhen.playersReadyForLeaderboard" to emptyMap<String, Boolean>(),
    "players" to updatedRoomPlayersData
)).addOnSuccessListener {
    Log.i("WW_Host_Flow_MapRevealEnd", "[HOST $myPlayerId] Successfully posted round results & updated state to RESULTS for challenge ${currentChallengeAfterDelay.id}")
}.addOnFailureListener { e -> Log.e("WW_Host_Flow_MapRevealEnd", "[HOST $myPlayerId] Error posting round results for challenge ${currentChallengeAfterDelay.id}", e) }
```
**Replace with:**
```kotlin
repository.transitionToMapReveal(
    roomCode = roomCode,
    resultsContainer = resultsContainer,
    updatedPlayers = updatedRoomPlayersData,
    onSuccess = { Log.i("WW_Host_Flow_MapRevealEnd", "[HOST $myPlayerId] Successfully posted round results & updated state to RESULTS for challenge ${currentChallengeAfterDelay.id}") },
    onError = { e -> Log.e("WW_Host_Flow_MapRevealEnd", "[HOST $myPlayerId] Error posting round results for challenge ${currentChallengeAfterDelay.id}", e) }
)
```

### Step 7: Update Transition to Leaderboard - All Ready (Line 388)
**Current:**
```kotlin
db.collection("rooms").document(roomCode).update(mapOf(
    "gameState.whereandwhen.roundStatus" to WhereAndWhenGameState.STATUS_SHOWING_LEADERBOARD,
    "gameState.whereandwhen.leaderboardStartTimeMillis" to System.currentTimeMillis()
)).addOnFailureListener { e -> Log.e("WW_Host_Flow_ResultsEnd", "[HOST $myPlayerId] Error setting status to SHOWING_LEADERBOARD (all ready)", e) }
```
**Replace with:**
```kotlin
repository.transitionToLeaderboard(
    roomCode = roomCode,
    onSuccess = {},
    onError = { e -> Log.e("WW_Host_Flow_ResultsEnd", "[HOST $myPlayerId] Error setting status to SHOWING_LEADERBOARD (all ready)", e) }
)
```

### Step 8: Update Transition to Leaderboard - Timeout (Line 419)
**Current:**
```kotlin
db.collection("rooms").document(roomCode).update(mapOf(
    "gameState.whereandwhen.roundStatus" to WhereAndWhenGameState.STATUS_SHOWING_LEADERBOARD,
    "gameState.whereandwhen.leaderboardStartTimeMillis" to System.currentTimeMillis()
)).addOnFailureListener { e -> Log.e("WW_Host_Flow_ResultsEnd", "[HOST $myPlayerId] Error setting status to SHOWING_LEADERBOARD (timeout/post-delay)", e) }
```
**Replace with:**
```kotlin
repository.transitionToLeaderboard(
    roomCode = roomCode,
    onSuccess = {},
    onError = { e -> Log.e("WW_Host_Flow_ResultsEnd", "[HOST $myPlayerId] Error setting status to SHOWING_LEADERBOARD (timeout/post-delay)", e) }
)
```

### Step 9: Update Proceed to Next Round (Line 461)
**Current:**
```kotlin
scope.launch { proceedToNextRoundOrEndGame(db, roomCode, gameStateForProceed, TOTAL_ROUNDS, myPlayerId) }
```
**Replace with:**
```kotlin
scope.launch { proceedToNextRoundOrEndGame(repository, roomCode, gameStateForProceed, TOTAL_ROUNDS, myPlayerId) }
```

### Step 10: Update Manual Guess Submit (Line 602)
**Current:**
```kotlin
db.collection("rooms").document(roomCode).update("gameState.whereandwhen.playerGuesses.$myPlayerId", guessToSubmit)
    .addOnSuccessListener { Log.i("WW_Submit_Success", "[Player $myPlayerId] Guess successfully submitted.") }
    .addOnFailureListener { e -> Log.e("WW_Submit_Fail", "[Player $myPlayerId] Error submitting guess.", e); hasSubmittedGuessThisRound = false }
```
**Replace with:**
```kotlin
repository.submitPlayerGuess(
    roomCode = roomCode,
    playerId = myPlayerId,
    guess = guessToSubmit,
    onSuccess = { Log.i("WW_Submit_Success", "[Player $myPlayerId] Guess successfully submitted.") },
    onError = { e -> Log.e("WW_Submit_Fail", "[Player $myPlayerId] Error submitting guess.", e); hasSubmittedGuessThisRound = false }
)
```

### Step 11: Update Mark Player Ready (Line 632)
**Current:**
```kotlin
db.collection("rooms").document(roomCode).update(
    "gameState.whereandwhen.playersReadyForLeaderboard.$myPlayerId", true
).addOnSuccessListener { Log.i("WW_Sync_ResultsAck_Success", "[Player $myPlayerId] Successfully set playersReadyForLeaderboard to true.") }
    .addOnFailureListener { e -> Log.e("WW_Sync_ResultsAck_Fail", "[Player $myPlayerId] Failed to set playersReadyForLeaderboard.", e)}
```
**Replace with:**
```kotlin
repository.markPlayerReadyForLeaderboard(
    roomCode = roomCode,
    playerId = myPlayerId,
    onSuccess = { Log.i("WW_Sync_ResultsAck_Success", "[Player $myPlayerId] Successfully set playersReadyForLeaderboard to true.") },
    onError = { e -> Log.e("WW_Sync_ResultsAck_Fail", "[Player $myPlayerId] Failed to set playersReadyForLeaderboard.", e) }
)
```

### Step 12: Update Delete Room (Line 679)
**Current:**
```kotlin
db.collection("rooms").document(roomCode).delete()
    .addOnSuccessListener { Log.i("WW_GameEnd_Host", "[HOST $myPlayerId] Successfully deleted room $roomCode.")}
    .addOnFailureListener { e -> Log.e("WW_GameEnd_Host", "[HOST $myPlayerId] Error deleting room $roomCode", e)}
```
**Replace with:**
```kotlin
repository.deleteRoom(
    roomCode = roomCode,
    onSuccess = { Log.i("WW_GameEnd_Host", "[HOST $myPlayerId] Successfully deleted room $roomCode.") },
    onError = { e -> Log.e("WW_GameEnd_Host", "[HOST $myPlayerId] Error deleting room $roomCode", e) }
)
```

## Summary

**Total Changes Required in WhereAndWhenScreen.kt: 12 replacements**

All database operations have been abstracted into the repository layer, following the same pattern as OhPardon and Codenames games.

## Benefits Achieved:
- ✅ Separation of concerns - UI no longer depends on Firebase directly
- ✅ Testability - Repository can be mocked for testing
- ✅ Consistency - Follows same pattern as other games
- ✅ Maintainability - Database logic centralized in one place
