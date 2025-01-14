package yz.cool.towerverse.gameobject

import kotlinx.serialization.Serializable
import yz.cool.towerverse.model.GameObjectListReference
import yz.cool.towerverse.model.GameObjectReference

/**
 * Represents an enemy type.
 *
 * @param description The enemy type's description. Optional.
 * @param size The size of the enemy in tiles.
 * @param health The health of the enemy.
 * @param speed The speed of the enemy in tiles per second. Defaults to 1.
 * @param weight The weight of the enemy used to determine collision with other enemies. Defaults to 1.
 * @param z The height of the bottom of the enemy hitbox off the ground. Defaults to 0.
 * @param height The z-height of the enemy roughly in tiles. Defaults to 1.
 * A value of 0 means the enemy will be defeated by any damage event, including those that do 0 damage.
 */
@Serializable
data class EnemyType(
    var description: String? = null,
    var size: Double = 0.0,
    var health: Double = 0.0,
    var speed: Double = 1.0,
    var weight: Double = 1.0,
    var z: Double = 0.0,
    var height: Double = 1.0,
    private val appearanceRef: GameObjectReference<Appearance> = GameObjectReference(AppearanceCircle()),
    private val tagsRef: GameObjectListReference<EnemyTag> = GameObjectListReference(),
    private val spawnOnDeathRef: GameObjectListReference<EnemyType> = GameObjectListReference(),
    override var id: String = "",
    override var name: String = ""
) : GameObject() {
    /** The appearance of the enemy type. This will be scaled to the enemy's size. Defaults to a white circle. */
    var appearance by appearanceRef
    /** List of tags for the enemy type. */
    var tags by tagsRef
    /** List of enemies to spawn when this enemy is defeated. */
    var spawnOnDeath by spawnOnDeathRef

    companion object {
        operator fun invoke(action: EnemyType.() -> Unit) = EnemyType().applyAtCompile(action)
    }
}

/**
 * A named enemy tag that represents a "trait" of an enemy.
 * Affects attributes such as how much damage an enemy takes from specific towers.
 *
 * @param description The enemy tag's description. Optional.
 */
@Serializable
data class EnemyTag(
    var description: String? = null,
    override var id: String = "",
    override var name: String = ""
) : GameObject() {
    companion object {
        operator fun invoke(action: EnemyTag.() -> Unit) = EnemyTag().applyAtCompile(action)
    }
}