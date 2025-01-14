package yz.cool.towerverse.gameobject

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * A game object which represents an asset encoded via Base64.
 *
 * To simplify development, Towerverse encodes all assets directly within the JSON files.
 * This is doable because Towerverse has a low-poly art style with small asset sizes.
 * Another game that has much larger assets would need to store assets separately and reference them by IDs instead.
 */
@Serializable
sealed class Asset: GameObject() {
    /** Base64-encoded asset contents. */
    abstract var content: String

    /** Computed length of the asset's Base64 string. */
    val contentLength: Int get() = content.length

    /** Path to the asset. Only used in the Modding SDK.
     * The compilation process will look up the asset and encode it to Base64. */
    @Transient
    var path: String? = null

    override fun toString(): String { // Content is too long, so print content length instead
        return "${this::class.simpleName}(id=$id, name=$name, contentLength=${content.length})"
    }
}

/**
 * Represents an audio asset. (not currently used in-game)
 */
@Serializable
class AudioAsset(
    override var content: String = "",
    override var id: String = "",
    override var name: String = "",
): Asset() {
    companion object {
        operator fun invoke(action: AudioAsset.() -> Unit) = AudioAsset().applyAtCompile(action)
    }
}

/**
 * Represents an image asset. The image file must be in PNG format.
 */
@Serializable
class ImageAsset(
    override var content: String = "",
    override var id: String = "",
    override var name: String = "",
): Asset() {
    companion object {
        operator fun invoke(action: ImageAsset.() -> Unit) = ImageAsset().applyAtCompile(action)
    }
}
