package com.example.gamehub.features.spaceinvaders.classes

class SpaceInvadersGameEngine {
    var player = Player(x = 0f, y = 0f)
    private val playerWidth = 100f
    private val moveSpeed = 10f
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

    fun updateGame() {
        enemyController.setBounds(screenWidthPx)
        playerController.updatePlayerMovement()
        player = playerController.getPlayer()
        playerController.updateBullets(screenHeightPx)
        enemyController.updateEnemies()
    }

}
