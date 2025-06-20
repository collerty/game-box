package com.example.gamehub.features.JorisJump.logic

import com.example.gamehub.features.JorisJump.model.*
import com.example.gamehub.features.JorisJump.model.PlatformState
import kotlin.random.Random
import android.graphics.RectF

class PlatformLogic {
    // TODO: Implement platform generation, movement, and collision logic
}

fun generateInitialPlatformsList(screenWidth: Float, screenHeight: Float, count: Int, startingId: Int): Pair<List<PlatformState>, Int> {
    val initialPlatforms = mutableListOf<PlatformState>()
    var currentNextId = startingId
    var currentY = screenHeight - PLATFORM_HEIGHT_DP - 5f
    val platformMoveRange = screenWidth * 0.20f

    for (i in 0 until count) {
        if (initialPlatforms.size >= count && i > 0) break
        val xPos = if (i == 0) (screenWidth / 2) - (PLATFORM_WIDTH_DP / 2) else Random.nextFloat() * (screenWidth - PLATFORM_WIDTH_DP)
        val yPos = if (i == 0) currentY else currentY - (PLATFORM_HEIGHT_DP * Random.nextInt(4, 9)).toFloat() - Random.nextFloat() * PLATFORM_HEIGHT_DP * 2.5f
        val shouldMove = Random.nextInt(0, 3) == 0
        val shouldHaveSpring = Random.nextInt(0, 8) == 0

        initialPlatforms.add(
            PlatformState(
                id = currentNextId++, x = xPos, y = yPos,
                isMoving = if (i == 0) false else shouldMove,
                movementDirection = if (Random.nextBoolean()) 1 else -1,
                movementSpeed = PLATFORM_BASE_MOVE_SPEED + Random.nextFloat() * PLATFORM_MOVE_SPEED_VARIATION,
                movementRange = platformMoveRange, originX = xPos,
                hasSpring = if (i == 0) false else shouldHaveSpring,
                springJumpFactor = if (shouldHaveSpring) 1.65f + Random.nextFloat() * 0.5f else 1.0f
            )
        )
        if (i > 0) currentY = yPos
    }
    return Pair(initialPlatforms.toList(), currentNextId)
}

fun movePlatforms(platforms: List<PlatformState>, screenWidthDp: Float): List<PlatformState> =
    platforms.map { p ->
        if (p.isMoving) {
            var newX = p.x + (p.movementSpeed * p.movementDirection)
            var newDirection = p.movementDirection
            if (p.movementDirection == 1 && newX > p.originX + p.movementRange) { newX = p.originX + p.movementRange; newDirection = -1 }
            else if (p.movementDirection == -1 && newX < p.originX - p.movementRange) { newX = p.originX - p.movementRange; newDirection = 1 }
            newX = newX.coerceIn(0f, screenWidthDp - PLATFORM_WIDTH_DP)
            p.copy(x = newX, movementDirection = newDirection)
        } else p
    }

fun cleanupPlatforms(platforms: List<PlatformState>, totalScrollOffsetDp: Float, screenHeightDp: Float): List<PlatformState> =
    platforms.filter { it.y > totalScrollOffsetDp - PLATFORM_HEIGHT_DP * 2 && it.y < totalScrollOffsetDp + screenHeightDp + PLATFORM_HEIGHT_DP * 2 }

fun checkPlayerPlatformCollision(
    playerState: PlayerState,
    platforms: List<PlatformState>,
    onSpring: (PlatformState) -> Unit,
    onNormal: (PlatformState) -> Unit
) {
    val playerBottomWorldDp = playerState.yWorldDp + PLAYER_HEIGHT_DP
    val playerRightScreenDp = playerState.xScreenDp + PLAYER_WIDTH_DP
    if (playerState.velocityY > 0) {
        platforms.forEach { platform ->
            val xOverlaps = playerRightScreenDp > platform.x && playerState.xScreenDp < (platform.x + PLATFORM_WIDTH_DP)
            if (xOverlaps) {
                val previousPlayerBottomWorldDp = playerBottomWorldDp - playerState.velocityY
                if (previousPlayerBottomWorldDp <= platform.y && playerBottomWorldDp >= platform.y) {
                    if (platform.hasSpring) onSpring(platform) else onNormal(platform)
                }
            }
        }
    }
}

data class GenerationResult(
    val platforms: List<PlatformState>,
    val enemies: List<EnemyState>,
    val nextPlatformId: Int,
    val nextEnemyId: Int
)

