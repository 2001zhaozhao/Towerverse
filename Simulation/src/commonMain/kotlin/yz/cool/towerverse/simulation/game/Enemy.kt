package yz.cool.towerverse.simulation.game

import kotlinx.serialization.Serializable
import yz.cool.towerverse.gameobject.EnemyType
import yz.cool.towerverse.gameobject.TowerDamage
import yz.cool.towerverse.simulation.GameSimulation
import yz.cool.towerverse.simulation.ui.FloatingTextParticle
import yz.cool.towerverse.types.Color
import kotlin.math.cbrt
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Represents an enemy on the board.
 */
@Serializable
class Enemy(
    val type: EnemyType,
    var x: Double,
    var y: Double,
    val level: Int
) {
    val maxHealth = type.health * (0.9 + level * 0.1).pow(1.15) // Max health scaling based on level
    var health: Double = maxHealth
    var isDead: Boolean = false // Set to true to remove this tower
    var vx: Double = 0.0
    var vy: Double = 0.0
    var lifetime: Double = 0.0

    var currentTileX: Int = -1
    var currentTileY: Int = -1
    var targetTileX: Int = -1
    var targetTileY: Int = -1

    /**
     * Ticks the enemy. Returns whether to despawn it.
     */
    fun GameSimulation.tick(): Boolean {
        if(isDead) return true
        lifetime += 0.05
        // Movement of the enemy, taking care not to let them go off the map
        x = (x + vx * 0.05).coerceIn(0.0..(map.width - 0.0000001))
        y = (y + vy * 0.05).coerceIn(0.0..(map.height - 0.0000001))

        // Detect if enemy has moved to a new tile
        val tileX = x.toInt()
        val tileY = y.toInt()
        val prevTileX = currentTileX
        val prevTileY = currentTileY
        if(tileX != currentTileX || tileY != currentTileY) {
            currentTileX = tileX
            currentTileY = tileY
            targetTileX = -1
            targetTileY = -1

            if(map.exits.any { it.x == tileX && it.y == tileY }) {
                // Damage the player's base. To be fair to the player, damage is scaled back based on level
                playerHealth -= health / (maxHealth / type.health) / 100.0
                // Despawn the enemy
                isDead = true
                return true
            }
        }

        // Pick new target tile to move to randomly based on cost, excluding the tile the enemy just came from
        if(targetTileX == -1 || targetTileY == -1) {
            val rightCost = if(tileX + 1 < map.width && tileX + 1 != prevTileX)
                flowField.costArray[tileX + 1][tileY] else Double.MAX_VALUE
            val leftCost = if(tileX - 1 >= 0 && tileX - 1 != prevTileX)
                flowField.costArray[tileX - 1][tileY] else Double.MAX_VALUE
            val upCost = if(tileY - 1 >= 0 && tileY - 1 != prevTileY)
                flowField.costArray[tileX][tileY - 1] else Double.MAX_VALUE
            val downCost = if(tileY + 1 < map.height && tileY + 1 != prevTileY)
                flowField.costArray[tileX][tileY + 1] else Double.MAX_VALUE

            val minCost = minOf(minOf(rightCost, leftCost), minOf(upCost, downCost))
            // Chance of choosing a tile decreases exponentially based on difference in cost from the best direction
            // There is still some chance of enemies choosing a non-optimal direction to keep the player guessing...
            val rightWeight = 1 / exp(rightCost - minCost)
            val leftWeight = 1 / exp(leftCost - minCost)
            val upWeight = 1 / exp(upCost - minCost)
            val downWeight = 1 / exp(downCost - minCost)
            val totalWeight = rightWeight + leftWeight + upWeight + downWeight

            var random = random.nextDouble() * totalWeight
            random -= rightWeight
            if (random <= 0) {
                targetTileX = tileX + 1
                targetTileY = tileY
            }
            else {
                random -= leftWeight
                if (random <= 0) {
                    targetTileX = tileX - 1
                    targetTileY = tileY
                }
                else {
                    random -= upWeight
                    if (random <= 0) {
                        targetTileX = tileX
                        targetTileY = tileY - 1
                    }
                    else {
                        targetTileX = tileX
                        targetTileY = tileY + 1
                    }
                }
            }
        }

        // If currently inside a solid tile, try to move out of it
        val tile = tiles[tileX][tileY]
        if(tile.isCurrentlySolid) {
            vx *= 0.7 // Make the push more forceful
            vy *= 0.7

            val distanceFromRight = if(tileX + 1 < map.width && !tiles[tileX + 1][tileY].isCurrentlySolid)
                1 - (x - tileX) else Double.POSITIVE_INFINITY
            val distanceFromLeft = if(tileX - 1 >= 0 && !tiles[tileX - 1][tileY].isCurrentlySolid)
                x - tileX else Double.POSITIVE_INFINITY
            val distanceFromBottom = if(tileY + 1 < map.height && !tiles[tileX][tileY + 1].isCurrentlySolid)
                1 - (y - tileY) else Double.POSITIVE_INFINITY
            val distanceFromTop = if(tileY - 1 >= 0 && !tiles[tileX][tileY - 1].isCurrentlySolid)
                y - tileY else Double.POSITIVE_INFINITY

            val minDistance = minOf(minOf(distanceFromRight, distanceFromLeft), minOf(distanceFromBottom, distanceFromTop))
            if(minDistance == Double.POSITIVE_INFINITY) {
                // We're in a clump of solid tiles, push the enemy to the target tile
                vx += (targetTileX - tileX).toDouble() * 0.5
                vy += (targetTileY - tileY).toDouble() * 0.5
            }
            else {
                // Push the enemy away from the solid tile
                if(distanceFromRight == minDistance) vx += 0.5
                if(distanceFromLeft == minDistance) vx -= 0.5
                if(distanceFromBottom == minDistance) vy += 0.5
                if(distanceFromTop == minDistance) vy -= 0.5
            }
        }

        // Push against other enemies
        for (enemy in enemies) {
            if (enemy != this@Enemy) {
                val totalDistance = (type.size + enemy.type.size) / 2.0
                val dx = x - enemy.x
                val dy = y - enemy.y
                val squaredDistance = dx * dx + dy * dy
                if(
                    dx < totalDistance && dx > -totalDistance && dy < totalDistance && dy > -totalDistance &&
                    squaredDistance < totalDistance * totalDistance && squaredDistance > 0
                ) {
                    val distance = sqrt(squaredDistance)
                    vx += dx / distance * 0.02 * (enemy.type.weight / type.weight)
                    vy += dy / distance * 0.02 * (enemy.type.weight / type.weight)
                }
            }
        }

        // Move towards the target tile
        vx *= 0.95
        vy *= 0.95
        vx += (targetTileX - tileX).toDouble() * type.speed * 0.05
        vy += (targetTileY - tileY).toDouble() * type.speed * 0.05

        // Don't despawn the enemy
        return false
    }

    /**
     * Damages the enemy. This may cause this enemy to be removed from the enemies list.
     */
    fun GameSimulation.damage(damage: TowerDamage, damageMultiplier: Double) {
        if(isDead) return
        var damageAmount = damage.damage * damageMultiplier

        // Apply enemy-tag-specific damage multipliers
        for(tag in type.tags) {
            damage.damageMultiplier[tag]?.let {
                damageAmount *= it
            }
        }

        health -= damageAmount
        if(health <= 0) {
            isDead = true // Trigger enemy removal

            // Grant money reward
            val moneyReward = randomlyRound(sqrt(maxHealth) / 10.0).coerceAtLeast(1)
            money += moneyReward

            // Grant score based on how fast the enemy died
            val scoreReward = randomlyRound(
                sqrt(maxHealth) / cbrt(cbrt(lifetime + 1))
            ).coerceAtLeast(1)
            score += scoreReward

            ui?.apply {
                particles += FloatingTextParticle(x, y, "$$moneyReward",
                    color = Color.YELLOW, size = 0.25 * cbrt(cbrt(moneyReward.toDouble()))
                )
            }

            // Spawn enemies on death
            for(typeToSpawn in type.spawnOnDeath) {
                enemies += Enemy(
                    type = typeToSpawn, x = x, y = y, level = level
                )
            }
        }
    }
}