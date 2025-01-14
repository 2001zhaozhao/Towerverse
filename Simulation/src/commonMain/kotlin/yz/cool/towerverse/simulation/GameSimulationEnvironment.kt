package yz.cool.towerverse.simulation

import yz.cool.towerverse.gameobject.GameObject
import yz.cool.towerverse.model.GameEnvironment
import kotlin.reflect.KClass

/**
 * Implementation of [GameEnvironment] during game simulation.
 *
 * It stores all [GameObject]-s loaded by all mods.
 * The complete list of game objects must be provided by the constructor.
 */
class GameSimulationEnvironment(override val gameObjects: MutableMap<String, GameObject>) : GameEnvironment() {
    /**
     * A map of game objects by type and then ID. Each inner map is a LinkedHashMap with entries sorted by ID.
     */
    val gameObjectsByType: Map<KClass<*>, Map<String, GameObject>> = buildMap {
        gameObjects.entries.sortedBy{ it.key }.forEach { (id, gameObject) ->
            val type = gameObject::class
            val typeMap = getOrPut(type) { LinkedHashMap() } as MutableMap
            typeMap[id] = gameObject
        }
    }

    /**
     * Gets a collection of all game objects of the specified type.
     *
     * The game objects are sorted by ID.
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> getGameObjectsByType(): Collection<T> =
        (gameObjectsByType[T::class]?.values ?: emptyList()) as Collection<T>

    /**
     * Gets a game object of the specified type with the specified ID.
     */
    inline fun <reified T> getGameObjectByType(id: String): T? =
        gameObjectsByType[T::class]?.get(id) as T?
}