package yz.cool.towerverse.gameobject

import kotlinx.serialization.Serializable
import yz.cool.towerverse.model.GameObjectListReference
import yz.cool.towerverse.types.TileVec

/**
 * A spawnpoint that enemies can spawn at.
 *
 * @param location The location of the spawnpoint.
 */
@Serializable
data class Spawnpoint(
    var location: TileVec = TileVec(),
    private val tagsRef: GameObjectListReference<SpawnpointTag> = GameObjectListReference(),
    override var id: String = "",
    override var name: String = "",
) : GameObject() {
    /** A list of tags that can be used to reference this spawnpoint in a wave definition. */
    var tags by tagsRef

    companion object {
        operator fun invoke(action: Spawnpoint.() -> Unit) = Spawnpoint().applyAtCompile(action)
    }
}

/**
 * A named spawnpoint tag that can be used to reference a type of spawnpoint in a wave definition.
 */
@Serializable
data class SpawnpointTag(
    override var id: String = "",
    override var name: String = "",
) : GameObject() {
    companion object {
        operator fun invoke(action: SpawnpointTag.() -> Unit) = SpawnpointTag().applyAtCompile(action)
    }
}