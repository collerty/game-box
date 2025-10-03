# Where and When Game - Direct Database Calls Research

## Summary
This document identifies all direct Firestore database calls in the UI layer of the "Where and When" game that should be refactored to use the repository pattern.

---

## Files Analyzed

### 1. **WhereAndWhenScreen.kt** (Main UI Screen)
**Location:** `app/src/main/java/com/example/gamehub/features/whereandwhe/ui/WhereAndWhenScreen.kt`

#### Direct Database Calls Found:

1. **Line 89: Firestore Instance Creation**
   ```kotlin
   val db = FirebaseFirestore.getInstance()
   ```
   - **Type:** Initialization
   - **Purpose:** Creates Firestore instance directly in UI

2. **Line 152: Room Document Reference**
   ```kotlin
   val roomDocRef = db.collection("rooms").document(roomCode)
   ```
   - **Type:** Document reference
   - **Purpose:** Gets reference to room document

3. **Line 156: Snapshot Listener (Real-time Updates)**
   ```kotlin
   roomListenerReg = roomDocRef.addSnapshotListener { snapshot, error ->
   ```
   - **Type:** Real-time listener
   - **Purpose:** Listens to room document changes
   - **Data Retrieved:**
     - Room document snapshot (line 159)
     - Game state (`gameState.whereandwhen`) (line 161)
     - Room status (line 196)
   - **Complexity:** High - Contains complex parsing logic for game state

4. **Line 257: Auto-Submit Guess (Timeout)**
   ```kotlin
   db.collection("rooms").document(roomCode).update("gameState.whereandwhen.playerGuesses.$myPlayerId", guessToSubmit)
   ```
   - **Type:** Update operation
   - **Purpose:** Auto-submits player guess when time runs out
   - **Data:** Player guess object

5. **Line 285: Update Round Status to Map Reveal (Host Only)**
   ```kotlin
   db.collection("rooms").document(roomCode).update(mapOf(
       "gameState.whereandwhen.roundStatus" to WhereAndWhenGameState.STATUS_SHOWING_MAP_REVEAL,
       "gameState.whereandwhen.mapRevealStartTimeMillis" to System.currentTimeMillis()
   ))
   ```
   - **Type:** Update operation
   - **Purpose:** Host transitions game to map reveal phase

6. **Line 355: Update Round Results and Status (Host Only)**
   ```kotlin
   db.collection("rooms").document(roomCode).update(mapOf(
       "gameState.whereandwhen.roundResults" to resultsContainer,
       "gameState.whereandwhen.roundStatus" to WhereAndWhenGameState.STATUS_RESULTS,
       "gameState.whereandwhen.resultsDialogStartTimeMillis" to System.currentTimeMillis(),
       "gameState.whereandwhen.playersReadyForLeaderboard" to emptyMap<String, Boolean>(),
       "players" to updatedRoomPlayersData
   ))
   ```
   - **Type:** Update operation
   - **Purpose:** Host posts round results and updates player scores

7. **Line 388: Update to Leaderboard Status - All Ready (Host Only)**
   ```kotlin
   db.collection("rooms").document(roomCode).update(mapOf(
       "gameState.whereandwhen.roundStatus" to WhereAndWhenGameState.STATUS_SHOWING_LEADERBOARD,
       "gameState.whereandwhen.leaderboardStartTimeMillis" to System.currentTimeMillis()
   ))
   ```
   - **Type:** Update operation
   - **Purpose:** Host transitions to leaderboard when all players ready

8. **Line 419: Update to Leaderboard Status - Timeout (Host Only)**
   ```kotlin
   db.collection("rooms").document(roomCode).update(mapOf(
       "gameState.whereandwhen.roundStatus" to WhereAndWhenGameState.STATUS_SHOWING_LEADERBOARD,
       "gameState.whereandwhen.leaderboardStartTimeMillis" to System.currentTimeMillis()
   ))
   ```
   - **Type:** Update operation
   - **Purpose:** Host transitions to leaderboard on timeout

9. **Line 602: Submit Player Guess (Manual)**
   ```kotlin
   db.collection("rooms").document(roomCode).update("gameState.whereandwhen.playerGuesses.$myPlayerId", guessToSubmit)
   ```
   - **Type:** Update operation
   - **Purpose:** Player manually submits their guess

10. **Line 632: Mark Player Ready for Leaderboard**
    ```kotlin
    db.collection("rooms").document(roomCode).update(
        "gameState.whereandwhen.playersReadyForLeaderboard.$myPlayerId", true
    )
    ```
    - **Type:** Update operation
    - **Purpose:** Player acknowledges results dialog

11. **Line 679: Delete Room (Host Only - Game End)**
    ```kotlin
    db.collection("rooms").document(roomCode).delete()
    ```
    - **Type:** Delete operation
    - **Purpose:** Host deletes room when game ends

---

### 2. **HelperFunctions.kt** (Game Logic Helpers)
**Location:** `app/src/main/java/com/example/gamehub/features/whereandwhe/ui/HelperFunctions.kt`

#### Direct Database Calls Found:

