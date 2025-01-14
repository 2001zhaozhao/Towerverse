package yz.cool.towerverse.simulation

import kotlinx.serialization.Serializable
import yz.cool.towerverse.simulation.action.Action
import yz.cool.towerverse.simulation.game.Enemy
import yz.cool.towerverse.simulation.game.MapTile
import yz.cool.towerverse.simulation.game.Projectile

/**
 * A serializable object that mirrors the mutable state portion of the game simulation.
 *
 * This object can be serialized and hashed to compare the state of the simulation between an expected and actual
 * value, thereby allowing the server to verify the client's gameplay.
 *
 * Note: all properties in this class can be accessed from the [GameSimulation] class too.
 */
@Serializable
class GameSimulationState(
    val tiles: List<List<MapTile>>
) {
    var tick: Long = 0
    var money: Long = 0
    var score: Long = 0
    var playerHealth: Double = 0.0
    val actions: LinkedHashMap<Long, ArrayList<Action>> = LinkedHashMap()
    val enemies: ArrayList<Enemy> = ArrayList()
    val projectiles: ArrayList<Projectile> = ArrayList()
    val enemySpawner: EnemySpawner = EnemySpawner()
}