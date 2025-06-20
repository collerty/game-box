package com.example.gamehub.features.JorisJump.logic

import com.example.gamehub.features.JorisJump.model.*
import android.graphics.RectF

class EnemyLogic {
    // TODO: Implement enemy generation, movement, and collision logic
}

fun moveEnemies(enemies: List<EnemyState>, gameTimeMillis: Long): List<EnemyState> =
    enemies.map { enemy ->
        val twitchAngle = (gameTimeMillis * ENEMY_TWITCH_SPEED_FACTOR + enemy.id)
        enemy.copy(
            visualOffsetX = kotlin.math.sin(twitchAngle.toDouble()).toFloat() * ENEMY_TWITCH_AMOUNT_DP,
            visualOffsetY = kotlin.math.cos(twitchAngle.toDouble() * 0.7).toFloat() * ENEMY_TWITCH_AMOUNT_DP
        )
    }

fun cleanupEnemies(enemies: List<EnemyState>, totalScrollOffsetDp: Float, screenHeightDp: Float): List<EnemyState> =
    enemies.filter { it.y < totalScrollOffsetDp + screenHeightDp + ENEMY_HEIGHT_DP * 3 }

fun checkPlayerEnemyCollision(
    playerState: PlayerState,
    enemies: List<EnemyState>,
    totalScrollOffsetDp: Float,
    onHit: (EnemyState) -> Unit
) {
    val playerTopOnScreen = playerState.yWorldDp - totalScrollOffsetDp
    val playerRect = RectF(
        playerState.xScreenDp, playerTopOnScreen,
        playerState.xScreenDp + PLAYER_WIDTH_DP, playerTopOnScreen + PLAYER_HEIGHT_DP
    )
    enemies.forEach { enemy ->
        val enemyTopOnScreen = enemy.y - totalScrollOffsetDp
        val enemyRect = RectF(
            enemy.x, enemyTopOnScreen,
            enemy.x + ENEMY_WIDTH_DP, enemyTopOnScreen + ENEMY_HEIGHT_DP
        )
        if (playerRect.intersect(enemyRect)) {
            onHit(enemy)
            return
        }
    }
} 