1. **Line 131: Update for Next Round (Host Only)**
   ```kotlin
   db.collection("rooms").document(roomCode).update(updates)
   ```
   - **Type:** Update operation
   - **Purpose:** Host starts next round
   - **Data Updated:**
     - currentRoundIndex
     - currentChallengeId
     - roundStartTimeMillis
     - roundStatus
     - playerGuesses (reset)
     - roundResults (reset)
     - Various timestamp fields
     - Player ready states (reset)

2. **Line 136: Update Game Status to Ended (Host Only)**
   ```kotlin
   db.collection("rooms").document(roomCode).update("status", "ended")
   ```
   - **Type:** Update operation
   - **Purpose:** Host marks game as ended after all rounds

---

### 3. **RoundResultsDialog.kt** (Results Dialog Component)
**Location:** `app/src/main/java/com/example/gamehub/features/whereandwhe/ui/components/RoundResultsDialog.kt`

#### Direct Database Calls Found:
- **None** - This component only displays data and uses callbacks

---

### 4. **RoundLeaderboardScreen.kt** (Leaderboard Component)
**Location:** `app/src/main/java/com/example/gamehub/features/whereandwhe/ui/RoundLeaderboardScreen.kt`

#### Direct Database Calls Found:
- **None** - This component only displays data and uses callbacks

---

## Database Operations Summary

### Read Operations
1. **Real-time Listener** (1 instance)
   - Room document snapshot listener
   - Retrieves entire game state and room data
   - Active throughout game session

### Write Operations
1. **Update Operations** (9 instances)
   - Player guess submissions (2 types: manual + auto)
   - Game state transitions (4 types: guessing→map reveal, map reveal→results, results→leaderboard x2)
   - Round results posting
   - Player ready acknowledgments
   - Next round initialization
   - Game end status

2. **Delete Operations** (1 instance)
   - Room deletion on game end

---

## Recommended Repository Methods

Based on the analysis, the `WhereAndWhenRepository` should implement:

### Read Methods
```kotlin
// Real-time listeners
fun observeRoomUpdates(roomCode: String): Flow<RoomSnapshot>
fun observeGameState(roomCode: String): Flow<WhereAndWhenGameState?>

// One-time reads (if needed)
suspend fun getRoomData(roomCode: String): RoomData?
suspend fun getGameState(roomCode: String): WhereAndWhenGameState?
```

### Write Methods
```kotlin
// Player actions
suspend fun submitPlayerGuess(roomCode: String, playerId: String, guess: WWPlayerGuess): Result<Unit>
suspend fun markPlayerReadyForLeaderboard(roomCode: String, playerId: String): Result<Unit>

// Host actions - State transitions
suspend fun transitionToMapReveal(roomCode: String): Result<Unit>
suspend fun transitionToResults(roomCode: String, results: WWRoundResultsContainer, updatedPlayers: List<Map<String, Any>>): Result<Unit>
suspend fun transitionToLeaderboard(roomCode: String): Result<Unit>

// Host actions - Round management
suspend fun startNextRound(roomCode: String, nextRoundData: NextRoundData): Result<Unit>
suspend fun endGame(roomCode: String): Result<Unit>
suspend fun deleteRoom(roomCode: String): Result<Unit>
```

---

## Refactoring Priority

### High Priority (Core Game Flow)
1. ✅ Real-time listener for game state (Line 156 in WhereAndWhenScreen.kt)
2. ✅ Submit player guess operations (Lines 257, 602)
3. ✅ Host state transition operations (Lines 285, 355, 388, 419)

### Medium Priority (Round Management)
4. ✅ Next round initialization (Line 131 in HelperFunctions.kt)
5. ✅ Game end operations (Lines 136, 679)

### Low Priority (Player Acknowledgments)
6. ✅ Player ready acknowledgments (Line 632)

---

## Additional Notes

### Current Repository Status
The `WhereAndWhenRepository.kt` file exists but is empty (only contains a TODO comment).

### Architecture Concerns
1. **UI contains business logic**: State transition logic is embedded in UI composables
2. **Direct Firestore dependency**: UI layer directly depends on Firebase SDK
3. **Complex parsing in UI**: Game state parsing happens in the snapshot listener
4. **No error handling abstraction**: Each DB call handles errors independently
5. **Testing difficulty**: Direct DB calls make UI testing challenging

### Benefits of Repository Pattern
1. **Separation of concerns**: Move data operations out of UI
2. **Testability**: Easy to mock repository for UI tests
3. **Error handling**: Centralized error handling and retry logic
4. **Type safety**: Repository can provide strongly-typed interfaces
5. **Flexibility**: Easy to swap data sources (e.g., local cache, different backend)

---

## Files That Need Modification

1. ✅ `WhereAndWhenScreen.kt` - Remove all direct DB calls, inject repository
2. ✅ `HelperFunctions.kt` - Move to repository or use repository methods
3. ✅ `WhereAndWhenRepository.kt` - Implement all repository methods
4. ⚠️ Consider creating a ViewModel to handle business logic between UI and Repository

---

*Research completed: 2025-10-03*
