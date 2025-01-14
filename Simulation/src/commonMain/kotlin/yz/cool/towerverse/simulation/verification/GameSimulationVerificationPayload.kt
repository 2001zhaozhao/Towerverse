package yz.cool.towerverse.simulation.verification

import kotlinx.serialization.Serializable
import yz.cool.towerverse.mod.ModJson
import yz.cool.towerverse.simulation.GameSimulation
import yz.cool.towerverse.simulation.action.Action


/**
 * The payload sent from the client to the server to verify the game state.
 *
 * @param modJsons A list of mod JSONs that the server should load to simulate the game.
 * @param gameMapId The ID of the game map that the client has played through.
 * @param gameEndStateHash Base64-encoded SHA256 hash of the serialized game end state.
 * @param actions A complete map of the actions the client has taken at each tick.
 */
@Serializable
class GameSimulationVerificationPayload(
    val mods: List<ModJson>,
    val gameMapId: String,
    val gameEndStateHash: String,
    val actions: LinkedHashMap<Long, ArrayList<Action>>,
    val playerName: String
) {
    companion object {
        /**
         * Generates a [GameSimulationVerificationPayload] from the current state of a [GameSimulation].
         *
         * Should be called just after the victory event is triggered for the simulation on the client.
         */
        fun fromGameSimulation(
            mods: List<ModJson>,
            gameSimulation: GameSimulation,
            playerName: String
        ): GameSimulationVerificationPayload {
            // Redo "actions" map in sorted order for consistent ordering when hashed
            val actions = LinkedHashMap<Long, ArrayList<Action>>()
            for((tick, actionList) in gameSimulation.actions.entries.sortedBy{it.key}) {
                actions[tick] = actionList
            }

            return GameSimulationVerificationPayload(
                mods,
                gameSimulation.map.id,
                gameSimulation.getGameStateHash(),
                gameSimulation.actions,
                playerName
            )
        }
    }
}