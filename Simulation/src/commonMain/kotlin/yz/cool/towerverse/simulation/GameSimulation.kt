package yz.cool.towerverse.simulation

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.kotlincrypto.hash.sha2.SHA256
import yz.cool.towerverse.gameobject.GameMap
import yz.cool.towerverse.gameobject.GameObject
import yz.cool.towerverse.gameobject.MapTileType
import yz.cool.towerverse.mod.ModJson
import yz.cool.towerverse.simulation.action.Action
import yz.cool.towerverse.simulation.game.Enemy
import yz.cool.towerverse.simulation.game.MapTile
import yz.cool.towerverse.simulation.game.Tower
import yz.cool.towerverse.simulation.pathfinding.FlowField
import yz.cool.towerverse.simulation.ui.UI
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

/**
 * The main game simulation class.
 *
 * @param map The game map to simulate.
 * @param env The game environment to simulate.
 * @param randomSeed The seed to use for the random number generator.
 * @param isVerificationMode Whether this is the server verification mode of the simulation.
 */
class GameSimulation(
    val map: GameMap,
    val env: GameSimulationEnvironment,
    val randomSeed: Long,
    val isVerificationMode: Boolean,
    val onVictory: (() -> Unit)? = null
) {
    companion object {
        /** Creates a game simulation based on the official content as well as currently loaded mods */
        fun createSimulation(
            officialGameObjects: Map<String, GameObject>,
            mods: List<ModJson>,
            isVerificationMode: Boolean,
            onVictory: (() -> Unit)? = null
        ): GameSimulation {
            // Load the game's official content as well as mod content, and use it to create a simulation
            val gameObjects = officialGameObjects.toMutableMap()
            var gameMap = gameObjects["towerverse:OfficialMap:officialMap"] as GameMap

            for(mod in mods) {
                // Remove game objects
                for(idToRemove in mod.modInformation.remove) {
                    gameObjects.remove(idToRemove)
                }
                // Add mod game objects
                gameObjects.putAll(mod.gameObjects)
                // If the mod contains a new map, use it instead of the official map
                val modMap = mod.gameObjects.values.firstOrNull { it is GameMap } as GameMap?
                if(modMap != null) {
                    gameMap = modMap
                }
            }

            return GameSimulation(
                map = gameMap,
                env = GameSimulationEnvironment(gameObjects).apply{ repopulateEnvironment() },
                randomSeed = 2345,
                isVerificationMode = isVerificationMode,
                onVictory = onVictory
            )
        }
    }

    /**
     * The UI linked to the game simulation. This will be present only for the client.
     */
    var ui: UI? = null

    /**
     * The random generator of the game simulation.
     *
     * Note that in Kotlin Multiplatform, random generators always retain the same behavior across platforms.
     * Towerverse uses this to allow the game simulation to be reproducible, and therefore
     * allowing the client's offline gameplay to be verified by the server.
     */
    val random = Random(randomSeed)

    /**
     * Helper function to use the game simulation's random generator to randomly round a value to an integer
     * such that the expected return value equals the original value.
     */
    fun randomlyRound(value: Double): Int {
        return (value.toInt() + if(random.nextDouble() < (value % 1)) 1 else 0)
    }

    /** Called on server's failure to verify the game simulation. */
    inline fun fail(message: () -> String): Nothing = throw VerificationFailedException(message())

    // ========== Map Tiles

    /** Tiles on the map. This is initialized at the beginning but can change over the course of the game. */
    val tiles: List<List<MapTile>> = List(map.width) {
        List(map.height) {
            MapTile(map.defaultTileType)
        }
    }

    // Randomly generate map on init
    init {
        val totalWeight = map.tileTypeWeights.values.sum()

        fun chooseRandomTileType(): MapTileType {
            // Chose a random tile type from the map tileTypeWeights
            var value = random.nextDouble() * totalWeight
            map.tileTypeWeights.forEach { (tileType, weight) ->
                value -= weight
                if (value <= 0) {
                    return tileType
                }
            }
            error("Failed to choose a random tile type")
        }

        if(totalWeight > 0) {
            val tileVecs = map.getAllTileVectors().shuffled(random)
            for(tileVec in tileVecs) {
                val (x, y) = tileVec
                val tileType = chooseRandomTileType()
                setTileTypeIfDoesNotBlockEnemyPath(x, y, tileType)
            }
        }
    }

    // ========= Simulation State

    /**
     * A serializable object that mirrors the mutable state portion of the game simulation.
     * This does not include UI state (e.g. whether a tower is selected)
     *
     * This object can be serialized and hashed to compare the state of the simulation between an expected and actual
     * value, thereby allowing the server to verify the client's gameplay.
     */
    val state = GameSimulationState(
        tiles = tiles
    )

    /** The current tick of the game simulation. */
    var tick: Long by state::tick

    /** The amount of money that the player has. */
    var money: Long by state::money

    /** The player's current score. */
    var score: Long by state::score

    /** The health of the player's base. */
    var playerHealth: Double by state::playerHealth
    init {
        money = map.startingMoney
        playerHealth = map.playerHealth
    }

    /** The actions that are performed during the course of the game simulation.
     * The key is the tick at which the action is performed.
     *
     * Whenever the simulation starts a new tick with this tick number, the effects of the action will be applied.
     *
     * When this simulation is being run on the client, actions are **queued dynamically** from client inputs.
     * All past actions are saved in this object and serialized when the client sends the game results to the server.
     * When the game server verifies the client gameplay, it will therefore be able to execute the exact same actions
     * at the same time as the client did when playing through the game. */
    val actions by state::actions

    /** The list of enemies in the game simulation. */
    val enemies by state::enemies

    /**
     * Gets a list of enemies that are within a given radius of the given coordinates, taking enemy size into account.
     */
    fun getEnemiesIntersectingRadius(x: Double, y: Double, radius: Double): List<Enemy> {
        return enemies.filter { enemy ->
            val radiusPlusSize = radius + enemy.type.size
            val dx = x - enemy.x
            val dy = y - enemy.y
            dx * dx + dy * dy <= radiusPlusSize * radiusPlusSize
        }
    }

    /** The list of enemies in the game simulation. */
    val projectiles by state::projectiles

    /** The enemy spawner instance of the game simulation, containing its own state. */
    val enemySpawner by state::enemySpawner

    /**
     * Called by the game client to queue an action to be executed in the next game tick.
     * If the action is not valid, it will not be queued.
     * @return an error message if the action was not valid, null otherwise
     */
    fun queueActionIfValid(action: Action): String? {
        val errorMessage = with(action) { checkValid() }
        if(errorMessage == null) {
            actions.getOrPut(tick + 1) { ArrayList() }.add(action)
            return null
        }
        else return errorMessage
    }

    // ========== Action callbacks

    fun onTowerPlaced(x: Int, y: Int, tower: Tower) {
        updatePathfinding()
    }

    fun onTowerUpgraded(x: Int, y: Int, tower: Tower) {

    }

    fun onTowerRemoved(x: Int, y: Int, tower: Tower) {
        updatePathfinding()
    }

    // ========== Pathfinding

    var flowField = FlowField(this)

    /**
     * Try to create a new flow field or return null if the flow field does not satisfy the game's criteria
     */
    fun tryCreateNewFlowField(): FlowField? {
        // Build a temporary flow field
        val flowField = FlowField(this)
        return if(flowField.satisfiesGameCriteria()) flowField else null
    }

    private fun setTileTypeIfDoesNotBlockEnemyPath(x: Int, y: Int, tileType: MapTileType): Boolean {
        val currentTileType = tiles[x][y].type
        tiles[x][y].type = tileType

        val newFlowField = tryCreateNewFlowField()
        if(newFlowField == null) {
            tiles[x][y].type = currentTileType // revert type
            return false
        }
        else flowField = newFlowField // Update flowfield
        return true
    }

    /**
     * Gets whether a tile can be blocked by a new solid tower placement and still allow enemies to pass through.
     */
    fun canTileBeBlockedBySolidTower(x: Int, y: Int): Boolean {
        val tile = tiles[x][y]
        if(tile.type.isSolid) return true // Enemies already cannot walk on solid tiles
        // Try to block off the tile
        tile.isTemporarilyBlocked = true
        val newFlowField = tryCreateNewFlowField()
        tile.isTemporarilyBlocked = false
        return newFlowField != null
    }

    /**
     * Updates the current pathfinding flow field.
     */
    fun updatePathfinding() {
        flowField = tryCreateNewFlowField() ?: error("The new flow field does not satisfy the game criteria")
    }

    // ========== Simulation Logic

    val isFinalWave get() = map.waveDefinition.finalWave.let{it > 0 && enemySpawner.waveNumber == it}

    var hasAlreadyLost: Boolean = false
    var hasAlreadyWon: Boolean = false

    /**
     * Game ticks are performed at fixed timesteps of 50 ms.
     */
    fun gameTick() {
        tick++

        // Perform actions for this tick
        val actionsThisTick = actions[tick]
        if(actionsThisTick != null) {
            if(isVerificationMode) {
                // Perform actions while checking if they are valid. If an action is invalid, fail the simulation
                for(action in actionsThisTick) {
                    with(action) {
                        val errorMessage = checkValid()
                        if(errorMessage != null) fail { "Action is invalid: $action Due to: $errorMessage" }
                        perform()
                    }
                }
            }
            else {
                // Perform actions and remove those from the list that are invalid
                // (this ensures that we do not go on to send these invalid actions to the server)
                actionsThisTick.removeAll { action ->
                    with(action) {
                        val errorMessage = checkValid()
                        if(errorMessage != null) true
                        else {
                            perform()
                            false
                        }
                    }
                }
            }
        }

        // Tick towers
        for(x in 0 ..< map.width) {
            for(y in 0 ..< map.height) {
                val tower = tiles[x][y].tower
                if(tower != null) with(tower) { tick(x, y) }
            }
        }

        // Tick projectiles
        projectiles.removeAll { projectile ->
            with(projectile) { tick() }
        }

        // Tick enemies and remove those that should despawn
        enemies.removeAll { enemy ->
            with(enemy) { tick() }
        }

        // Spawn enemies
        with(enemySpawner) { tick() }

        // Check if player has lost (only in verification mode, in game client you can keep playing :D)
        if(playerHealth <= 0 && !hasAlreadyWon && !hasAlreadyLost) {
            hasAlreadyLost = true
            if(isVerificationMode) fail { "Player has lost" }
        }

        // Check if player has won
        if(
            isFinalWave && enemySpawner.queuedEnemies.isEmpty() && enemies.isEmpty() &&
            !hasAlreadyWon && !hasAlreadyLost
        ) {
            hasAlreadyWon = true
            onVictory?.invoke()
        }
    }

    // ========== Game State Verification

    /**
     * Gets a hash of the current game state.
     * This allows comparing the game state of the client and the server at specific ticks.
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun getGameStateHash(): String {
        val serializedState = Json.encodeToString(state).also{ println(it) }
        return Base64.encode(SHA256().digest(serializedState.encodeToByteArray()))
    }
}