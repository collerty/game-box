package com.example.gamehub.features.spaceinvaders.classes

class EnemyController {
    val enemyMap: Array<Array<Int>> = arrayOf(
        arrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        arrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        arrayOf(2, 2, 2, 3, 3, 3, 3, 2, 2, 2),
        arrayOf(2, 2, 2, 3, 3, 3, 3, 2, 2, 2),
        arrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        arrayOf(2, 2, 2, 2, 2, 2, 2, 2, 2, 2)
    )


    val enemies: Array<Array<Enemy>> = createEnemies()

    fun createEnemies(): Array<Array<Enemy>> {
        return Array(enemyMap.size) { rowIndex ->
            Array(enemyMap[rowIndex].size) { colIndex ->
                val type = mapToEnemyType(enemyMap[rowIndex][colIndex])
                Enemy(x = rowIndex, y = colIndex, isAlive = true, type = type)
            }
        }
    }

    fun mapToEnemyType(value: Int): EnemyType = when (value) {
        1 -> EnemyType.SHOOTER
        2 -> EnemyType.MIDDLE
        3 -> EnemyType.BOTTOM
        else -> throw IllegalArgumentException("Unknown enemy type value: $value")
    }
}