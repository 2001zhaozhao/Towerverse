package yz.cool.towerverse.gameobject

import kotlinx.serialization.Serializable
import yz.cool.towerverse.model.GameObjectReference

/**
 * Represents a tile type on the game map.
 *
 * @param isSolid Whether the tile is solid, meaning that enemies cannot walk on it. Defaults to false.
 * @param isTowerPlaceable Whether towers can be placed on the tile. Defaults to true.
 * @param appearanceRef Reference to the appearance of the tile.
 * @param movementSpeedModifier The enemy movement speed modifier of the tile.
 * The pathfinding cost will be affected proportional to this value. Defaults to 1.
 * @param pathfindingCostModifier The pathfinding cost modifier of the tile,
 * which stacks with any effects from the movement speed modifier.
 */
@Serializable
data class MapTileType(
    var isSolid: Boolean = false,
    var isTowerPlaceable: Boolean = true,
    private val appearanceRef: GameObjectReference<Appearance> = GameObjectReference(AppearanceSquare()),
    var movementSpeedModifier: Double = 1.0,
    var pathfindingCostModifier: Double = 1.0,
    override var id: String = "",
    override var name: String = ""
) : GameObject() {
    /** The appearance of the tile. */
    var appearance: Appearance by appearanceRef

    companion object {
        operator fun invoke(action: MapTileType.() -> Unit) = MapTileType().applyAtCompile(action)
    }
}