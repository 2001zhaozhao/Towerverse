package yz.cool.towerverse.simulation.game

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import yz.cool.towerverse.gameobject.*
import yz.cool.towerverse.simulation.GameSimulation
import yz.cool.towerverse.simulation.ui.LineParticle
import yz.cool.towerverse.simulation.ui.SplashParticle
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * The state of a tower's turret.
 */
@Serializable
class TowerTurretState(
    val type: TowerTurret
) {
    /**
     * The angle that the turret is facing in radians.
     */
    var angle: Double = 0.0

    fun updateTurretAngle(tileX: Int, tileY: Int) {
        if(targets.isEmpty()) return
        val target = targets[0]
        angle = atan2(target.y - (tileY + 0.5), target.x - (tileX + 0.5))
        if(angle.isNaN()) angle = 0.0
    }

    /**
     * Time that has passed since the last attack.
     */
    var timeSinceLastAttack: Double = 0.0

    /**
     * The set of enemies that the turret is currently targeting.
     * The tower will rotate to face its most recently acquired target.
     *
     * Note that this is not used if the tower can have infinite targets.
     */
    @Transient // don't serialize
    val targets = ArrayList<Enemy>()

    /**
     * Ticks this tower turret.
     */
    fun GameSimulation.tick(
        tileX: Int, tileY: Int,
        baseRangeModifier: Double, levelRangeModifier: Double,
        baseDamageModifier: Double, levelDamageModifier: Double,
        baseAttackRateModifier: Double
    ) {
        val x = tileX + 0.5 + type.offsetX
        val y = tileY + 0.5 + type.offsetY
        val numTargets = type.simultaneousTargets

        val attackDelay = type.attackDelay / baseAttackRateModifier
        timeSinceLastAttack += 0.05
        if(timeSinceLastAttack < attackDelay) { // Skip ticking if we haven't reached the attack delay yet
            updateTurretAngle(tileX, tileY)
            return
        }

        fun enemyIntersectsShape(enemy: Enemy, shape: TowerTargetingShape): Boolean {
            return when(shape) {
                is TowerTargetingShapeCompound -> {
                    shape.shapes.all { enemyIntersectsShape(enemy, it) }
                }
                is TowerTargetingShapeCircle -> {
                    val range = shape.radius * baseRangeModifier *
                            ((levelRangeModifier - 1.0) * shape.radiusRangeModifierEffect + 1.0) + enemy.type.size
                    val xDist = abs(enemy.x - x)
                    val yDist = abs(enemy.y - y)
                    return xDist < range && yDist < range && (xDist * xDist + yDist * yDist) < range * range
                }
                is TowerTargetingShapeRectangle -> {
                    val width = (shape.width * baseRangeModifier *
                            (((levelRangeModifier - 1.0) * shape.widthRangeModifierEffect) + 1.0)) + enemy.type.size
                    val height = shape.height * baseRangeModifier *
                            ((levelRangeModifier - 1.0) * shape.heightRangeModifierEffect + 1.0) + enemy.type.size
                    val offsetX = shape.offsetX *
                            ((levelRangeModifier - 1.0) * shape.offsetXRangeModifierEffect + 1.0)
                    val offsetY = shape.offsetY *
                            ((levelRangeModifier - 1.0) * shape.offsetYRangeModifierEffect + 1.0)
                    // Technically a bit inaccurate at the corners but good enough
                    return x + offsetX - width / 2 - enemy.type.size < enemy.x &&
                            x + offsetX + width / 2 - enemy.type.size > enemy.x &&
                            y + offsetY - height / 2 - enemy.type.size < enemy.y &&
                            y + offsetY + height / 2 - enemy.type.size > enemy.y
                }
                else -> false
            }
        }

        var successfullyAttacked = false
        val range = type.shape
        if(numTargets == 0) {
            // Attack all enemies in range if this tower can have infinite targets
            var potentialTargets: ArrayList<Enemy>? = null // optimization (towers usually don't find a target)
            for(enemy in enemies) {
                if(enemyIntersectsShape(enemy, range) && !enemy.isDead) {
                    if(potentialTargets == null) potentialTargets = ArrayList()
                    potentialTargets += enemy
                }
            }
            if(potentialTargets != null) {
                for(enemy in potentialTargets) {
                    attack(x, y, enemy, baseRangeModifier, levelRangeModifier, baseDamageModifier, levelDamageModifier)
                    successfullyAttacked = true
                }
            }
        }
        else {
            // Remove any targets that are no longer in range
            targets.removeAll {
                !enemyIntersectsShape(it, range) || it.isDead
            }

            // Try to acquire new targets up to the limit
            if(targets.size < numTargets) {
                var potentialTargets: ArrayList<Enemy>? = null // optimization (towers usually don't find a target)
                for(enemy in enemies) {
                    if(enemyIntersectsShape(enemy, range) && !enemy.isDead) {
                        if(potentialTargets == null) potentialTargets = ArrayList()
                        potentialTargets += enemy
                    }
                }
                // Target closer enemies first
                if(potentialTargets != null) {
                    potentialTargets.sortBy {
                        val xDist = it.x - x
                        val yDist = it.y - y
                        xDist * xDist + yDist * yDist
                    }
                    var i = 0
                    while(targets.size < numTargets && i < potentialTargets.size) {
                        val enemy = potentialTargets[i]
                        if(!targets.contains(enemy)) targets += enemy
                        i++
                    }
                }
            }

            // Attack all targets
            for(target in targets) {
                attack(x, y, target, baseRangeModifier, levelRangeModifier, baseDamageModifier, levelDamageModifier)
                successfullyAttacked = true
            }
        }

        if(successfullyAttacked) { // Decrease timeSinceLastAttack by attack delay
            timeSinceLastAttack -= attackDelay
        }
        else timeSinceLastAttack = attackDelay // Prevent timeSinceLastAttack from building up indefinitely

        updateTurretAngle(tileX, tileY)
    }

    /** Attacks an enemy. */
    private fun GameSimulation.attack(
        x: Double, y: Double,
        enemy: Enemy,
        baseRangeModifier: Double, levelRangeModifier: Double,
        baseDamageModifier: Double, levelDamageModifier: Double
    ) {
        val attack = type.attack

        val damage = attack.damage
        if(damage != null) {
            // Directly damage the enemy
            with(enemy) { damage(damage, baseDamageModifier * levelDamageModifier) }
            // Spawn a damage particle
            ui?.apply {
                if(attack.damageParticleColor != null && attack.damageParticleLineWidth > 0.0) {
                    particles += LineParticle(x, y, enemy.x, enemy.y,
                        attack.damageParticleColor!!,
                        lineWidth = attack.damageParticleLineWidth * sqrt(sqrt(levelDamageModifier)),
                        lifetime = 0.2)
                }
            }

            // Deal splash damage as well if applicable
            val splashDamage = attack.splashDamage
            if(splashDamage != null) {
                getEnemiesIntersectingRadius(enemy.x, enemy.y, attack.splashRadius).forEach {
                    if(it != enemy) { // Don't damage the same enemy twice
                        with(it) { damage(splashDamage, baseDamageModifier * levelDamageModifier) }
                    }
                }
                // Spawn a splash particle
                ui?.apply {
                    if(attack.splashParticleColor != null && attack.splashRadius > 0.0) {
                        particles += SplashParticle(enemy.x, enemy.y, attack.splashRadius, attack.splashParticleColor!!, 1.0)
                    }
                }
            }
        }
        val projectileType = attack.projectile
        if(projectileType != null) {
            // Spawn a projectile
            val spread = attack.projectileSpread
            projectiles += Projectile(projectileType, x, y,
                damageMultiplier = baseDamageModifier * levelDamageModifier,
                towerRange = getShapeMaxRange(type.shape, baseRangeModifier, levelRangeModifier)
            ).apply {
                setVelocityToTarget(enemy.x, enemy.y)
                applySpread(spread, random)
            }
        }
    }

    /** Used to calculate how far a projectile can travel before it may disappear. */
    private fun getShapeMaxRange(
        shape: TowerTargetingShape, baseRangeModifier: Double, levelRangeModifier: Double): Double {
        when(shape) {
            is TowerTargetingShapeCompound -> {
                return shape.shapes.maxOf { getShapeMaxRange(it, baseRangeModifier, levelRangeModifier) }
            }
            is TowerTargetingShapeCircle -> {
                val range = shape.radius * baseRangeModifier *
                        ((levelRangeModifier - 1.0) * shape.radiusRangeModifierEffect + 1.0)
                return range
            }
            is TowerTargetingShapeRectangle -> {
                val width = (shape.width * baseRangeModifier *
                        (((levelRangeModifier - 1.0) * shape.widthRangeModifierEffect) + 1.0))
                val height = shape.height * baseRangeModifier *
                        ((levelRangeModifier - 1.0) * shape.heightRangeModifierEffect + 1.0)
                val offsetX = shape.offsetX *
                        ((levelRangeModifier - 1.0) * shape.offsetXRangeModifierEffect + 1.0)
                val offsetY = shape.offsetY *
                        ((levelRangeModifier - 1.0) * shape.offsetYRangeModifierEffect + 1.0)
                return sqrt((abs(width) + abs(offsetX)) * (abs(width) + abs(offsetX)) +
                        (abs(height) + abs(offsetY)) * (abs(height) + abs(offsetY)))
            }
        }
    }
}