package yz.cool.towerverse.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import yz.cool.towerverse.gameobject.GameObject
import kotlin.reflect.KProperty

/**
 * A class designed to be placed inside a game object as a field to reference another game object.
 * It supports a property delegate that allows directly getting and setting the referenced game object,
 * including anonymous game objects.
 *
 * This is serialized as the ID of the referenced game object.
 *
 * ## Anonymous Game Objects
 *
 * Anonymous game objects are game objects directly set to the delegated field of this reference.
 * For example:
 *
 * ```kotlin
 * val tower by TowerType {
 *     appearance = ImageAppearance { /* ... */ } // This is an anonymous game object
 * }
 * ```
 *
 * In the Modding SDK, since the game object initializers are run at compile time,
 * the nested referenced game object will only be populated into a ModFile at compile time as well.
 * Therefore, anonymous game objects that are put into references (for example as defaults) but replaced before
 * compile time will not appear in the final mod JSON.
 */
@Serializable(with = GameObjectReference.Serializer::class)
class GameObjectReference<T : GameObject?>() {
    /** Constructs a GameObjectReference with an initial referenced game object ID. */
    constructor(referencedId: String?) : this() {
        this.referencedId = referencedId
    }
    /** Constructs a GameObjectReference with an initial referenced game object. */
    constructor(referencedObject: T & Any) : this() {
        this.referencedId = referencedObject.id
        this.referencedObject = referencedObject
    }

    /** The ID of the referenced game object.
     *
     * The entire [GameObjectReference] class is serialized as an ID.
     *
     * Setting this ID to a different value as its current value will also clear the [referencedObject]. */
    var referencedId: String? = null
        set(value) {
            if(field != value) {
                field = value
                referencedObject = null // Clear referencedObject if referencedId changes
            }
        }

    /** The game object being referenced.
     *
     * In the modding SDK, this is populated by setting the delegated reference field to a value.
     * In the game simulation, this is not populated at first but will be populated on a lazy basis. */
    internal var referencedObject: T? = null

    /** Serializer for [GameObjectReference]. */
    @Suppress("UNUSED_PARAMETER")
    internal class Serializer<T : GameObject>(dataSerializer: KSerializer<T>) : KSerializer<GameObjectReference<T>> {
        private val nullableStringSerializer = String.serializer().nullable
        override val descriptor: SerialDescriptor = nullableStringSerializer.descriptor
        override fun serialize(encoder: Encoder, value: GameObjectReference<T>) =
            nullableStringSerializer.serialize(encoder, value.referencedId)
        override fun deserialize(decoder: Decoder) =
            GameObjectReference<T>(nullableStringSerializer.deserialize(decoder))
    }

    /**
     * Gets the referenced game object. Takes the parent game object as parameter.
     *
     * Will look up the referenced object from the parent game object's game environment if it is not already populated.
     *
     * Note that if T is nullable and referencedId is null, an error will be thrown.
     */
    @Suppress("UNCHECKED_CAST")
    fun get(gameObject: GameObject): T {
        if(referencedId == null) return (null as T)
        if(referencedObject?.id == referencedId) return referencedObject as T

        val lookupResult = gameObject.environment.gameObjects[referencedId] as T
        referencedObject = lookupResult // Cache this result
        return lookupResult
    }

    /**
     * Sets the referenced game object. Takes the parent game object as parameter.
     *
     * If the provided object does not have an ID, this method will set an appropriate ID for it that assumes that
     * the provided object is a direct child object of this game object.
     * However, the method will not add it to the parent object's game environment
     * (that is done during compilation logic).
     */
    fun set(gameObject: GameObject, value: T) {
        // If reference game object is anonymous, set an integer ID
        if(value != null && value.id.isEmpty()) {
            value.id = "${gameObject.id}:${gameObject.childReferenceIdCounter++}"
        }

        referencedId = value?.id
        referencedObject = value
    }

    // ===== Delegate for direct access to the referenced game object. This delegate must be inside another game object
    operator fun getValue(gameObject: GameObject, property: KProperty<*>): T {
        return get(gameObject)
    }

    operator fun setValue(gameObject: GameObject, property: KProperty<*>, value: T) {
        return set(gameObject, value)
    }

    operator fun provideDelegate(gameObject: GameObject, property: KProperty<*>): GameObjectReference<T> {
        // Record this reference in references list so that it can be found during compilation
        val gameObjectReferences = gameObject.gameObjectReferences ?: HashSet<GameObjectReference<*>>()
            .apply{gameObject.gameObjectReferences = this }
        gameObjectReferences += this@GameObjectReference
        return this@GameObjectReference
    }

    override fun toString(): String = referencedId ?: "[None]"
}