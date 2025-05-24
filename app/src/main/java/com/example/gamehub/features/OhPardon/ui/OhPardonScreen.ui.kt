package com.example.gamehub.features.ohpardon.ui

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.example.gamehub.R
import com.example.gamehub.features.ohpardon.OhPardonViewModel
import com.example.gamehub.features.ohpardon.classes.OhPardonViewModelFactory
import com.google.firebase.firestore.FirebaseFirestore


@Composable
fun OhPardonScreen(
    navController: NavController,
    code: String,
    userName: String
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val firestore = FirebaseFirestore.getInstance()


    val viewModel: OhPardonViewModel = viewModel(
        factory = OhPardonViewModelFactory(application, code, userName)
    )

    val gameRoom by viewModel.gameRoom.collectAsState()
    val currentDiceRoll = gameRoom?.gameState?.diceRoll
    val lifecycleOwner = LocalLifecycleOwner.current

    var selectedPawnId by remember { mutableStateOf<Int?>(null) }

    val toastMessage by viewModel.toastMessage.collectAsState()
    val showVictoryDialog = remember { mutableStateOf(false) }
    val winnerName = remember { mutableStateOf("") }

    LaunchedEffect(gameRoom?.status) {
        if (gameRoom?.status == "over" && gameRoom?.gameState?.gameResult?.contains("wins") == true) {
            winnerName.value = gameRoom?.gameState?.gameResult?.removeSuffix(" wins!") ?: ""
            showVictoryDialog.value = true
        }
    }


    // Debugging logs
    LaunchedEffect(gameRoom) {
        Log.d("OhPardonScreen", "GameRoom updated: $gameRoom")
        gameRoom?.let {
            Log.d("OhPardonScreen", "Current turn UID: ${it.gameState.currentTurnUid}")
            Log.d("OhPardonScreen", "Players count: ${it.players.size}")
        }
    }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToastMessage()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.registerShakeListener()
                Lifecycle.Event.ON_PAUSE -> viewModel.unregisterShakeListener()
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            viewModel.unregisterShakeListener()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // ImageLoader for SVGs
    val imageLoader = ImageLoader.Builder(context)
        .components { add(SvgDecoder.Factory()) }
        .build()

    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data("android.resource://${context.packageName}/${R.raw.ohpardon_board}")
            .crossfade(true)
            .build(),
        imageLoader = imageLoader
    )

    fun colorToString(color: Color): String {
        return when (color) {
            Color.Red -> "Red"
            Color.Green -> "Green"
            Color.Blue -> "Blue"
            Color.Yellow -> "Yellow"
            else -> "Unknown"
        }
    }

    val currentPlayer = gameRoom?.players?.find { it.name == userName }
    val isHost = gameRoom?.hostUid == currentPlayer?.uid

    if (showVictoryDialog.value) {
        AlertDialog(
            onDismissRequest = { /* Prevent dismissal on tap outside */ },
            title = { Text(text = "🎉 Game Over") },
            text = { Text(text = "${winnerName.value} has won the game!") },
            confirmButton = {
                TextButton(onClick = {
                    // Host deletes room or navigates away
                    showVictoryDialog.value = false
                    // Call delete if host, or popBackStack if client
                    if (isHost) {
                        firestore.collection("rooms").document(code)
                            .delete()
                            .addOnSuccessListener {
                                navController.popBackStack()
                            }
                    } else {
                        navController.popBackStack()
                    }
                }) {
                    Text(if (isHost) "Close Room" else "Exit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVictoryDialog.value = false }) {
                    Text("Stay")
                }
            }
        )
    }


    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        )
        {
            // Board Image
            Image(
                painter = painter,
                contentDescription = "Game Board",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (gameRoom != null) {
                val currentPlayer = gameRoom!!.players.find { it.uid == gameRoom!!.gameState.currentTurnUid }

                if (currentPlayer != null) {
                    Text(
                        text = "It's ${currentPlayer.name}'s turn!",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    gameRoom!!.gameState.diceRoll?.let {
                        Text(
                            text = "${currentPlayer.name} rolled a $it!",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    // Pawn selection buttons - only show if it's the current player's turn
                    if (currentPlayer.name == userName) {

                        // Dice roll button - only show if dice has NOT been rolled yet this turn
                        if (currentDiceRoll == null) {
                            Button(
                                onClick = {
                                    viewModel.attemptRollDice(userName)
                                },
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                Text("Roll Dice")
                            }
                        }

                        // Pawn selection buttons - only show if dice rolled
                        if (currentDiceRoll != null) {
                            Text(
                                "Select a Pawn to Move:",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                (0..3).forEach { pawnId ->
                                    Button(
                                        onClick = { selectedPawnId = pawnId },
                                        colors = if (selectedPawnId == pawnId)
                                            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        else
                                            ButtonDefaults.buttonColors(),
                                        enabled = currentDiceRoll != null // Only enable if dice has been rolled
                                    ) {
                                        Text("Pawn $pawnId")
                                    }
                                }
                            }

                            // Move button
                            selectedPawnId?.let {
                                Button(
                                    onClick = {
                                        viewModel.attemptMovePawn(
                                            gameRoom!!.gameState.currentTurnUid,
                                            it.toString()
                                        )
                                        selectedPawnId = null
                                    },
                                    modifier = Modifier.padding(top = 8.dp),
                                    enabled = true
                                ) {
                                    Text("Move Selected Pawn")
                                }
                            }

                            Button(
                                onClick = {
                                    viewModel.skipTurn(
                                        userName
                                    )
                                    selectedPawnId = null
                                },
                                modifier = Modifier.padding(top = 8.dp),
                                enabled = true
                            ) {
                                Text("Skip turn")
                            }
                        }
                    }
                }

                // Debug info - show all players
                gameRoom!!.players.forEach { player ->
                    Text(text = player.name, style = MaterialTheme.typography.titleMedium)
                    Text(text = "Color: ${colorToString(player.color)}")
                    Text(text = "Pawns:")
                    player.pawns.forEachIndexed { index, pawn ->
                        Text(text = "Pawn $index: ${pawn.position}")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else {
                CircularProgressIndicator()
                Text("Loading game data...")
            }
        }
    }
}