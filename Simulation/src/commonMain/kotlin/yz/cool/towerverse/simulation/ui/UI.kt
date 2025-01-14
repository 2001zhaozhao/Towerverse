package yz.cool.towerverse.simulation.ui

import yz.cool.towerverse.gameobject.TowerType
import yz.cool.towerverse.simulation.GameSimulation
import yz.cool.towerverse.simulation.action.ActionTowerPlace
import yz.cool.towerverse.simulation.action.ActionTowerRemove
import yz.cool.towerverse.simulation.action.ActionTowerUpgrade
import yz.cool.towerverse.types.Color
import yz.cool.towerverse.types.TileVec

/**
 * Represents UI state of the client on top of a running game simulation.
 * This UI state is used in rendering and input handling.
 */
class UI(
    val gameSimulation: GameSimulation
) {
    init {
        // Set the UI reference of the simulation
        gameSimulation.ui = this
    }

    /** Sorted list of tower types displayed in the bottom bar. */
    val bottomBarTowerTypes = gameSimulation.env.getGameObjectsByType<TowerType>().sortedBy { it.cost }
    /**
     * Index of the currently selected tower.
     */
    var selectedTowerIndex: Int = 0
    /** The currently selected tower. */
    val selectedTower get() = bottomBarTowerTypes[selectedTowerIndex]

    /**
     * Currently selected tile. Click the tile again to place or upgrade a tower. Right click to remove a tower.
     */
    var selectedTile: TileVec? = null

    /**
     * Current game speed.
     */
    var gameSpeed: Double = 1.0

    /**
     * Time in seconds since last tick. Used for visual interpolation as well as tick pacing.
     * If above 0.05, the client should trigger a game tick and subtract 0.05 from this value.
     * (The client should also check to prevent too many game ticks from running per frame at once.)
     */
    var timeSinceLastTick: Double = 0.0

    /**
     * List of particles currently being rendered.
     */
    val particles: ArrayList<Particle> = ArrayList()

    /** Called per frame to update [timeSinceLastTick] based on game speed. */
    fun tick(dt: Double) {
        timeSinceLastTick = if(gameSpeed <= 0.0) 0.0 else timeSinceLastTick + dt * gameSpeed

        // Tick particles
        particles.removeAll { particle ->
            particle.remainingLifetime -= dt * gameSpeed
            particle.remainingLifetime <= 0.0
        }
    }

    /**
     * Called when a tile on the map is clicked.
     */
    fun onTileClick(x: Int, y: Int, isRightClick: Boolean) {
        if(x == selectedTile?.x && y == selectedTile?.y) {
            // Place, upgrade, or remove tower
            val tile = gameSimulation.tiles[x][y]
            if(tile.tower != null) {
                if(isRightClick) {
                    val errorMessage = gameSimulation.queueActionIfValid(ActionTowerRemove(x, y))
                    if(errorMessage != null) particles += FloatingTextParticle(x + 0.5, y + 0.5, errorMessage,
                        color = Color.RED)
                    else selectedTile = null
                }
                else {
                    val errorMessage = gameSimulation.queueActionIfValid(ActionTowerUpgrade(x, y))
                    if(errorMessage != null) particles += FloatingTextParticle(x + 0.5, y + 0.5, errorMessage,
                        color = Color.RED)
                    else selectedTile = null
                }
            }
            else {
                if(!isRightClick) {
                    val errorMessage = gameSimulation.queueActionIfValid(
                        ActionTowerPlace(x, y, towerTypeId = bottomBarTowerTypes[selectedTowerIndex].id)
                    )
                    if(errorMessage != null) particles += FloatingTextParticle(x + 0.5, y + 0.5, errorMessage,
                        color = Color.RED)
                    else selectedTile = null
                }
                else {
                    selectedTile = null
                }
            }
        } else {
            // Select or deselect the tile depending on mouse button
            if(isRightClick) {
                selectedTile = null
            }
            else {
                selectedTile = TileVec(x, y)
            }
        }
    }

    fun onClickOutsideMap() {
        selectedTile = null
    }

    /**
     * Called when a tower is clicked in the bottom bar.
     */
    fun onTowerSelect(towerIndex: Int) {
        selectedTowerIndex = towerIndex
    }

    /**
     * Called when the game speed area is clicked.
     */
    fun onGameSpeedClick(isRightClick: Boolean) {
        // Cycle through game speeds
        if(!isRightClick) {
            gameSpeed *= 2.0
            if(gameSpeed > 16.0) gameSpeed = 0.25
        }
        else {
            gameSpeed /= 2.0
            if(gameSpeed < 0.25) gameSpeed = 16.0
        }
    }
}