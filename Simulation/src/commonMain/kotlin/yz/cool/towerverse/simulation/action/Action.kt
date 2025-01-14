package yz.cool.towerverse.simulation.action

import kotlinx.serialization.Serializable
import yz.cool.towerverse.gameobject.TowerType
import yz.cool.towerverse.simulation.GameSimulation
import yz.cool.towerverse.simulation.game.Tower
import yz.cool.towerverse.simulation.ui.FloatingTextParticle
import yz.cool.towerverse.types.Color

/**
 * Represents an in-game action that can be performed by the user, such as placing, upgrading, and removing a tower.
 *
 * Queued actions will be executed at the beginning of the simulation of the next game tick.
 */
@Serializable
sealed class Action {
    /**
     * Whether the action is valid. If valid, return null. If not valid returns an error message.
     *
     * If the action is not valid, the server verifying the action will consider the entire game save invalid.
     * (This is to defend against the client including large numbers of invalid actions in the game save.)
     */
    abstract fun GameSimulation.checkValid(): String?

    /**
     * Performs the action. The action is guaranteed to be valid. Note that sometimes an action may produce no outcome.
     */
    abstract fun GameSimulation.perform()
}

/**
 * The action of placing a tower.
 */
@Serializable
data class ActionTowerPlace(val x: Int, val y: Int, val towerTypeId: String) : Action() {
    override fun GameSimulation.checkValid(): String? {
        val tile = tiles.getOrNull(x)?.getOrNull(y) ?: return "Invalid coordinates"
        val towerType = env.getGameObjectByType<TowerType>(towerTypeId) ?: return "Invalid tower type"
        if(tile.tower != null) return "Tile already has a tower"
        if(!tile.type.isTowerPlaceable) return "A tower cannot be placed on this tile type"
        if(money < towerType.cost) return "Not enough money"
        if(towerType.isSolid && !canTileBeBlockedBySolidTower(x, y)) return "Must leave a path to the exit"
        return null
    }

    override fun GameSimulation.perform() {
        val tile = tiles[x][y]
        val towerType = env.getGameObjectByType<TowerType>(towerTypeId)!!

        money -= towerType.cost
        val tower = Tower(towerType)
        tile.tower = tower
        onTowerPlaced(x, y, tower)

        ui?.apply{
            particles += FloatingTextParticle(x + 0.5, y + 0.5, "-$${towerType.cost}", color = Color.RED)
        }
    }
}

/**
 * The action of upgrading a tower.
 */
@Serializable
data class ActionTowerUpgrade(val x: Int, val y: Int) : Action() {
    override fun GameSimulation.checkValid(): String? {
        val tower = tiles.getOrNull(x)?.getOrNull(y)?.tower ?: return "No tower on tile"
        if(tower.level >= tower.type.rarity.maxLevel) return "Tower is at max level"
        if(money < tower.upgradeCost) return "Not enough money"
        return null
    }

    override fun GameSimulation.perform() {
        val tower = tiles[x][y].tower!!
        val cost = tower.upgradeCost
        money -= cost
        tower.level++
        onTowerUpgraded(x, y, tower)

        ui?.apply{
            particles += FloatingTextParticle(x + 0.5, y + 0.5, "-$$cost", color = Color.RED)
        }
    }
}

/**
 * The action of removing a tower.
 */
@Serializable
data class ActionTowerRemove(val x: Int, val y: Int) : Action() {
    override fun GameSimulation.checkValid(): String? {
        if(tiles.getOrNull(x)?.getOrNull(y)?.tower == null) return "No tower on tile"
        return null
    }

    override fun GameSimulation.perform() {
        val tower = tiles[x][y].tower!!
        money += tower.refundCost
        tiles[x][y].tower = null
        onTowerRemoved(x, y, tower)

        ui?.apply{
            particles += FloatingTextParticle(x + 0.5, y + 0.5, "+$${tower.refundCost}", color = Color.GREEN)
        }
    }
}