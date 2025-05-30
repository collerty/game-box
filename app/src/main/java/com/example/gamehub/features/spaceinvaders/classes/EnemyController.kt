package com.example.gamehub.features.spaceinvaders.classes

class EnemyController {
    val enemyMap: Array<Array<Int>> = arrayOf(
        arrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        arrayOf(2, 2, 2, 2, 2, 2, 2, 2, 2, 2),
        arrayOf(2, 2, 2, 2, 2, 2, 2, 2, 2, 2),
        arrayOf(3, 3, 3, 3, 3, 3, 3, 3, 3, 3),
        arrayOf(3, 3, 3, 3, 3, 3, 3, 3, 3, 3)
    )


    val enemies: Array<Array<Enemy>> = createEnemies()
    val enemyBullets = mutableListOf<Bullet>()

    var direction = 1 // 1 = right, -1 = left
    val speed = 5f
    val enemyWidth = 60f // same as above, keep consistent

    // Movement bounds (set these based on your screen width in pixels)
    var leftBound = 0f
    var rightBound = 0f

    fun enemyShoot() {
        // SHOOTER-type enemies can fire
        val shooters = enemies.flatten().filter { it.isAlive && it.type == EnemyType.SHOOTER }

        if (shooters.isEmpty()) return


        val shooter = shooters.randomOrNull() ?: return

        val bulletX = shooter.x + 60f / 2 - 5f // enemy width 60, bullet width 10
        val bulletY = shooter.y + 40f         // enemy height 40

        enemyBullets.add(Bullet(x = bulletX, y = bulletY, isActive = true, speed = 10f))
    }


    fun createEnemies(): Array<Array<Enemy>> {
        val enemyWidth = 60f
        val enemyHeight = 40f
        val spacingX = 20f
        val spacingY = 20f

        return Array(enemyMap.size) { rowIndex ->
            Array(enemyMap[rowIndex].size) { colIndex ->
                val type = mapToEnemyType(enemyMap[rowIndex][colIndex])
                val x = colIndex * (enemyWidth + spacingX)
                val y = rowIndex * (enemyHeight + spacingY) + 50f // 50f margin from top
                Enemy(x = x, y = y, isAlive = true, type = type)
            }
        }
    }


    fun mapToEnemyType(value: Int): EnemyType = when (value) {
        1 -> EnemyType.SHOOTER
        2 -> EnemyType.MIDDLE
        3 -> EnemyType.BOTTOM
        else -> throw IllegalArgumentException("Unknown enemy type value: $value")
    }

    fun setBounds(screenWidthPx: Float) {
        leftBound = 300f
        rightBound = screenWidthPx - 300f
    }



    fun updateEnemies() {
        // Calculate current group edges
        val allEnemies = enemies.flatten().filter { it.isAlive }
        if (allEnemies.isEmpty()) return

        val minX = allEnemies.minOf { it.x }
        val maxX = allEnemies.maxOf { it.x }

        // Change direction if at edge
        if (direction == 1 && maxX + enemyWidth >= rightBound) {
            direction = -1
            moveEnemiesDown()
        } else if (direction == -1 && minX <= leftBound) {
            direction = 1
            moveEnemiesDown()
        }

        // Move enemies horizontally
        allEnemies.forEach { it.x += speed * direction }
    }

    fun updateEnemyBullets(screenHeight: Float) {
        enemyBullets.forEach { bullet ->
            bullet.y += bullet.speed
            if (bullet.y > screenHeight) bullet.isActive = false
        }

        enemyBullets.removeAll { !it.isActive }
    }


    fun moveEnemiesDown() {
        val allEnemies = enemies.flatten().filter { it.isAlive }
        allEnemies.forEach { it.y += 20f } // drop down by 20 pixels
    }

}

