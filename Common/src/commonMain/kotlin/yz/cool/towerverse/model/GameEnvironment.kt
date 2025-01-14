package yz.cool.towerverse.model

import yz.cool.towerverse.gameobject.GameObject

/**
 * A game environment which provides a table of game objects by name.
 *
 * Implementations include ModFile, the modding SDK object that stores game objects being compiled;
 * and GameSimulationEnvironment, the environment used to store game objects loaded by all mods in a simulation.
 */
abstract class GameEnvironment {
    /** The game objects in this environment. */
    abstract val gameObjects: MutableMap<String, GameObject>

    /**
     * Sets this environment as the environment of the provided game object.
     */
    fun setAsEnvironmentFor(gameObject: GameObject) {
        gameObject.environment = this
    }

    /**
     * Sets all child game object's environment to this environment.
     *
     * Should be used after deserializing a game environment from a mod JSON file.
     */
    fun repopulateEnvironment() {
        gameObjects.values.forEach { it.environment = this }
    }
}