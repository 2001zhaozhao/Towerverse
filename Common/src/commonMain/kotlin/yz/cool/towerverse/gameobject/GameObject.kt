package yz.cool.towerverse.gameobject

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import yz.cool.towerverse.mod.ModDslMarker
import yz.cool.towerverse.model.GameEnvironment
import yz.cool.towerverse.model.GameObjectListReference
import yz.cool.towerverse.model.GameObjectMapReference
import yz.cool.towerverse.model.GameObjectReference

/**
 * The base class for game objects. Game Objects can be compiled through the modding DSL.
 */
@Serializable
@ModDslMarker
sealed class GameObject {
    companion object {
        /**
         * Called when the game object is compiled by the modding SDK.
         * Will recursively compile any anonymous game objects and add them to the environment.
         *
         * This is a static function to avoid polluting [GameObject] scope in the modding DSL.
         */
        fun compile(gameObject: GameObject): Unit = with(gameObject) {
            // Execute the delayed compilation task
            delayedCompileTask?.invoke(this)

            // Recursively compile any referenced anonymous game objects
            val nestedReferences = (gameObjectReferences ?: emptyList()) +
                    (gameObjectReferenceLists?.flatMap{it.referenceList} ?: emptyList()) +
                    (gameObjectReferenceMaps?.flatMap{it.referenceMap.keys} ?: emptyList())
            for(reference in nestedReferences) {
                val referencedObject = reference.referencedObject
                if(referencedObject != null) {
                    check(referencedObject.id.isNotEmpty()) {"Referenced game object should have an ID"} // Sanity check

                    // Note: Here we depend on how the modding SDK names objects.
                    // More than 2 colons means that the object is not a top-level object in another mod file.
                    if(referencedObject.id.count {it == ':'} > 2 && referencedObject.id !in environment.gameObjects) {
                        // The referenced game object is anonymous, add it to the environment and compile it
                        referencedObject.environment = environment
                        environment.gameObjects[referencedObject.id] = referencedObject
                        compile(referencedObject)
                    }
                }
            }
        }
    }

    /** Unique ID of the game object.
     * This ID must start with the mod name followed by a colon. Otherwise, the game object will not be loaded.
     * Only letters, numbers, underscore, hyphen and colon are allowed.
     *
     * This field is automatically filled when loading the game object declaration
     * and therefore should not be manually modified. */
    abstract var id: String

    /** Displayed name of the game object. It does not have to be unique.
     * If not set (or set to an empty string),
     * defaults to a prettified version of the game object's declared property name. */
    abstract var name: String

    /** The game environment that this GameObject resides in.
     * In the modding SDK this field is filled in by the delegate handler when loading a ModFile containing
     * the game object. In the game simulation, this field is filled in by the repopulate process. */
    @Transient
    internal lateinit var environment: GameEnvironment

    /** The counter used to generate unique IDs for child game objects. */
    @Transient
    var childReferenceIdCounter: Int = 0

    /** Set of all references to other game objects in this object. Only available in the modding SDK. */
    @Transient
    internal var gameObjectReferences: HashSet<GameObjectReference<*>>? = null

    /** Set of all references to lists of other game objects in this object. Only available in the modding SDK. */
    @Transient
    internal var gameObjectReferenceLists: HashSet<GameObjectListReference<*>>? = null

    /** Set of all references to maps of other game objects to regular values in this object.
     * Only available in the modding SDK. */
    @Transient
    internal var gameObjectReferenceMaps: HashSet<GameObjectMapReference<*, *>>? = null

    /** The delayed task to run for this game object to populate its fields during modding SDK compilation. */
    @Transient
    internal var delayedCompileTask: ((GameObject) -> Unit)? = null
}

/**
 * Applies the given lambda to the game object at the time of the mod being compiled.
 * Any anonymous game object references that may be added during this task will be compiled as well.
 */
@Suppress("UNCHECKED_CAST")
internal fun <T : GameObject> T.applyAtCompile(action: (T) -> Unit): T {
    delayedCompileTask = action as ((GameObject) -> Unit)
    return this
}