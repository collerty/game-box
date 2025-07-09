package com.example.gamehub.features.spaceinvaders.classes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.gamehub.features.spaceinvaders.controllers.BunkerController
import com.example.gamehub.features.spaceinvaders.controllers.EnemyController
import com.example.gamehub.features.spaceinvaders.controllers.PlayerController
import com.example.gamehub.features.spaceinvaders.controllers.UFOController
import com.example.gamehub.features.spaceinvaders.models.Enemy
import com.example.gamehub.features.spaceinvaders.models.EnemyType
import com.example.gamehub.features.spaceinvaders.models.GameState
import com.example.gamehub.features.spaceinvaders.models.Player
import com.example.gamehub.features.spaceinvaders.util.AudioManager
import com.example.gamehub.features.spaceinvaders.util.CollisionDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SpaceInvadersGameEngine(
    private val audioManager: AudioManager,
    private val coroutineScope: CoroutineScope
) {
    var playerLives by mutableStateOf(3)
        private set
    var gameState by mutableStateOf(GameState.PLAYING)
        internal set
    var score = 0
        private set

    var screenWidthPx: Float = 0f
        set(value) {
            field = value
            playerController.screenWidthPx = value
        }
    var screenHeightPx: Float = 500f
        set(value) {
            field = value
            playerController.screenHeightPx = value
        }

    private val playerWidth = 100f
    private val moveSpeed = 15f
    private var enemyShootCooldown = 0
    var player = Player(x = 300f, y = 0f)

    val playerController = PlayerController(
        player = player,
        playerWidth = playerWidth,
        moveSpeed = moveSpeed,
        audioManager = audioManager,
        coroutineScope = coroutineScope
    )

    val enemyController = EnemyController()

    val ufoController = UFOController(
        audioManager = audioManager,
        coroutineScope = coroutineScope
    )

    val bunkerController = BunkerController()
    val collisionDetector = CollisionDetector()

    var isMovingLeft: Boolean
        get() = playerController.isMovingLeft
        set(value) {
            playerController.isMovingLeft = value
        }

    var isMovingRight: Boolean
        get() = playerController.isMovingRight
        set(value) {
            playerController.isMovingRight = value
        }

    private fun handleEnemyHit(enemy: Enemy) {
        // Scoring logic
        val points = when (enemy.type) {
            EnemyType.SHOOTER -> 40
            EnemyType.MIDDLE -> 20
            EnemyType.BOTTOM -> 10
        }
        score += points

        coroutineScope.launch {
            audioManager.playExplodeSound()
            audioManager.vibrate()
        }
    }

    private fun handleUFOHit() {
        score += listOf(50, 100, 150, 300).random()
        coroutineScope.launch {
            audioManager.playExplodeSound()
            audioManager.vibrate()
        }
    }

    private fun handlePlayerHit() {
        playerLives = (playerLives - 1).coerceAtLeast(0)
        if (playerLives == 0) {
            gameState = GameState.GAME_OVER
        }
        coroutineScope.launch {
            audioManager.playTakeDamageSound()
            audioManager.vibrate()
        }
    }

    fun updateGame() {
        enemyController.setBounds(screenWidthPx)
        playerController.updatePlayerMovement()
        bunkerController.initializeBunkers(screenWidthPx, screenHeightPx)

        player = playerController.getPlayer()
        playerController.updateBullets(screenHeightPx)
        enemyController.updateEnemies()
        ufoController.updateUFO(screenWidthPx)

        // Collision detection
        collisionDetector.checkBulletEnemyCollisions(
            bullets = playerController.playerBullets,
            enemies = enemyController.enemies,
            onEnemyHit = this::handleEnemyHit
        )

        collisionDetector.checkBulletUfoCollision(
            bullets = playerController.playerBullets,
            ufo = ufoController.ufo,
            onUfoHit = this::handleUFOHit
        )

        if (gameState == GameState.PLAYING) {
            collisionDetector.checkBulletPlayerCollision(
                bullets = enemyController.enemyBullets,
                player = player,
                onPlayerHit = this::handlePlayerHit
            )
        }

        collisionDetector.checkBulletBunkerCollisions(
            bullets = playerController.playerBullets + enemyController.enemyBullets,
            bunkers = bunkerController.getBunkers()
        )

        bunkerController.removeDestroyedBunkers()

        if (enemyShootCooldown <= 0) {
            enemyController.enemyShoot()
            enemyShootCooldown = 60
        } else {
            enemyShootCooldown--
        }

        enemyController.updateEnemyBullets(screenHeightPx)

        // Check if the wave is cleared
        if (enemyController.isWaveCleared()) {
            enemyController.loadMap((enemyController.enemyMaps.indices).random()) // Load random new map
        }

        if (enemyController.hasEnemyReachedPlayerLine(player.y + -100f)) {
            gameState = GameState.GAME_OVER
        }
    }

    fun reset() {
        playerLives = 3
        score = 0
        gameState = GameState.PLAYING
        player = Player(x = 300f, y = screenHeightPx - 100f)
        playerController.setPlayer(player)
        bunkerController.reset()
        enemyController.loadMap((enemyController.enemyMaps.indices).random())
    }
}