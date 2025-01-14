package yz.cool.towerverse.simulation.game

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import yz.cool.towerverse.gameobject.ProjectileType
import yz.cool.towerverse.simulation.GameSimulation
import yz.cool.towerverse.simulation.ui.CircleParticle
import yz.cool.towerverse.simulation.ui.SplashParticle
import kotlin.math.*
import kotlin.random.Random

@Serializable
class Projectile(
    val type: ProjectileType,
    var x: Double = 0.0,
    var y: Double = 0.0,
    val damageMultiplier: Double = 1.0,
    var towerRange: Double = 0.0
) {
    var vx: Double = 0.0
    var vy: Double = 0.0
    /**
     * Sets the projectile velocity to the target position.
     */
    fun setVelocityToTarget(targetX: Double, targetY: Double) {
        val dx = targetX - x
        val dy = targetY - y
        val distance = sqrt(dx * dx + dy * dy)
        vx = dx / distance * type.speed
        vy = dy / distance * type.speed
    }

    /**
     * Applies random spread to the projectile.
     */
    fun applySpread(spreadDegrees: Double, random: Random) {
        if(spreadDegrees == 0.0) return
        val velocityMagnitude = sqrt(vx * vx + vy * vy)
        val currentAngle = atan2(vy, vx)
        val newAngle = currentAngle + (random.nextDouble() - 0.5) * spreadDegrees * PI / 180.0
        vx = velocityMagnitude * cos(newAngle)
        vy = velocityMagnitude * sin(newAngle)
    }

    var z: Double = type.zInitial
    var vz: Double = type.zSpeed

    var numEnemiesHit: Int = 0
    /** Only used if "type.multipleHitsToSameEnemy" is false */
    var enemiesHit: HashSet<Enemy>? = null
    var rangeTravelled: Double = 0.0

    @Transient
    val renderSizeMultiplier = sqrt(sqrt(damageMultiplier))

    /**
     * Ticks this projectile. Returns whether to despawn it.
     */
    fun GameSimulation.tick(): Boolean {
        x += vx * 0.05
        y += vy * 0.05
        rangeTravelled += sqrt(vx * vx + vy * vy) * 0.05
        z += vz * 0.05
        vz += type.zAcceleration * 0.05
        // Despawn if out of bounds
        if(x < 0 || x >= map.width || y < 0 || y >= map.height) return true
        // Despawn if out of range
        if(type.disappearWhenOutOfRange && rangeTravelled > towerRange) {
            if(type.splashWhenDisappear) splashDamage()
            return true
        }
        // Hitting ground
        if(z <= 0) {
            if(type.splashWhenHittingGround) splashDamage()
            return true
        }
        // Hitting enemy
        for(enemy in enemies) {
            val distX = x - enemy.x
            val distY = y - enemy.y
            val hitDistance = (type.size + enemy.type.size) * 0.5
            if(
                abs(distX) < hitDistance && abs(distY) < hitDistance &&
                distX * distX + distY * distY <= hitDistance * hitDistance &&
                z >= enemy.type.z && z <= enemy.type.z + enemy.type.height &&
                enemiesHit?.contains(enemy) != true
            ) {
                damage(enemy)
                numEnemiesHit += 1
                if(numEnemiesHit >= type.maxHits) return true
                if(!type.multipleHitsToSameEnemy) {
                    if(enemiesHit == null) enemiesHit = HashSet()
                    enemiesHit!! += enemy
                }
            }
        }
        return false
    }

    /** Damages this enemy and spawns a damage particle */
    private fun GameSimulation.damage(enemy: Enemy) {
        val damage = type.damage
        if(damage != null) {
            // Damage enemy
            with(enemy) { damage(damage, damageMultiplier) }
        }

        // Spawn a damage particle
        ui?.apply {
            if(type.damageParticleColor != null && type.damageParticleSize > 0.0) {
                particles += CircleParticle(x, y, type.damageParticleSize, type.damageParticleColor!!, lifetime = 0.15)
            }
        }

        // Do splash damage
        if(type.splashWhenHittingEnemy) {
            // Don't damage enemy again if we have already dealt direct damage
            splashDamage(excludeEnemy = if(damage == null) null else enemy)
        }
    }

    /** Deals splash damage at the projectile position. Optionally excludes an enemy that has been hit directly */
    private fun GameSimulation.splashDamage(excludeEnemy: Enemy? = null) {
        val splashDamage = type.splashDamage ?: return
        val splashRadius = type.splashRadius

        getEnemiesIntersectingRadius(x, y, splashRadius).forEach {
            if(it != excludeEnemy) { // Don't damage the same enemy twice
                with(it) { damage(splashDamage, damageMultiplier) }
            }
        }
        // Spawn a splash particle
        ui?.apply {
            if(type.splashParticleColor != null && splashRadius > 0.0) {
                particles += SplashParticle(x, y, splashRadius, type.splashParticleColor!!, 1.0)
            }
        }
    }
}