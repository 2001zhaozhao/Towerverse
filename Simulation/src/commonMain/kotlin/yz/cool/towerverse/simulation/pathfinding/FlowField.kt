package yz.cool.towerverse.simulation.pathfinding

import yz.cool.towerverse.simulation.GameSimulation
import yz.cool.towerverse.types.TileVec

/**
 * Represents a flow field that can be built based on the state of the game simulation.
 */
class FlowField(
    val gameSimulation: GameSimulation,
) {
    val width get() = gameSimulation.map.width
    val height get() = gameSimulation.map.height

    val costArray = Array(width) { DoubleArray(height) { Double.POSITIVE_INFINITY } }

    init {
        val tiles = gameSimulation.tiles
        val exits = gameSimulation.map.exits

        val queue = ArrayDeque<TileVec>()
        for(exit in exits) {
            // Ignore exits that are currently solid
            if(tiles[exit.x][exit.y].isCurrentlySolid) continue

            queue.addLast(exit)
            costArray[exit.x][exit.y] = 0.0 // Set cost of exit tiles to 0
        }

        // Dijkstra's algorithm to build the flow field
        while(queue.isNotEmpty()) {
            val (x, y) = queue.removeFirst()

            val thisCost = costArray[x][y]
            // Try to decrease the cost of neighbors. If successful, add them to the queue
            if(x + 1 < width) {
                val neighbor = tiles[x + 1][y]
                val neighborCost = thisCost + neighbor.pathfindingCost
                if(neighborCost < costArray[x + 1][y]) {
                    costArray[x + 1][y] = neighborCost
                    queue.addLast(TileVec(x + 1, y))
                }
            }
            if(x - 1 >= 0) {
                val neighbor = tiles[x - 1][y]
                val neighborCost = thisCost + neighbor.pathfindingCost
                if(neighborCost < costArray[x - 1][y]) {
                    costArray[x - 1][y] = neighborCost
                    queue.addLast(TileVec(x - 1, y))
                }
            }
            if(y + 1 < height) {
                val neighbor = tiles[x][y + 1]
                val neighborCost = thisCost + neighbor.pathfindingCost
                if(neighborCost < costArray[x][y + 1]) {
                    costArray[x][y + 1] = neighborCost
                    queue.addLast(TileVec(x, y + 1))
                }
            }
            if(y - 1 >= 0) {
                val neighbor = tiles[x][y - 1]
                val neighborCost = thisCost + neighbor.pathfindingCost
                if(neighborCost < costArray[x][y - 1]) {
                    costArray[x][y - 1] = neighborCost
                    queue.addLast(TileVec(x, y - 1))
                }
            }
        }
    }

    /** Check whether a tile can reach the exit. */
    fun canReachExit(x: Int, y: Int): Boolean = costArray[x][y] < 1000000000

    /**
     * Check whether this flowfield state satisfies the game simulation's criteria.
     */
    fun satisfiesGameCriteria(): Boolean {
        // Check whether all spawnpoints are reachable (cost below 1 billion magic value)
        val spawnpoints = gameSimulation.map.spawnpoints
        for(spawnpoint in spawnpoints) {
            if(!canReachExit(spawnpoint.location.x, spawnpoint.location.y)) return false
        }

        // Check whether all exits are connected to other exits if the config option is set
        val exits = gameSimulation.map.exits
        if(gameSimulation.map.isAllExitsMustBeReachable && exits.size > 1) {
            // Try to flood fill from one of the exits and see if we can reach the other exits
            val queue = ArrayDeque<TileVec>()
            val visited = HashSet<TileVec>()
            val visitedExits = HashSet<TileVec>()
            queue.addLast(exits[0])
            visitedExits += exits[0]
            while(queue.isNotEmpty()) {
                val tileVec = queue.removeFirst()
                val (x, y) = tileVec
                val tile = gameSimulation.tiles[x][y]
                if(tile.isCurrentlySolid) continue
                visited += tileVec
                if(exits.contains(tileVec)) {
                    visitedExits += tileVec
                    if(visitedExits.size == exits.size) return true
                }
            }
            return false
        }
        else return true
    }
}