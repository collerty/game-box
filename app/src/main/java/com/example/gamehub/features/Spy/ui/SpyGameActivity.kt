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
import android.app.Dialog
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import android.view.ViewGroup
import com.example.gamehub.features.spy.model.Location
import com.example.gamehub.features.spy.model.LocationManager
import com.example.gamehub.features.spy.model.SpyGameSettings
import com.example.gamehub.features.spy.model.SpyGameState
import com.example.gamehub.features.spy.service.SpyGameService

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
    private lateinit var newGameButton: Button

    private var currentPlayerIndex = 0
    private var isRoleRevealed = false
    private var allRolesRevealed = false

    private lateinit var locationManager: LocationManager
    private lateinit var locationsAdapter: LocationsAdapter
    private lateinit var spyGameService: SpyGameService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spy_game)
        locationManager = LocationManager(this)
        spyGameService = SpyGameService(locationManager)
        initializeViews()
        spyGameService.setupGameSettings()
        setupClickListeners()
        updateSettingsButtons()
    }

    private fun initializeViews() {
        settingsContainer = findViewById(R.id.settingsContainer)
        gameContainer = findViewById(R.id.gameContainer)
        playerCard = findViewById(R.id.playerCard)
        playerNumberText = findViewById(R.id.playerNumberText)
        tapToRevealText = findViewById(R.id.tapToRevealText)
        roleTextView = findViewById(R.id.roleTextView)
        timerTextView = findViewById(R.id.timerTextView)
        newGameButton = findViewById(R.id.newGameButton)
        settingsContainer.visibility = View.VISIBLE
        gameContainer.visibility = View.GONE
    }

    private fun setupClickListeners() {
        findViewById<Button>(R.id.playersButton).setOnClickListener { showPlayersDialog() }
        findViewById<Button>(R.id.spiesButton).setOnClickListener { showSpiesDialog() }
        findViewById<Button>(R.id.timerButton).setOnClickListener { showTimerDialog() }
        findViewById<Button>(R.id.locationsButton).setOnClickListener { showManageLocationsDialog() }
        findViewById<Button>(R.id.startGameButton).setOnClickListener { startGame() }
        newGameButton.setOnClickListener { resetGame() }
        playerCard.setOnClickListener { onPlayerCardClick() }
    }

    private fun startGame() {
        if (!spyGameService.canStartGame()) {
            Toast.makeText(this, "Please check your settings", Toast.LENGTH_SHORT).show()
            return
        }
        spyGameService.startGame()
        showGameUI()
        showPlayerCard()
    }

    private fun showPlayerCard() {
        val info = spyGameService.getPlayerCardInfo()
        playerNumberText.text = "Player ${info.playerNumber}"
        tapToRevealText.visibility = if (!info.isRoleRevealed) View.VISIBLE else View.GONE
        tapToRevealText.text = if (!info.isRoleRevealed) "Tap to reveal role" else "Tap to pass to the next player"
        roleTextView.visibility = if (info.isRoleRevealed) View.VISIBLE else View.GONE
        roleTextView.text = info.role ?: ""
        timerTextView.visibility = View.GONE
        newGameButton.visibility = View.GONE
    }

    private fun onPlayerCardClick() {
        val info = spyGameService.getPlayerCardInfo()
        if (!info.isRoleRevealed) {
            spyGameService.revealRole()
            showPlayerCard()
            if (spyGameService.currentPlayerIndex == spyGameService.gameSettings.numberOfPlayers - 1) {
                startTimer()
            }
        } else {
            if (spyGameService.advancePlayer()) {
                showPlayerCard()
            }
        }
    }

    private fun startTimer() {
        timerTextView.visibility = View.VISIBLE
        playerCard.visibility = View.GONE
        playerCard.setOnClickListener(null)
        spyGameService.startTimer(
            onTick = { seconds -> updateTimerDisplay(seconds) },
            onFinish = { updateTimerDisplay(0); showGameOver() }
        )
    }

    private fun updateTimerDisplay(seconds: Int? = null) {
        val time = seconds ?: spyGameService.getTimeRemaining()
        val minutes = time / 60
        val secs = time % 60
        timerTextView.text = String.format("%02d:%02d", minutes, secs)
    }

    private fun showGameUI() {
        settingsContainer.visibility = View.GONE
        gameContainer.visibility = View.VISIBLE
    }

    private fun showGameOver() {
        newGameButton.visibility = View.VISIBLE
    }

    private fun resetGame() {
        spyGameService.resetGame()
        showSettingsUI()
        updateSettingsButtons()
    }

    private fun showSettingsUI() {
        settingsContainer.visibility = View.VISIBLE
        gameContainer.visibility = View.GONE
    }

    private fun updateSettingsButtons() {
        val summary = spyGameService.getSettingsSummary()
        findViewById<Button>(R.id.playersButton).text = "Players: ${summary.players}"
        findViewById<Button>(R.id.spiesButton).text = "Spies: ${summary.spies}"
        findViewById<Button>(R.id.timerButton).text = "Timer: ${summary.timer} min"
        findViewById<Button>(R.id.locationsButton).text = "Locations: ${summary.locations}"
    }

    private fun showPlayersDialog() {
        SpyGameDialogs.showNumberPickerDialog(
            context = this,
            title = "Select Number of Players",
            minValue = 3,
            maxValue = 10,
            currentValue = spyGameService.gameSettings.numberOfPlayers
        ) { newValue ->
            spyGameService.updateNumberOfPlayers(newValue)
            updateSettingsButtons()
        }
    }

    private fun showSpiesDialog() {
        SpyGameDialogs.showNumberPickerDialog(
            context = this,
            title = "Select Number of Spies",
            minValue = 1,
            maxValue = spyGameService.gameSettings.numberOfPlayers - 1,
            currentValue = spyGameService.gameSettings.numberOfSpies
        ) { newValue ->
            spyGameService.updateNumberOfSpies(newValue)
            updateSettingsButtons()
        }
    }

    private fun showTimerDialog() {
        SpyGameDialogs.showNumberPickerDialog(
            context = this,
            title = "Select Timer Duration (minutes)",
            minValue = 1,
            maxValue = 10,
            currentValue = spyGameService.gameSettings.timerMinutes
        ) { newValue ->
            spyGameService.updateTimerMinutes(newValue)
            updateSettingsButtons()
        }
    }

    private fun showManageLocationsDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_manage_locations)
        dialog.window?.let { window ->
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            window.setBackgroundDrawableResource(android.R.color.transparent)
        }
        val recyclerView = dialog.findViewById<RecyclerView>(R.id.locationsRecyclerView)
        val addButton = dialog.findViewById<MaterialButton>(R.id.addLocationButton)
        recyclerView.layoutManager = LinearLayoutManager(this)
        locationsAdapter = LocationsAdapter(
            spyGameService.getLocations(),
            onEditClick = { location -> showEditLocationDialog(location) },
            onDeleteClick = { location ->
                spyGameService.removeLocation(location)
                locationsAdapter.updateLocations(spyGameService.getLocations())
                updateSettingsButtons()
            }
        )
        recyclerView.adapter = locationsAdapter
        addButton?.setOnClickListener { showAddLocationDialog() }
        dialog.show()
    }

    private fun showAddLocationDialog() {
        val dialog = Dialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_add_edit_location, null)
        dialog.setContentView(view)
        val nameEdit = view.findViewById<EditText>(R.id.locationNameEdit)
        val descriptionEdit = view.findViewById<EditText>(R.id.locationDescriptionEdit)
        val saveButton = view.findViewById<Button>(R.id.saveButton)
        val cancelButton = view.findViewById<Button>(R.id.cancelButton)
        saveButton.setOnClickListener {
            val name = nameEdit.text.toString()
            val description = descriptionEdit.text.toString()
            if (name.isNotEmpty() && description.isNotEmpty()) {
                spyGameService.addLocation(name, description)
                locationsAdapter.updateLocations(spyGameService.getLocations())
                updateSettingsButtons()
                dialog.dismiss()
            }
        }
        cancelButton.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showEditLocationDialog(location: Location) {
        val dialog = Dialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_add_edit_location, null)
        dialog.setContentView(view)
        val nameEdit = view.findViewById<EditText>(R.id.locationNameEdit)
        val descriptionEdit = view.findViewById<EditText>(R.id.locationDescriptionEdit)
        val saveButton = view.findViewById<Button>(R.id.saveButton)
        val cancelButton = view.findViewById<Button>(R.id.cancelButton)
        nameEdit.setText(location.name)
        descriptionEdit.setText(location.description)
        saveButton.setOnClickListener {
            val name = nameEdit.text.toString()
            val description = descriptionEdit.text.toString()
            if (name.isNotEmpty() && description.isNotEmpty()) {
                spyGameService.updateLocation(location, Location(name, description))
                locationsAdapter.updateLocations(spyGameService.getLocations())
                updateSettingsButtons()
                dialog.dismiss()
            }
        }
        cancelButton.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        spyGameService.cancelTimer()
    }
} 