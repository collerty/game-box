package com.example.gamehub.features.ohpardon

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import com.example.gamehub.features.ohpardon.ui.BoardCell
import com.example.gamehub.features.ohpardon.ui.CellType
import com.example.gamehub.features.ohpardon.ui.PawnForUI
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

data class GameRoom(
    val gameId: String,
    val name: String,
    val hostUid: String,
    val hostName: String,
    val passwordHash: Int?,
    val maxPlayers: Int,
    val status: String = "waiting", // waiting, playing, finished
    val players: List<Player>,
    val gameState: GameState,
    val createdAt: Timestamp?
)

data class ColorConfig(
    val homePositions: List<Pair<Int, Int>>,
    val goalPath: List<Pair<Int, Int>>,
    val entryCell: Pair<Int, Int>
)


data class GameState(
    val currentTurnUid: String,
    val diceRoll: Int? = null,
    val gameResult: String = "Ongoing"
)

data class Player(
    val uid: String,
    val name: String,
    val color: Color,
    val pawns: List<Pawn>
)


data class Pawn(
    val id: String,
    var position: Int = -1 // -1 means at home
)

val colorConfigs = mapOf(
    Color.Red to ColorConfig(
        homePositions = listOf(0 to 0, 1 to 0, 0 to 1, 1 to 1),
        goalPath = listOf(1 to 5, 2 to 5, 3 to 5, 4 to 5),
        entryCell = 0 to 4
    ),
    Color.Blue to ColorConfig(
        homePositions = listOf(9 to 0, 10 to 0, 9 to 1, 10 to 1),
        goalPath = listOf(5 to 1, 5 to 2, 5 to 3, 5 to 4),
        entryCell = 6 to 0
    ),
    Color.Green to ColorConfig(
        homePositions = listOf(9 to 10, 10 to 10, 9 to 9, 10 to 9),
        goalPath = listOf(9 to 5, 8 to 5, 7 to 5, 6 to 5),
        entryCell = 10 to 6
    ),
    Color.Yellow to ColorConfig(
        homePositions = listOf(0 to 10, 0 to 9, 1 to 10, 1 to 9),
        goalPath = listOf(5 to 9, 5 to 8, 5 to 7, 5 to 6),
        entryCell = 4 to 10
    )
)

// Define shared path
val nonColoredPath = listOf(
    Pair(1, 4), Pair(2, 4), Pair(3, 4), Pair(4, 4),
    Pair(4, 3), Pair(4, 2), Pair(4, 1), Pair(4, 0),
    Pair(5, 0), Pair(6, 1), Pair(6, 2), Pair(6, 3),
    Pair(6, 4), Pair(7, 4), Pair(8, 4), Pair(9, 4),
    Pair(10, 4), Pair(10, 5), Pair(9, 6), Pair(8, 6),
    Pair(7, 6), Pair(6, 6), Pair(6, 7), Pair(6, 8),
    Pair(6, 9), Pair(6, 10), Pair(5, 10), Pair(4, 9),
    Pair(4, 8), Pair(4, 7), Pair(4, 6), Pair(3, 6),
    Pair(2, 6), Pair(1, 6), Pair(0, 6), Pair(0, 5)
)

