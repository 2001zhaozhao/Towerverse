package yz.cool.towerverse.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import yz.cool.towerverse.gameobject.GameObject
import kotlin.reflect.KProperty

/**
 * A class designed to be placed inside a game object as a field to reference a list of game objects.
 * It supports a delegate that allows directly getting and setting the referenced game object list.
 * It supports anonymous game objects as well, similar to [GameObjectReference].
 *
 * This is serialized as the list of IDs of the referenced game object.
 *
 * @param referenceList The list of game object references.
 */
@Serializable(with = GameObjectListReference.Serializer::class)
class GameObjectListReference<T : GameObject>(
    var referenceList: List<GameObjectReference<T>> = listOf()
) {
    /** Serializer for [GameObjectListReference]. */
    @Suppress("UNUSED_PARAMETER")
    internal class Serializer<T : GameObject>(
        dataSerializer: KSerializer<T>
    ) : KSerializer<GameObjectListReference<T>> {
        private val stringListSerializer = ListSerializer(String.serializer().nullable)
        override val descriptor: SerialDescriptor = stringListSerializer.descriptor
        override fun serialize(encoder: Encoder, value: GameObjectListReference<T>) =
            stringListSerializer.serialize(encoder, value.referenceList.map{it.referencedId})
        override fun deserialize(decoder: Decoder) =
            GameObjectListReference<T>(stringListSerializer.deserialize(decoder).map{GameObjectReference(it)})
    }

    // ===== Delegate for direct access to the game object list. This delegate must be inside another game object
    var cachedList: List<T>? = null
    operator fun getValue(gameObject: GameObject, property: Any): List<T> {
        cachedList?.let{return it} // Return cache if possible
        val newList = referenceList.map { reference -> reference.get(gameObject) }
        cachedList = newList
        return newList
    }

    operator fun setValue(gameObject: GameObject, property: Any, value: List<T>) {
        referenceList = value.map { referencedGameObject ->
            GameObjectReference(referencedGameObject).apply{ set(gameObject, referencedGameObject) }
        }
        cachedList = null
    }

    operator fun provideDelegate(gameObject: GameObject, property: KProperty<*>): GameObjectListReference<T> {
        // Record this reference in references list so that it can be found during compilation
        val gameObjectReferenceLists = gameObject.gameObjectReferenceLists ?: HashSet<GameObjectListReference<*>>()
            .apply{gameObject.gameObjectReferenceLists = this }
        gameObjectReferenceLists += this
        return this
    }
}