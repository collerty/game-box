import com.example.gamehub.features.whereandwhe.model.WWPlayerGuess
import com.example.gamehub.features.whereandwhe.model.WhereAndWhenGameState
import kotlinx.coroutines.flow.StateFlow

// IWhereAndWhenRepository.kt

interface IWhereAndWhenRepository {
    // State Flow (Read)
    val gameState: StateFlow<WhereAndWhenGameState?>
    val roomData: StateFlow<Map<String, Any>?> // For room-level data like 'players', 'hostUid'

    // Lifecycle/Setup
    fun joinRoom(roomCode: String)
    fun leaveRoom() // Optional cleanup

    // Write Operations (Player Actions)
    suspend fun submitGuess(playerId: String, guess: WWPlayerGuess)
    suspend fun markPlayerReadyForNextPhase(playerId: String, currentStatus: String)

    // Write Operations (Host Actions - The implementation must handle all the complex logic)
    suspend fun runHostStateTransition(
        roomCode: String,
        activePlayerUids: List<String>,
        totalRounds: Int
    )

    // Initial Host Setup (optional, or merge into joinRoom if host)
    // suspend fun initializeGame(roomCode: String, challengeOrder: List<String>)
}