class OhPardonViewModel(
    application: Application,
    private val roomCode: String,
    private val currentUserName: String
) : AndroidViewModel(application), SensorEventListener {

    private val _gameRoom = MutableStateFlow<GameRoom?>(null)
    val gameRoom: StateFlow<GameRoom?> = _gameRoom.asStateFlow()

    private val firestore = FirebaseFirestore.getInstance()
    private var listenerRegistration: ListenerRegistration? = null

    private val sensorManager =
        application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var lastShakeTime = 0L
    private val SHAKE_THRESHOLD_GRAVITY = 2.7
    private val SHAKE_SLOP_TIME_MS = 500

    fun rollDice(): Int {
        return (1..6).random()
    }

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val gForce = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH

            if (gForce > SHAKE_THRESHOLD_GRAVITY) {
                val now = System.currentTimeMillis()
                if (now - lastShakeTime > SHAKE_SLOP_TIME_MS) {
                    lastShakeTime = now
                    val result = rollDice()
                    attemptRollDice(currentUserName)
                }
            }
        }
    }

    fun attemptRollDice(currentUserName: String) {
        val currentGame = _gameRoom.value ?: return
        val gameState = currentGame.gameState
        val currentPlayer = currentGame.players.find { it.name == currentUserName }


        // 1. Check if it's the user's turn
        if (gameState.currentTurnUid != currentPlayer?.uid) {
            Log.w("OhPardonVM", "Not your turn to roll dice")
            return
        }

        // 2. Check if dice already rolled this turn
        if (gameState.diceRoll != null) {
            Log.w("OhPardonVM", "Dice already rolled")
            return
        }

        // 3. Roll dice
        val diceRoll = rollDice()

        // 4. Update Firestore with new dice roll
        firestore.collection("rooms").document(roomCode)
            .update("gameState.ohpardon.diceRoll", diceRoll)
            .addOnSuccessListener {
                Log.d("OhPardonVM", "Dice rolled: $diceRoll")
            }
            .addOnFailureListener {
                Log.e("OhPardonVM", "Failed to update dice roll", it)
            }
    }


    fun registerShakeListener() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
    }

    fun unregisterShakeListener() {
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }


    init {
        listenToRoomChanges()
    }

    private fun listenToRoomChanges() {
        listenerRegistration = firestore.collection("rooms")
            .document(roomCode)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("OhPardonVM", "Firestore error", e)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val data = snapshot.data
                    if (data != null) {
                        _gameRoom.value = parseGameRoom(data)
                    }
                }
            }
    }

    fun parseColor(colorStr: String): Color {
        return when (colorStr.lowercase()) {
            "red" -> Color.Red
            "green" -> Color.Green
            "blue" -> Color.Blue
            "yellow" -> Color.Yellow
            else -> Color.Red
        }
    }

    fun parsePlayer(data: Map<String, Any?>): Player {
        val pawnsMap = data["pawns"] as? Map<String, Long> ?: emptyMap()

        val pawns = pawnsMap.map { (key, position) ->
            Pawn(id = key, position = position.toInt())
        }

        return Player(
            uid = data["uid"] as? String ?: "",
            name = data["name"] as? String ?: "",
            color = parseColor(
                data["color"] as? String ?: "red"
            ), // Assume color is stored as string
            pawns = pawns
        )
    }

    fun parseGameState(data: Map<String, Any?>?): GameState {
        val currentPlayer = (data?.get("currentPlayer") as? String)
            ?: (data?.get("gameState.ohpardon.currentPlayer") as? String)
            ?: ""

        Log.d("OhPardonVM", "Parsing GameState. currentPlayer: $currentPlayer")

        return GameState(
            currentTurnUid = currentPlayer,
            diceRoll = (data?.get("diceRoll") as? Long)?.toInt(),
            gameResult = data?.get("gameResult") as? String ?: "Ongoing"
        )
    }


    fun parseGameRoom(data: Map<String, Any?>): GameRoom {
        val playersList = (data["players"] as? List<Map<String, Any?>>)?.map { playerData ->
            parsePlayer(playerData)
        } ?: emptyList()

        val gameStateMap = data["gameState"] as? Map<String, Any?>
        val ohPardonState = gameStateMap?.get("ohpardon") as? Map<String, Any?>

        return GameRoom(
            gameId = data["gameId"] as? String ?: "",
            name = data["name"] as? String ?: "",
            hostUid = data["hostUid"] as? String ?: "",
            hostName = data["hostName"] as? String ?: "",
            passwordHash = (data["password"] as? Long)?.toInt(),
            maxPlayers = (data["maxPlayers"] as? Long)?.toInt() ?: 4,
            status = data["status"] as? String ?: "waiting",
            players = playersList,
            gameState = parseGameState(ohPardonState),
            createdAt = data["createdAt"] as? Timestamp
        )
    }

    fun skipTurn(currentUserName: String) {
        val currentGame = _gameRoom.value ?: return
        val gameState = currentGame.gameState
        val currentPlayer = currentGame.players.find { it.name == currentUserName }

        if (currentPlayer?.uid == gameState.currentTurnUid) {
            val updates = mapOf(
                "gameState.ohpardon.diceRoll" to null,
                "gameState.ohpardon.currentPlayer" to getNextPlayerUid(
                    currentGame,
                    currentPlayer.uid
                )
            )
            firestore.collection("rooms")
                .document(roomCode)
                .update(updates)
            return
        } else {
            _toastMessage.value = "Not allowed to skip another player's turn!"
            return
        }
    }

    fun attemptMovePawn(currentUserUid: String, pawnId: String) {
        val currentGame = _gameRoom.value ?: return
        val gameState = currentGame.gameState

        if (gameState.currentTurnUid != currentUserUid) {
            _toastMessage.value = "Not your turn!"
            return
        }

        val diceRoll = gameState.diceRoll
        if (diceRoll == null) {
            _toastMessage.value = "Roll the dice first!"
            return
        }

        val playerIndex = currentGame.players.indexOfFirst { it.uid == currentUserUid }
        val player = currentGame.players.getOrNull(playerIndex)
        val pawn = player?.pawns?.find { it.id == "pawn$pawnId" }

        if (player == null || pawn == null) {
            Log.e("OhPardonVM", "Player or Pawn not found")
            return
        }

        fun isOwnPawnBlocking(
            newPosition: Int,
            pawnId: String,
            player: Player
        ): Boolean {
            return player.pawns.any { it.id != "pawn$pawnId" && it.position == newPosition }
        }

        fun getBoardOffsetForPlayer(color: Color): Int = when (color) {
            Color.Red -> 0
            Color.Green -> 20
            Color.Yellow -> 30
            Color.Blue -> 10
            else -> 0
        }

        val playerOffset = getBoardOffsetForPlayer(player.color)
        val currentPos = pawn.position

        val victoryRange = 40..43

        fun isProtectedStartCellBlocked(
            targetPosition: Int,
            currentUserUid: String,
            game: GameRoom
        ): Boolean {
            return game.players.any { opponent ->
                if (opponent.uid == currentUserUid) return@any false
                val opponentStartCell = getBoardOffsetForPlayer(opponent.color)
                val isProtected = targetPosition == opponentStartCell
                val hasPawnThere = opponent.pawns.any { it.position == targetPosition }
                isProtected && hasPawnThere && targetPosition < 40
            }
        }

        //Move logic
        val newPosition = when {
            currentPos == -1 && diceRoll == 6 -> playerOffset

            currentPos in 0 until 40 -> {
                val stepsTaken = (currentPos - playerOffset + 40) % 40
                val totalSteps = stepsTaken + diceRoll

                if (totalSteps >= 40) {
                    val victoryCell = 40 + (totalSteps - 40)
                    if (victoryCell > 43) {
                        _toastMessage.value = "Invalid move!"
                        return
                    }
                    victoryCell
                } else {
                    (playerOffset + totalSteps) % 40
                }
            }

            currentPos in victoryRange -> {
                val newVictoryPos = currentPos + diceRoll
                if (newVictoryPos > 43) {
                    _toastMessage.value = "Invalid move!"
                    return
                }
                newVictoryPos
            }

            else -> {
                _toastMessage.value = "Invalid move!"
                return
            }
        }


        val isBlockedByOpponent =
            isProtectedStartCellBlocked(newPosition, currentUserUid, currentGame)
        val isBlockedBySelf = isOwnPawnBlocking(newPosition, pawnId, player)

        val isMoveBlocked = isBlockedByOpponent || isBlockedBySelf


        if (isMoveBlocked) {
            val message = when {
                isBlockedByOpponent -> "Can't move to opponent's protected start cell."
                isBlockedBySelf -> "Can't move to a tile occupied by your own pawn."
                else -> "Invalid move."
            }
            _toastMessage.value = message
            return
        }


        //Capture logic
        val updatedPlayers = currentGame.players.map { p ->
            if (p.uid == currentUserUid) {
                val updatedPawns = p.pawns.map {
                    if (it.id == "pawn$pawnId") {
                        if (newPosition == getBoardOffsetForPlayer(p.color)) it.copy(position = newPosition - getBoardOffsetForPlayer(p.color)) else it.copy(position = newPosition)
                    } else it
                }
                p.copy(pawns = updatedPawns)
            } else {
                val updatedPawns = p.pawns.map {
                    val opponentStartCell = getBoardOffsetForPlayer(p.color)
                    val isProtected = it.position == opponentStartCell
                    if (it.position == newPosition && !isProtected && newPosition < 40) {
                        Log.d("OhPardonVM", "Captured pawn ${it.id} from ${p.uid}")
                        it.copy(position = -1)
                    } else it
                }

                p.copy(pawns = updatedPawns)
            }
        }

        val updatedPlayerMaps = updatedPlayers.map { p ->
            mapOf(
                "uid" to p.uid,
                "name" to p.name,
                "color" to when (p.color) {
                    Color.Red -> "red"
                    Color.Green -> "green"
                    Color.Blue -> "blue"
                    Color.Yellow -> "yellow"
                    else -> "red"
                },
                "pawns" to p.pawns.associate { it.id to it.position }
            )
        }

        val nextPlayerUid = getNextPlayerUid(currentGame, currentUserUid)
        _gameRoom.value = currentGame.copy(
            gameState = currentGame.gameState.copy(
                currentTurnUid = nextPlayerUid,
                diceRoll = null
            )
        )

        val winningPlayer = updatedPlayers.find { player ->
            val victoryPositions = setOf(40, 41, 42, 43)
            player.pawns.map { it.position }.toSet() == victoryPositions
        }

        if (winningPlayer != null) {

            // Update game state to finished
            val updates = mapOf(
                "players" to updatedPlayerMaps,
                "gameState.ohpardon.diceRoll" to null,
                "gameState.ohpardon.currentPlayer" to nextPlayerUid,
                "gameState.ohpardon.gameResult" to "${winningPlayer.name} wins!",
                "status" to "over"
            )

            firestore.collection("rooms")
                .document(roomCode)
                .update(updates)
                .addOnSuccessListener {
                    Log.d("OhPardonVM", "Game over. ${winningPlayer.name} wins!")
                }
                .addOnFailureListener {
                    Log.e("OhPardonVM", "Failed to update game over state", it)
                }

            _gameRoom.value = currentGame.copy(
                players = updatedPlayers,
                gameState = currentGame.gameState.copy(
                    currentTurnUid = nextPlayerUid,
                    diceRoll = null,
                    gameResult = "${winningPlayer.name} wins!"
                ),
                status = "over"
            )

            return // Skip remaining update block
        }

        val updates = mapOf(
            "players" to updatedPlayerMaps,
            "gameState.ohpardon.diceRoll" to null,
            "gameState.ohpardon.currentPlayer" to nextPlayerUid
        )

        firestore.collection("rooms")
            .document(roomCode)
            .update(updates)
            .addOnSuccessListener {
                Log.d("OhPardonVM", "DB updated")
            }
            .addOnFailureListener {
                Log.e("OhPardonVM", "Failed to update DB", it)
            }
    }


    private fun getNextPlayerUid(game: GameRoom, currentUid: String): String {
        val index = game.players.indexOfFirst { it.uid == currentUid }
        val nextIndex = (index + 1) % game.players.size
        return game.players[nextIndex].uid
    }

    fun getCoordinatesFromPosition(
        position: Int,
        color: Color,
        pawnIndex: Int = 0
    ): Pair<Int, Int> {
        val offset = when (color) {
            Color.Red -> 0
            Color.Green -> 20
            Color.Blue -> 10
            Color.Yellow -> 30
            else -> 0
        }

        val config = colorConfigs[color]

        // Define shared path
        val fullPath = listOf(
            Pair(0, 4),
            Pair(1, 4), Pair(2, 4), Pair(3, 4), Pair(4, 4),
            Pair(4, 3), Pair(4, 2), Pair(4, 1), Pair(4, 0),
            Pair(5, 0), Pair(6, 0), Pair(6, 1), Pair(6, 2), Pair(6, 3),
            Pair(6, 4), Pair(7, 4), Pair(8, 4), Pair(9, 4),
            Pair(10, 4), Pair(10, 5), Pair(10, 6), Pair(9, 6), Pair(8, 6),
            Pair(7, 6), Pair(6, 6), Pair(6, 7), Pair(6, 8),
            Pair(6, 9), Pair(6, 10), Pair(5, 10), Pair(4, 10), Pair(4, 9),
            Pair(4, 8), Pair(4, 7), Pair(4, 6), Pair(3, 6),
            Pair(2, 6), Pair(1, 6), Pair(0, 6), Pair(0, 5)
        )

        return when {
            position == -1 -> {
                // Home: choose index (e.g., pawnIndex) to spread pawns uniquely
                config?.homePositions?.getOrNull(pawnIndex % 4) ?: (0 to 0)
            }

            position in 0..39 -> {
                val index = (position + offset) % 40
                fullPath.getOrElse(index) { 5 to 5 }
            }

            position in 40..43 -> {
                config?.goalPath?.getOrNull(position - 40) ?: (5 to 5)
            }

            else -> (5 to 5) // fallback to center
        }
    }


    fun getBoardForUI(players: List<Player>): List<List<BoardCell>> {
        val board = MutableList(11) { y ->
            MutableList(11) { x ->
                BoardCell(x, y, type = CellType.EMPTY)
            }
        }

        // Mark home, goal, and entry cells based on colorConfigs
        colorConfigs.forEach { (color, config) ->
            config.homePositions.forEach { (x, y) ->
                board[y][x] = BoardCell(x, y, type = CellType.HOME, color = color)
            }
            config.goalPath.forEach { (x, y) ->
                board[y][x] = BoardCell(x, y, type = CellType.GOAL, color = color)
            }
            val (ex, ey) = config.entryCell
            board[ey][ex] = BoardCell(ex, ey, type = CellType.ENTRY, color = color)
        }

        nonColoredPath.forEach { (x, y) ->
            board[y][x] = BoardCell(x, y, type = CellType.PATH, color = Color.White)
        }

        // Place pawns
        players.forEach { player ->
            player.pawns.forEach { pawn ->
                val pawnId = pawn.id.filter { it.isDigit() }.toIntOrNull() ?: 0
                val (x, y) = getCoordinatesFromPosition(pawn.position, player.color, pawnId)
                board[y][x] = board[y][x].copy(
                    pawn = PawnForUI(color = player.color, id = pawnId)
                )
            }
        }

        return board
    }


    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}
