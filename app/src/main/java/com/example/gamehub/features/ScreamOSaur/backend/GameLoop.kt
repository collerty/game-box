
package com.example.gamehub.features.ScreamOSaur.backend

import androidx.compose.ui.geometry.Rect
import com.example.gamehub.features.ScreamOSaur.model.GameState
import com.example.gamehub.features.ScreamOSaur.model.ScreamOSaurUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GameLoop(
    private val scope: CoroutineScope,
    private val onUpdate: (ScreamOSaurUiState) -> Unit,
    private val onGameOver: () -> Unit,
    private val getState: () -> ScreamOSaurUiState
) {
    private var gameLoopJob: Job? = null
    private val gameLogic = GameLogic()
    private var gameStartTime = 0L
    private var initialDelayHasOccurred = false

    companion object {
        const val INITIAL_GAME_SPEED = 8f
        const val MAX_GAME_SPEED = 18f
    }

    fun start() {
        if (gameLoopJob?.isActive == true) return

        gameLoopJob = scope.launch(Dispatchers.Default) {
            if (!initialDelayHasOccurred) {
                delay(1500)
                gameStartTime = System.currentTimeMillis()
                initialDelayHasOccurred = true
            }

            while (isActive && getState().gameState == GameState.PLAYING) {
                val currentState = getState()
                val newState = updateState(currentState)
                onUpdate(newState)

                if (checkCollision(getState())) {
                    onGameOver()
                    break
                }
                delay(16)
            }
        }
    }

    fun stop() {
        gameLoopJob?.cancel()
        gameLoopJob = null
    }

    fun reset() {
        stop()
        initialDelayHasOccurred = false
        gameStartTime = 0L
    }

    private fun updateState(currentState: ScreamOSaurUiState): ScreamOSaurUiState {
        val currentTime = System.currentTimeMillis()
        val elapsedGameTime = currentTime - gameStartTime

        val updatedObstacles = currentState.obstacles.map {
            it.copy(xPosition = it.xPosition - currentState.gameSpeed)
        }.filter { it.xPosition + it.width > 0 }

        val canvasWidthPx = currentState.gameHeightPx * (16f / 9f)
        val spawnTriggerX = canvasWidthPx * 0.5f
        var newObstaclesCurrent = updatedObstacles

        if (newObstaclesCurrent.isEmpty() && elapsedGameTime > 500) {
            newObstaclesCurrent = listOf(gameLogic.createNewObstacle(canvasWidthPx, currentState))
        } else if (newObstaclesCurrent.isNotEmpty() && newObstaclesCurrent.last().xPosition < spawnTriggerX && newObstaclesCurrent.size < 5) {
            newObstaclesCurrent = newObstaclesCurrent + gameLogic.createNewObstacle(canvasWidthPx, currentState)
        }

        var newScore = currentState.score
        var newSpeed = currentState.gameSpeed
        val scoredObstacles = newObstaclesCurrent.map { obstacle ->
            if (!obstacle.passed && (obstacle.xPosition + obstacle.width) < currentState.dinosaurVisualXPositionPx) {
                newScore++
                newSpeed = (INITIAL_GAME_SPEED + (newScore / 8f)).coerceAtMost(MAX_GAME_SPEED)
                obstacle.copy(passed = true)
            } else {
                obstacle
            }
        }

        return currentState.copy(
            obstacles = scoredObstacles,
            score = newScore,
            gameSpeed = newSpeed
        )
    }

    private fun checkCollision(state: ScreamOSaurUiState): Boolean {
        val dinoTopY = state.dinoTopYOnGroundPx - (state.jumpAnimValue * state.jumpMagnitudePx)
        val dinoHitbox = Rect(
            left = state.dinosaurVisualXPositionPx + state.dinosaurSizePx * 0.3f,
            top = dinoTopY + state.dinosaurSizePx * 0.3f,
            right = state.dinosaurVisualXPositionPx + state.dinosaurSizePx * 0.7f,
            bottom = dinoTopY + state.dinosaurSizePx * 0.8f
        )

        return state.obstacles.any { obs ->
            val obsRect = Rect(
                left = obs.xPosition,
                top = state.gameHeightPx - state.groundHeightPx - obs.height,
                right = obs.xPosition + obs.width,
                bottom = state.gameHeightPx - state.groundHeightPx
            )
            dinoHitbox.overlaps(obsRect)
        }
    }
}