package com.example.gamehub.features.spy.ui

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.gamehub.R

class SpyGameActivity : AppCompatActivity() {
    private lateinit var gameSettings: SpyGameSettings
    private lateinit var gameState: SpyGameState
    private var timer: CountDownTimer? = null

    private lateinit var settingsContainer: LinearLayout
    private lateinit var gameContainer: LinearLayout
    private lateinit var playerCard: CardView
    private lateinit var playerNumberText: TextView
    private lateinit var tapToRevealText: TextView
    private lateinit var roleTextView: TextView
    private lateinit var timerTextView: TextView
    private lateinit var nextPlayerButton: Button
    private lateinit var newGameButton: Button

    private var currentPlayerIndex = 0
    private var isRoleRevealed = false
    private var allRolesRevealed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("SpyGame", "onCreate called")
        setContentView(R.layout.activity_spy_game)
        Log.d("SpyGame", "setContentView completed")

        try {
            initializeViews()
            setupGameSettings()
            setupClickListeners()
            Log.d("SpyGame", "Initialization completed successfully")
        } catch (e: Exception) {
            Log.e("SpyGame", "Error during initialization", e)
            Toast.makeText(this, "Error initializing game: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initializeViews() {
        Log.d("SpyGame", "Initializing views")
        try {
            settingsContainer = findViewById(R.id.settingsContainer)
            gameContainer = findViewById(R.id.gameContainer)
            playerCard = findViewById(R.id.playerCard)
            playerNumberText = findViewById(R.id.playerNumberText)
            tapToRevealText = findViewById(R.id.tapToRevealText)
            roleTextView = findViewById(R.id.roleTextView)
            timerTextView = findViewById(R.id.timerTextView)
            nextPlayerButton = findViewById(R.id.nextPlayerButton)
            newGameButton = findViewById(R.id.newGameButton)
            
            Log.d("SpyGame", "All views found successfully")
            
            // Set initial visibility
            settingsContainer.visibility = View.VISIBLE
            gameContainer.visibility = View.GONE
            Log.d("SpyGame", "Initial visibility set: settings=${settingsContainer.visibility}, game=${gameContainer.visibility}")
        } catch (e: Exception) {
            Log.e("SpyGame", "Error finding views", e)
            throw e
        }
    }

    private fun setupGameSettings() {
        Log.d("SpyGame", "Setting up game settings")
        try {
            gameSettings = SpyGameSettings(
                numberOfPlayers = 4,
                numberOfSpies = 1,
                timerMinutes = 5,
                selectedLocations = SpyGameSettings.defaultLocations
            )
            gameState = SpyGameState(gameSettings)
            updateSettingsButtons()
            Log.d("SpyGame", "Game settings initialized: players=${gameSettings.numberOfPlayers}, spies=${gameSettings.numberOfSpies}, locations=${gameSettings.selectedLocations.size}")
        } catch (e: Exception) {
            Log.e("SpyGame", "Error setting up game settings", e)
            throw e
        }
    }

    private fun setupClickListeners() {
        Log.d("SpyGame", "Setting up click listeners")
        try {
            findViewById<Button>(R.id.playersButton).setOnClickListener { showPlayersDialog() }
            findViewById<Button>(R.id.spiesButton).setOnClickListener { showSpiesDialog() }
            findViewById<Button>(R.id.timerButton).setOnClickListener { showTimerDialog() }
            findViewById<Button>(R.id.locationsButton).setOnClickListener { showLocationsDialog() }
            findViewById<Button>(R.id.startGameButton).setOnClickListener { startGame() }
            nextPlayerButton.setOnClickListener { nextPlayer() }
            newGameButton.setOnClickListener { resetGame() }
            playerCard.setOnClickListener { revealRole() }
            Log.d("SpyGame", "All click listeners set up successfully")
        } catch (e: Exception) {
            Log.e("SpyGame", "Error setting up click listeners", e)
            throw e
        }
    }

    private fun startGame() {
        Log.d("SpyGame", "Starting game")
        try {
            // Validate game settings
            if (gameSettings.selectedLocations.isEmpty()) {
                Log.d("SpyGame", "No locations selected")
                Toast.makeText(this, "Please select at least one location", Toast.LENGTH_SHORT).show()
                return
            }
            if (gameSettings.numberOfSpies >= gameSettings.numberOfPlayers) {
                Log.d("SpyGame", "Invalid number of spies")
                Toast.makeText(this, "Number of spies must be less than number of players", Toast.LENGTH_SHORT).show()
                return
            }

            gameState = SpyGameState(gameSettings)
            gameState.startGame()
            currentPlayerIndex = 0
            allRolesRevealed = false
            showGameUI()
            showPlayerCard()
            Log.d("SpyGame", "Game started successfully")
        } catch (e: Exception) {
            Log.e("SpyGame", "Error starting game", e)
            Toast.makeText(this, "Error starting game: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showPlayerCard() {
        Log.d("SpyGame", "Showing player card for player ${currentPlayerIndex + 1}")
        try {
            isRoleRevealed = false
            playerNumberText.text = "Player ${currentPlayerIndex + 1}"
            tapToRevealText.visibility = View.VISIBLE
            roleTextView.visibility = View.GONE
            nextPlayerButton.visibility = View.GONE
            timerTextView.visibility = View.GONE
            newGameButton.visibility = View.GONE
            Log.d("SpyGame", "Player card shown successfully")
        } catch (e: Exception) {
            Log.e("SpyGame", "Error showing player card", e)
            throw e
        }
    }

    private fun revealRole() {
        Log.d("SpyGame", "Revealing role for player ${currentPlayerIndex + 1}")
        try {
            if (allRolesRevealed) return
            if (!isRoleRevealed) {
                isRoleRevealed = true
                tapToRevealText.visibility = View.GONE
                roleTextView.visibility = View.VISIBLE
                roleTextView.text = gameState.getPlayerRole(currentPlayerIndex)
                
                if (currentPlayerIndex < gameSettings.numberOfPlayers - 1) {
                    nextPlayerButton.visibility = View.VISIBLE
                } else {
                    Log.d("SpyGame", "All players have seen their roles, starting timer")
                    allRolesRevealed = true
                    startTimer()
                }
                Log.d("SpyGame", "Role revealed successfully")
            }
        } catch (e: Exception) {
            Log.e("SpyGame", "Error revealing role", e)
            throw e
        }
    }

    private fun nextPlayer() {
        Log.d("SpyGame", "Moving to next player")
        try {
            if (currentPlayerIndex < gameSettings.numberOfPlayers - 1) {
                currentPlayerIndex++
                showPlayerCard()
                Log.d("SpyGame", "Moved to next player: ${currentPlayerIndex + 1}")
            }
        } catch (e: Exception) {
            Log.e("SpyGame", "Error moving to next player", e)
            throw e
        }
    }

    private fun startTimer() {
        Log.d("SpyGame", "Starting timer")
        try {
            timerTextView.visibility = View.VISIBLE
            nextPlayerButton.visibility = View.GONE
            playerCard.visibility = View.GONE  // Hide the location card when timer starts
            playerCard.setOnClickListener(null) // Disable further taps
            timer = object : CountDownTimer(
                gameSettings.timerMinutes * 60 * 1000L,
                1000
            ) {
                override fun onTick(millisUntilFinished: Long) {
                    val seconds = (millisUntilFinished / 1000).toInt()
                    gameState.updateTimer(seconds)
                    updateTimerDisplay()
                }

                override fun onFinish() {
                    Log.d("SpyGame", "Timer finished")
                    gameState.updateTimer(0)
                    updateTimerDisplay()
                    showGameOver()
                }
            }.start()
            Log.d("SpyGame", "Timer started successfully")
        } catch (e: Exception) {
            Log.e("SpyGame", "Error starting timer", e)
            throw e
        }
    }

    private fun updateTimerDisplay() {
        try {
            val minutes = gameState.getTimeRemaining() / 60
            val seconds = gameState.getTimeRemaining() % 60
            timerTextView.text = String.format("%02d:%02d", minutes, seconds)
        } catch (e: Exception) {
            Log.e("SpyGame", "Error updating timer display", e)
        }
    }

    private fun showGameUI() {
        Log.d("SpyGame", "Showing game UI")
        try {
            settingsContainer.visibility = View.GONE
            gameContainer.visibility = View.VISIBLE
            Log.d("SpyGame", "Game UI shown: settings=${settingsContainer.visibility}, game=${gameContainer.visibility}")
        } catch (e: Exception) {
            Log.e("SpyGame", "Error showing game UI", e)
            throw e
        }
    }

    private fun showGameOver() {
        Log.d("SpyGame", "Showing game over")
        try {
            nextPlayerButton.visibility = View.GONE
            newGameButton.visibility = View.VISIBLE
            Log.d("SpyGame", "Game over UI shown")
        } catch (e: Exception) {
            Log.e("SpyGame", "Error showing game over", e)
            throw e
        }
    }

    private fun resetGame() {
        Log.d("SpyGame", "Resetting game")
        if (timer != null) {
            timer?.cancel()
            timer = null
        }
        showSettingsUI()
        setupGameSettings()
        Log.d("SpyGame", "Game reset successfully")
    }

    private fun showSettingsUI() {
        Log.d("SpyGame", "Showing settings UI")
        try {
            settingsContainer.visibility = View.VISIBLE
            gameContainer.visibility = View.GONE
            Log.d("SpyGame", "Settings UI shown: settings=${settingsContainer.visibility}, game=${gameContainer.visibility}")
        } catch (e: Exception) {
            Log.e("SpyGame", "Error showing settings UI", e)
            throw e
        }
    }

    private fun updateSettingsButtons() {
        try {
            findViewById<Button>(R.id.playersButton).text = "Players: ${gameSettings.numberOfPlayers}"
            findViewById<Button>(R.id.spiesButton).text = "Spies: ${gameSettings.numberOfSpies}"
            findViewById<Button>(R.id.timerButton).text = "Timer: ${gameSettings.timerMinutes} min"
            findViewById<Button>(R.id.locationsButton).text = "Locations: ${gameSettings.selectedLocations.size}"
            Log.d("SpyGame", "Settings buttons updated")
        } catch (e: Exception) {
            Log.e("SpyGame", "Error updating settings buttons", e)
            throw e
        }
    }

    private fun showPlayersDialog() {
        try {
            SpyGameDialogs.showNumberPickerDialog(
                context = this,
                title = "Select Number of Players",
                minValue = 3,
                maxValue = 10,
                currentValue = gameSettings.numberOfPlayers
            ) { newValue ->
                gameSettings.numberOfPlayers = newValue
                updateSettingsButtons()
            }
        } catch (e: Exception) {
            Log.e("SpyGame", "Error showing players dialog", e)
            Toast.makeText(this, "Error showing dialog: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSpiesDialog() {
        try {
            SpyGameDialogs.showNumberPickerDialog(
                context = this,
                title = "Select Number of Spies",
                minValue = 1,
                maxValue = gameSettings.numberOfPlayers - 1,
                currentValue = gameSettings.numberOfSpies
            ) { newValue ->
                gameSettings.numberOfSpies = newValue
                updateSettingsButtons()
            }
        } catch (e: Exception) {
            Log.e("SpyGame", "Error showing spies dialog", e)
            Toast.makeText(this, "Error showing dialog: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showTimerDialog() {
        try {
            SpyGameDialogs.showNumberPickerDialog(
                context = this,
                title = "Select Timer Duration (minutes)",
                minValue = 1,
                maxValue = 10,
                currentValue = gameSettings.timerMinutes
            ) { newValue ->
                gameSettings.timerMinutes = newValue
                updateSettingsButtons()
            }
        } catch (e: Exception) {
            Log.e("SpyGame", "Error showing timer dialog", e)
            Toast.makeText(this, "Error showing dialog: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLocationsDialog() {
        try {
            SpyGameDialogs.showLocationsDialog(
                context = this,
                currentLocations = gameSettings.selectedLocations
            ) { selectedLocations ->
                gameSettings.selectedLocations = selectedLocations
                updateSettingsButtons()
            }
        } catch (e: Exception) {
            Log.e("SpyGame", "Error showing locations dialog", e)
            Toast.makeText(this, "Error showing dialog: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (timer != null) {
            timer?.cancel()
            timer = null
        }
        Log.d("SpyGame", "Activity destroyed")
    }
} 