package yz.cool.towerverse.simulation.game

import kotlinx.serialization.Serializable
import yz.cool.towerverse.gameobject.TowerType
import yz.cool.towerverse.simulation.GameSimulation

/**
 * Represents a Tower.
 */
@Serializable
data class Tower(
    val type: TowerType,
    var level: Int = 1
) {
    val turrets: List<TowerTurretState> = type.turrets.map { TowerTurretState(it) }

    // ========== Some computed properties
    val isAtMaxLevel: Boolean get() = level >= type.rarity.maxLevel

    val upgradeCost: Int get() {
        val cost = type.cost
        return cost * level
    }

    val totalCost: Int get() {
        val cost = type.cost
        return cost * (((level * (level - 1)) / 2) + 1) // x1, x2, x4, x7, x11, etc.
    }

    val refundCost: Int get() {
        return (totalCost * type.refundPercentage).toInt()
    }

    val levelDamageModifier: Double get() {
        return (((level * (level - 1.0)) / 2.0) + 1) // x1, x2, x4, x7, x11, etc.
    }

    val levelRangeModifier: Double get() {
        // Extra range bonus to incentivize upgrading towers (otherwise spamming level 1's will be better due to mazing)
        return 0.8 + level * 0.2
    }

    /**
     * Ticks this tower.
     */
    fun GameSimulation.tick(tileX: Int, tileY: Int) {
        val baseRangeModifier = type.baseRangeModifier
        val baseDamageModifier = type.baseDamageModifier
        val baseAttackRateModifier = type.baseAttackRateModifier
        turrets.forEach {
            with(it) {
                tick(tileX, tileY,
                    baseRangeModifier, levelRangeModifier,
                    baseDamageModifier, levelDamageModifier,
                    baseAttackRateModifier
                )
            }
        }
    }
}