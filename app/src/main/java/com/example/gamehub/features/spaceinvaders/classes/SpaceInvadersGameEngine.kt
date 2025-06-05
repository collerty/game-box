package com.example.gamehub.features.spaceinvaders.classes

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class SpaceInvadersGameEngine {
    var playerLives by mutableStateOf(3)
        private set
    var gameState by mutableStateOf(GameState.PLAYING)
        internal set
    private val playerWidth = 100f
    private val moveSpeed = 15f
    private var enemyShootCooldown = 0
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

    var player = Player(x = 300f, y = 0f)

    val playerController = PlayerController(player, playerWidth, moveSpeed)
    val enemyController = EnemyController()

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

    val bunkers: MutableList<Bunker> = mutableListOf()


    private fun checkBulletEnemyCollisions() {
        val bullets = playerController.playerBullets
        val enemies = enemyController.enemies

        bullets.forEach { bullet ->
            if (!bullet.isActive) return@forEach

            enemies.flatten().forEach { enemy ->
                if (enemy.isAlive && bulletCollidesWithEnemy(bullet, enemy)) {
                    bullet.isActive = false
                    enemy.isAlive = false

                    // Scoring logic
                    val points = when (enemy.type) {
                        EnemyType.SHOOTER -> 40
                        EnemyType.MIDDLE -> 20
                        EnemyType.BOTTOM -> 10
                    }
                    score += points
                }
            }
        }

        // Check if UFO was hit
        if (enemyController.ufo.isActive) {
            val ufo = enemyController.ufo
            val bulletWidth = 10f
            val bulletHeight = 20f

            playerController.playerBullets.forEach { bullet ->
                if (!bullet.isActive) return@forEach

                val collides = bullet.x < ufo.x + ufo.width &&
                        bullet.x + bulletWidth > ufo.x &&
                        bullet.y < ufo.y + ufo.height &&
                        bullet.y + bulletHeight > ufo.y

                if (collides) {
                    bullet.isActive = false
                    ufo.isActive = false

                    score += listOf(50, 100, 150, 300).random()
                }
            }
        }

    }

    private fun bulletCollidesWithEnemy(bullet: Bullet, enemy: Enemy): Boolean {
        val bulletWidth = 10f
        val bulletHeight = 20f
        val enemyWidth = 60f
        val enemyHeight = 40f

        return bullet.x < enemy.x + enemyWidth &&
                bullet.x + bulletWidth > enemy.x &&
                bullet.y < enemy.y + enemyHeight &&
                bullet.y + bulletHeight > enemy.y
    }

    private fun checkPlayerHit() {
        enemyController.enemyBullets.forEach { bullet ->
            if (bullet.isActive && bulletHitsPlayer(bullet, player)) {
                bullet.isActive = false
                playerLives = (playerLives - 1).coerceAtLeast(0)
                if (playerLives == 0) {
                    gameState = GameState.GAME_OVER
                }
            }
        }
    }

    private fun bulletHitsPlayer(bullet: Bullet, player: Player): Boolean {
        val bulletWidth = 10f
        val bulletHeight = 20f
        val playerWidth = 100f
        val playerHeight = 30f

        return bullet.x < player.x + playerWidth &&
                bullet.x + bulletWidth > player.x &&
                bullet.y < player.y + playerHeight &&
                bullet.y + bulletHeight > player.y
    }

    fun initializeBunkers() {
        if (screenWidthPx == 0f || screenHeightPx == 0f) return // screen not ready

        //if(!bunkers.isEmpty()) return

        bunkers.clear() // Always clear and re-initialize

        val bunkerCount = 3
        val spacing = screenWidthPx / (bunkerCount + 1)
        val y = screenHeightPx - 400f

        for (i in 0 until bunkerCount) {
            val x = spacing * (i + 1) - 40f // Centered spacing
            bunkers.add(
                Bunker(
                    id = i, x = x, y = y,
                    width = 200f,
                    height = 100f,
                    health = 15
                )
            )
        }
    }




    fun checkBunkerHits() {
        val allBullets = playerController.playerBullets + enemyController.enemyBullets

        allBullets.forEach { bullet ->
            if (!bullet.isActive) return@forEach

            for (bunker in bunkers) {
                if (bunker.isHit(bullet.x, bullet.y)) {
                    bullet.isActive = false
                    bunker.takeDamage()
                    break
                }
            }
        }

        bunkers.removeAll { it.isDestroyed() }
    }


    fun updateGame() {
        enemyController.setBounds(screenWidthPx)
        playerController.updatePlayerMovement()
        checkPlayerHit()
        initializeBunkers()
        player = playerController.getPlayer()
        playerController.updateBullets(screenHeightPx)
        enemyController.updateEnemies()
        enemyController.updateUFO(screenWidthPx)
        checkBulletEnemyCollisions()
        checkBunkerHits()
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
}