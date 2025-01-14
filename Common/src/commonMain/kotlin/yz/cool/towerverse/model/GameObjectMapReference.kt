package yz.cool.towerverse.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import yz.cool.towerverse.gameobject.GameObject
import kotlin.reflect.KProperty

/**
 * A class designed to be placed inside a game object as a field to reference a map of game objects to regular values.
 * It supports a delegate that allows directly getting and setting the referenced game object map.
 * It supports anonymous game objects as well, similar to [GameObjectReference].
 *
 * This is serialized as the map of IDs of the referenced game object to values.
 *
 * @param referenceMap The map of game object references to values.
 */
@Serializable(with = GameObjectMapReference.Serializer::class)
class GameObjectMapReference<T : GameObject, V>(
    var referenceMap: Map<GameObjectReference<T>, V> = mapOf()
) {
    /** Serializer for [GameObjectMapReference]. */
    @Suppress("UNUSED_PARAMETER")
    internal class Serializer<T : GameObject, V>(
        keySerializer: KSerializer<T>,
        valueSerializer: KSerializer<V>
    ) : KSerializer<GameObjectMapReference<T, V>> {
        private val stringValueMapSerializer = MapSerializer(String.serializer().nullable, valueSerializer)
        override val descriptor: SerialDescriptor = stringValueMapSerializer.descriptor
        override fun serialize(encoder: Encoder, value: GameObjectMapReference<T, V>) =
            stringValueMapSerializer.serialize(encoder, value.referenceMap.mapKeys{
                (reference, _) -> reference.referencedId
            })
        override fun deserialize(decoder: Decoder) =
            GameObjectMapReference<T, V>(stringValueMapSerializer.deserialize(decoder).mapKeys{
                (referencedGameObject, _) -> GameObjectReference(referencedGameObject)
            })
    }

    // ===== Delegate for direct access to the game object list. This delegate must be inside another game object
    var cachedMap: Map<T, V>? = null
    operator fun getValue(gameObject: GameObject, property: Any): Map<T, V> {
        cachedMap?.let{return it} // Return cache if possible
        val newMap = referenceMap.mapKeys { (reference, _) -> reference.get(gameObject) }
        cachedMap = newMap
        return newMap
    }

    operator fun setValue(gameObject: GameObject, property: Any, value: Map<T, V>) {
        referenceMap = value.mapKeys { (referencedGameObject, _) ->
            GameObjectReference(referencedGameObject).apply{ set(gameObject, referencedGameObject) }
        }
        cachedMap = null
    }

    operator fun provideDelegate(gameObject: GameObject, property: KProperty<*>): GameObjectMapReference<T, V> {
        // Record this reference in references list so that it can be found during compilation
        val gameObjectReferenceLists = gameObject.gameObjectReferenceMaps ?: HashSet<GameObjectMapReference<*, *>>()
            .apply{gameObject.gameObjectReferenceMaps = this }
        gameObjectReferenceLists += this
        return this
    }
}