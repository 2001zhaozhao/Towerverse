package yz.cool.towerverse.gameobject

import kotlinx.serialization.Serializable
import yz.cool.towerverse.model.GameObjectListReference
import yz.cool.towerverse.model.GameObjectMapReference
import yz.cool.towerverse.model.GameObjectReference
import yz.cool.towerverse.types.TileVec

/**
 * Represents a definition of a map in the game, including its size and generation behavior.
 *
 * @param width The width of the map in tiles.
 * @param height The height of the map in tiles.
 * @param exits The list of exit points in the map. At least one exit is required.
 * Defaults to the top left corner (tile 0,0).
 * @param startingMoney The starting money of the player.
 * @param playerHealth The max health of the player.
 * @param isAllExitsMustBeReachable If true, then each exit must be reachable from at least one enemy spawnpoint
 * for a tower/tile placement to be valid. If false then some exits can be completely blocked off and as long as one
 * exit is reachable, a tower/tile placement will be valid. Defaults to true.
 */
@Serializable
data class GameMap(
    var width: Int = 0,
    var height: Int = 0,
    var exits: List<TileVec> = listOf(TileVec(0, 0)),
    var isAllExitsMustBeReachable: Boolean = true,
    var startingMoney: Long = 0,
    var playerHealth: Double = 0.0,
    private val defaultTileTypeRef: GameObjectReference<MapTileType> = GameObjectReference(),
    private val exitTileTypeRef: GameObjectReference<MapTileType?> = GameObjectReference(),
    private val tileTypeWeightsRef: GameObjectMapReference<MapTileType, Double> = GameObjectMapReference(),
    private val spawnpointsRef: GameObjectListReference<Spawnpoint> = GameObjectListReference(),
    private val waveDefinitionRef: GameObjectReference<WaveDefinition> = GameObjectReference(),
    override var id: String = "",
    override var name: String = ""
) : GameObject() {
    /** The default tile type. This is used when another tile type cannot be placed because it would block enemies
     * from reaching the exit tile. Enemies must be able to pass through this. Required. */
    var defaultTileType by defaultTileTypeRef

    /** The exit tile type. If missing, will default to the default tile type. */
    var exitTileType by exitTileTypeRef

    /** The tile type weights used during map generation.
     * Note that a tile will not be placed if it would block enemies from reaching the exit tile.
     * The generation order will be random. */
    var tileTypeWeights by tileTypeWeightsRef

    /** The list of spawnpoints in the map. Required. */
    var spawnpoints by spawnpointsRef

    /** The wave definition of the map. Required. */
    var waveDefinition by waveDefinitionRef

    fun getAllTileVectors(): List<TileVec> {
        val result = mutableListOf<TileVec>()
        for (x in 0 ..< width) {
            for (y in 0 ..< height) {
                result.add(TileVec(x, y))
            }
        }
        return result
    }

    companion object {
        operator fun invoke(action: GameMap.() -> Unit) = GameMap().applyAtCompile(action)
    }
}