package com.example.gamehub.features.spaceinvaders.classes

class SpaceInvadersGameEngine {
    var player = Player(x = 0f, y = 0f)
    private val playerWidth = 100f
    private val moveSpeed = 10f
    private var enemyShootCooldown = 0
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

    val playerController = PlayerController(player, playerWidth, moveSpeed)
    val enemyController = EnemyController()

    var isMovingLeft: Boolean
        get() = playerController.isMovingLeft
        set(value) { playerController.isMovingLeft = value }

    var isMovingRight: Boolean
        get() = playerController.isMovingRight
        set(value) { playerController.isMovingRight = value }

    private fun checkBulletEnemyCollisions() {
        val bullets = playerController.playerBullets
        val enemies = enemyController.enemies

        bullets.forEach { bullet ->
            if (!bullet.isActive) return@forEach

            enemies.flatten().forEach { enemy ->
                if (enemy.isAlive && bulletCollidesWithEnemy(bullet, enemy)) {
                    bullet.isActive = false
                    enemy.isAlive = false
                    // You could also add score handling here
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






    fun updateGame() {
        enemyController.setBounds(screenWidthPx)
        playerController.updatePlayerMovement()
        player = playerController.getPlayer()
        playerController.updateBullets(screenHeightPx)
        enemyController.updateEnemies()
        checkBulletEnemyCollisions()

        if (enemyShootCooldown <= 0) {
            enemyController.enemyShoot()
            enemyShootCooldown = 60
        } else {
            enemyShootCooldown--
        }

        enemyController.updateEnemyBullets(screenHeightPx)
    }

}