fun generatePlatformsAndEnemies(
    platforms: List<PlatformState>,
    enemies: List<EnemyState>,
    nextPlatformId: Int,
    nextEnemyId: Int,
    totalScrollOffsetDp: Float,
    screenHeightDp: Float,
    screenWidthDp: Float
): GenerationResult {
    val currentPlatformsMutable = platforms.toMutableList()
    currentPlatformsMutable.removeAll { it.y > totalScrollOffsetDp + screenHeightDp + PLATFORM_HEIGHT_DP * 2 }
    val generationCeilingWorldY = totalScrollOffsetDp - screenHeightDp
    var highestExistingPlatformY = currentPlatformsMutable.minOfOrNull { it.y } ?: (totalScrollOffsetDp + screenHeightDp / 2f)
    var generationAttemptsInTick = 0
    val platformMoveRange = screenWidthDp * 0.15f
    var localNextPlatformId = nextPlatformId
    var localNextEnemyId = nextEnemyId
    var localEnemies = enemies.toList()

    while (currentPlatformsMutable.size < MAX_PLATFORMS_ON_SCREEN &&
        highestExistingPlatformY > generationCeilingWorldY &&
        generationAttemptsInTick < MAX_PLATFORMS_ON_SCREEN) {
        generationAttemptsInTick++
        var newPlatformX = Random.nextFloat() * (screenWidthDp - PLATFORM_WIDTH_DP)
        val minVerticalGapFactor = 5.5f; val maxVerticalGapFactor = 9.5f
        val verticalGap = (PLATFORM_HEIGHT_DP * (minVerticalGapFactor + Random.nextFloat() * (maxVerticalGapFactor - minVerticalGapFactor)))
        val newPlatformY = highestExistingPlatformY - verticalGap

        val platformImmediatelyBelow = currentPlatformsMutable.firstOrNull()
        if (platformImmediatelyBelow != null && kotlin.math.abs(newPlatformX - platformImmediatelyBelow.x) < PLATFORM_WIDTH_DP * 0.75f) {
            if (Random.nextFloat() < 0.6) {
                val shiftDirection = if (platformImmediatelyBelow.x < screenWidthDp / 2) 1 else -1
                newPlatformX = (platformImmediatelyBelow.x + shiftDirection * (PLATFORM_WIDTH_DP * 2 + Random.nextFloat() * screenWidthDp * 0.2f))
                    .coerceIn(0f, screenWidthDp - PLATFORM_WIDTH_DP)
            }
        }
        val shouldMovePlatform = Random.nextInt(0, 3) == 0
        val shouldHaveSpringOnPlatform = Random.nextInt(0, 8) == 0
        currentPlatformsMutable.add(0, PlatformState(
            id = localNextPlatformId++, x = newPlatformX, y = newPlatformY,
            isMoving = shouldMovePlatform,
            movementDirection = if (Random.nextBoolean()) 1 else -1,
            movementSpeed = PLATFORM_BASE_MOVE_SPEED + Random.nextFloat() * PLATFORM_MOVE_SPEED_VARIATION,
            movementRange = if(screenWidthDp > 0) platformMoveRange else 50f, originX = newPlatformX,
            hasSpring = shouldHaveSpringOnPlatform,
            springJumpFactor = if (shouldHaveSpringOnPlatform) 1.65f + Random.nextFloat() * 0.5f else 1.0f
        ))
        highestExistingPlatformY = newPlatformY

        // --- ENEMY GENERATION ---
        if (localEnemies.size < MAX_ENEMIES_ON_SCREEN &&
            Random.nextFloat() < ENEMY_SPAWN_CHANCE_PER_PLATFORM_ROW &&
            screenWidthDp > 0f) {

            var attemptSpawn = true
            var enemyX = 0f
            var enemyY = 0f
            val safetyMarginDp = PLATFORM_WIDTH_DP * 1.5f

            for (spawnAttempt in 0..2) {
                enemyX = Random.nextFloat() * (screenWidthDp - ENEMY_WIDTH_DP)
                val enemyYOffsetFromPlatform = (Random.nextFloat() - 0.5f) * PLATFORM_HEIGHT_DP * 8
                enemyY = newPlatformY + enemyYOffsetFromPlatform

                val tooCloseHorizontallyToItsPlatform =
                    (enemyX + ENEMY_WIDTH_DP > newPlatformX - safetyMarginDp &&
                            enemyX < newPlatformX + PLATFORM_WIDTH_DP + safetyMarginDp)
                val tooCloseVerticallyToItsPlatform =
                    kotlin.math.abs(enemyY - newPlatformY) < PLAYER_HEIGHT_DP

                if (tooCloseHorizontallyToItsPlatform && tooCloseVerticallyToItsPlatform) {
                    attemptSpawn = false
                } else {
                    attemptSpawn = true
                    break
                }
            }

            if (attemptSpawn) {
                localEnemies = localEnemies + EnemyState(id = localNextEnemyId++, x = enemyX, y = enemyY)
            }
        }
    }
    return GenerationResult(
        platforms = currentPlatformsMutable.toList(),
        enemies = localEnemies,
        nextPlatformId = localNextPlatformId,
        nextEnemyId = localNextEnemyId
    )
} 