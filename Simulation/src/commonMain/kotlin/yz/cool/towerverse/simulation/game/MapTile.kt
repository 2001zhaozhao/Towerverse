package yz.cool.towerverse.simulation.game

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import yz.cool.towerverse.gameobject.MapTileType

/**
 * Represents a map tile.
 */
@Serializable
data class MapTile(
    var type: MapTileType
) {
    var tower: Tower? = null

    /** Whether the tile is temporarily blocked off to calculate whether a new tower placement would be valid. */
    @Transient
    var isTemporarilyBlocked: Boolean = false

    /** Whether the tile is currently solid and blocks pathfinding. */
    val isCurrentlySolid: Boolean get() =
         isTemporarilyBlocked || type.isSolid || tower?.type?.isSolid == true

    /** Computed pathfinding cost of the tile. If solid, will use 1 billion (magic value) */
    val pathfindingCost get() = if(isCurrentlySolid) 1000000000.0 else
        type.pathfindingCostModifier / type.movementSpeedModifier
}