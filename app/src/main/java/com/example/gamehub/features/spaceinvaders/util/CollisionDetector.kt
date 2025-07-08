package com.example.gamehub.features.spaceinvaders.util

import com.example.gamehub.features.spaceinvaders.models.Bullet
import com.example.gamehub.features.spaceinvaders.models.Bunker
import com.example.gamehub.features.spaceinvaders.models.Enemy
import com.example.gamehub.features.spaceinvaders.models.GameEntity
import com.example.gamehub.features.spaceinvaders.models.Player
import com.example.gamehub.features.spaceinvaders.models.UFO

class CollisionDetector {

    fun checkCollision(entity1: GameEntity, entity2: GameEntity): Boolean {
        if (!entity1.isActive || !entity2.isActive) return false

        return entity1.x < entity2.x + entity2.width &&
               entity1.x + entity1.width > entity2.x &&
               entity1.y < entity2.y + entity2.height &&
               entity1.y + entity1.height > entity2.y
    }

    fun checkBulletEnemyCollisions(
        bullets: List<Bullet>,
        enemies: Array<Array<Enemy>>,
        onEnemyHit: (Enemy) -> Unit
    ) {
        bullets.filter { it.isActive }.forEach { bullet ->
            enemies.flatten().filter { it.isAlive }.forEach { enemy ->
                if (checkCollision(bullet, enemy)) {
                    bullet.isActive = false
                    enemy.isAlive = false
                    onEnemyHit(enemy)
                }
            }
        }
    }

    fun checkBulletUfoCollision(
        bullets: List<Bullet>,
        ufo: UFO,
        onUfoHit: () -> Unit
    ) {
        if (!ufo.isActive) return

        bullets.filter { it.isActive }.forEach { bullet ->
            if (checkCollision(bullet, ufo)) {
                bullet.isActive = false
                ufo.isActive = false
                onUfoHit()
            }
        }
    }

    fun checkBulletPlayerCollision(
        bullets: List<Bullet>,
        player: Player,
        onPlayerHit: () -> Unit
    ) {
        bullets.filter { it.isActive }.forEach { bullet ->
            if (checkCollision(bullet, player)) {
                bullet.isActive = false
                onPlayerHit()
            }
        }
    }

    fun checkBulletBunkerCollisions(
        bullets: List<Bullet>,
        bunkers: List<Bunker>
    ) {
        bullets.filter { it.isActive }.forEach { bullet ->
            for (bunker in bunkers) {
                if (bunker.isHit(bullet.x, bullet.y)) {
                    bullet.isActive = false
                    bunker.takeDamage()
                    break
                }
            }
        }
    }
}

