package com.example.gamehub.features.spaceinvaders.classes

import kotlin.random.Random

class EnemyController {

    // All available maps
    val enemyMaps: List<Array<Array<Int>>> = listOf(
        arrayOf(
            arrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
            arrayOf(2, 2, 2, 2, 2, 2, 2, 2, 2, 2),
            arrayOf(2, 2, 2, 2, 2, 2, 2, 2, 2, 2),
            arrayOf(3, 3, 3, 3, 3, 3, 3, 3, 3, 3),
            arrayOf(3, 3, 3, 3, 3, 3, 3, 3, 3, 3)
        ),
        arrayOf(
            arrayOf(1, 1, 1, 1, 0, 0, 1, 1, 1, 1),
            arrayOf(2, 0, 0, 2, 0, 0, 2, 0, 0, 2),
            arrayOf(2, 0, 0, 2, 0, 0, 2, 0, 0, 2),
            arrayOf(3, 0, 0, 3, 0, 0, 3, 0, 0, 3),
            arrayOf(3, 3, 3, 3, 0, 0, 3, 3, 3, 3)
        ),
        arrayOf(
            arrayOf(0, 0, 0, 0, 1, 0, 0, 0, 0),
            arrayOf(0, 0, 0, 1, 1, 1, 0, 0, 0),
            arrayOf(0, 0, 2, 2, 2, 2, 2, 0, 0),
            arrayOf(0, 3, 3, 2, 2, 2, 3, 3, 0),
            arrayOf(3, 3, 3, 3, 3, 3, 3, 3, 3)
        ),
        arrayOf(
            arrayOf(0, 1, 0, 1, 0, 1, 0, 1),
            arrayOf(2, 0, 2, 0, 2, 0, 2, 0),
            arrayOf(0, 1, 0, 1, 0, 1, 0, 1),
            arrayOf(2, 0, 2, 0, 2, 0, 2, 0),
            arrayOf(0, 3, 0, 3, 0, 3, 0, 3)
        ),
        arrayOf(
            arrayOf(0, 0, 0, 1, 0, 1, 0, 0, 0),
            arrayOf(0, 0, 2, 0, 2, 0, 2, 0, 0),
            arrayOf(0, 3, 0, 0, 0, 0, 0, 3, 0),
            arrayOf(3, 0, 0, 0, 0, 0, 0, 0, 3),
        )

    )

    var ufo: UFO = UFO(x = -100f, y = 50f, isActive = false)
    private var ufoSpawnCooldown = 0

    fun updateUFO(screenWidth: Float) {
        if (ufo.isActive) {
            ufo.x += ufo.speed * ufo.direction

            // Deactivate when off-screen
            if ((ufo.direction == 1 && ufo.x > screenWidth) ||
                (ufo.direction == -1 && ufo.x + ufo.width < 0)) {
                ufo.isActive = false
            }
        } else {
            // Maybe spawn UFO randomly
            if (ufoSpawnCooldown <= 0 && Random.nextFloat() < 0.005f) { // 0.5% chance per frame
                ufo.direction = if (Random.nextBoolean()) 1 else -1
                ufo.x = if (ufo.direction == 1) -ufo.width else screenWidth
                ufo.y = 50f
                ufo.isActive = true
                ufoSpawnCooldown = 600 // ~10 seconds if 60 FPS
            } else {
                ufoSpawnCooldown--
            }
        }
    }


    lateinit var enemies: Array<Array<Enemy>>
    val enemyBullets = mutableListOf<Bullet>()

    var direction = 1 // 1 = right, -1 = left
    val speed = 10f
    val enemyWidth = 60f

    var leftBound = 0f
    var rightBound = 0f

    init {
        loadMap(Random.nextInt(enemyMaps.size))
    }

    fun loadMap(index: Int) {
        if (index !in enemyMaps.indices) throw IllegalArgumentException("Map index out of bounds")
        enemies = createEnemies(enemyMaps[index])
    }

    fun enemyShoot() {
        val shooters = enemies.flatten().filter { it.isAlive && it.type == EnemyType.SHOOTER }
        if (shooters.isEmpty()) return

        val shooter = shooters.randomOrNull() ?: return
        val bulletX = shooter.x + enemyWidth / 2 - 5f
        val bulletY = shooter.y + 40f // enemy height
        enemyBullets.add(Bullet(x = bulletX, y = bulletY, isActive = true, speed = 10f))
    }

    fun createEnemies(enemyMap: Array<Array<Int>>): Array<Array<Enemy>> {
        val enemyHeight = 40f
        val spacingX = 20f
        val spacingY = 20f

        return Array(enemyMap.size) { rowIndex ->
            Array(enemyMap[rowIndex].size) { colIndex ->
                val value = enemyMap[rowIndex][colIndex]
                if (value == 0) {
                    Enemy(
                        x = -100f,
                        y = -100f,
                        isAlive = false,
                        type = EnemyType.MIDDLE
                    ) // Off-screen, dead
                } else {
                    val type = mapToEnemyType(value)
                    val x = colIndex * (enemyWidth + spacingX)
                    val y = rowIndex * (enemyHeight + spacingY) + 50f
                    Enemy(x = x, y = y, isAlive = true, type = type)
                }
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

    fun hasEnemyReachedPlayerLine(playerY: Float): Boolean {
        val allEnemies = enemies.flatten().filter { it.isAlive }
        return allEnemies.any { it.y + 40f >= playerY } // 40f is enemy height
    }




    fun moveEnemiesDown() {
        val allEnemies = enemies.flatten().filter { it.isAlive }
        allEnemies.forEach { it.y += 20f } // drop down by 20 pixels
    }

}

