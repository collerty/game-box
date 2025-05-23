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
    val rematchVotes: Map<String, Boolean>,
    val createdAt: Timestamp?
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

data class PawnWithUI(
    val logic: Pawn,
    val ui: PawnUI
)

data class Pawn(
    val id: String,
    var position: Int = -1 // -1 means at home
)

data class PawnUI(
    val id: String,
    val color: Color,
    val x: Float, // 0.0 - 1.0 relative to board width
    val y: Float  // 0.0 - 1.0 relative to board height
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

    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
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
            color = parseColor(data["color"] as? String ?: "red"), // Assume color is stored as string
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
            rematchVotes = (data["rematchVotes"] as? Map<String, Boolean>) ?: emptyMap(),
            createdAt = data["createdAt"] as? Timestamp
        )
    }
    fun attemptMovePawn(currentUserUid: String, pawnId: String) {
        val currentGame = _gameRoom.value ?: return
        val gameState = currentGame.gameState

        if (gameState.currentTurnUid != currentUserUid) {
            Log.w("OhPardonVM", "Not your turn")
            return
        }

        val diceRoll = gameState.diceRoll
        if (diceRoll == null) {
            Log.w("OhPardonVM", "Dice not rolled yet")
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

        val stepsToVictoryStart = 40
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
            currentPos == -1 && diceRoll == 6 -> playerOffset // Enter from home
            currentPos in 0 until stepsToVictoryStart -> {
                val stepsTaken = (currentPos - playerOffset + 40) % 40
                val totalSteps = stepsTaken + diceRoll

                if (totalSteps >= 40) {
                    val victoryCell = 40 + (totalSteps - 40)
                    if (victoryCell > 43) {
                        Log.d("OhPardonVM", "Cannot move beyond victory")
                        val updates = mapOf(
                            "gameState.ohpardon.diceRoll" to null,
                            "gameState.ohpardon.currentPlayer" to getNextPlayerUid(currentGame, currentUserUid)
                        )
                        firestore.collection("rooms")
                            .document(roomCode)
                            .update(updates)
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
                    Log.d("OhPardonVM", "Cannot move beyond victory")
                    val updates = mapOf(
                        "gameState.ohpardon.diceRoll" to null,
                        "gameState.ohpardon.currentPlayer" to getNextPlayerUid(currentGame, currentUserUid)
                    )
                    firestore.collection("rooms")
                        .document(roomCode)
                        .update(updates)
                    return
                }
                newVictoryPos
            }
            else -> {
                Log.d("OhPardonVM", "Invalid move")
                val updates = mapOf(
                    "gameState.ohpardon.diceRoll" to null,
                    "gameState.ohpardon.currentPlayer" to getNextPlayerUid(currentGame, currentUserUid)
                )
                firestore.collection("rooms")
                    .document(roomCode)
                    .update(updates)
                return
            }
        }

        val isBlockedByOpponent = isProtectedStartCellBlocked(newPosition, currentUserUid, currentGame)
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
                        if (isMoveBlocked) it else it.copy(position = newPosition)
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


    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}
