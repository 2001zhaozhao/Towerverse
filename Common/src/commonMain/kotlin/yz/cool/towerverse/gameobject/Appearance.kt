package yz.cool.towerverse.gameobject

import kotlinx.serialization.Serializable
import yz.cool.towerverse.model.GameObjectListReference
import yz.cool.towerverse.model.GameObjectReference
import yz.cool.towerverse.types.Color
import yz.cool.towerverse.types.Vec2

/**
 * Controls an entity (tower, enemy, projectile)'s appearance in the world.
 */
@Serializable
sealed class Appearance : GameObject()

/**
 * Renders multiple appearances from a list.
 * The assets will be rendered sequentially from bottom to top.
 *
 * @param childrenRef The list of appearance IDs to render.
 */
@Serializable
data class AppearanceCompound(
    private val childrenRef: GameObjectListReference<Appearance> = GameObjectListReference(),
    override var id: String = "",
    override var name: String = ""
) : Appearance() {
    /** The list of appearances to render. */
    var children by childrenRef

    companion object {
        operator fun invoke(action: AppearanceCompound.() -> Unit) = AppearanceCompound().applyAtCompile(action)
    }
}

/**
 * Renders an image asset.
 *
 * @param imageRef The image to display.
 * @param tintColor The color to tint the image. Defaults to white.
 * @param scale The scale of the image compared to the entity being rendered. Defaults to 1.
 * @param offset The offset of the image. Defaults to no offset.
 */
@Serializable
data class AppearanceImage(
    private val imageRef: GameObjectReference<ImageAsset> = GameObjectReference(),
    var tintColor: Color = Color.WHITE,
    var scale: Double = 1.0,
    var offset: Vec2 = Vec2(),
    override var id: String = "",
    override var name: String = ""
): Appearance() {
    /** The image asset to render. */
    var image by imageRef

    companion object {
        operator fun invoke(action: AppearanceImage.() -> Unit) = AppearanceImage().applyAtCompile(action)
    }
}

/**
 * Renders a solid circle.
 *
 * @param color The color of the circle. Defaults to white.
 * @param scale The scale of the circle compared to the entity being rendered. Defaults to 1.
 * @param offset The offset of the circle. Defaults to no offset.
 */
@Serializable
data class AppearanceCircle(
    var color: Color = Color.WHITE,
    var scale: Double = 1.0,
    var offset: Vec2 = Vec2(),
    override var id: String = "",
    override var name: String = ""
): Appearance() {
    companion object {
        operator fun invoke(action: AppearanceCircle.() -> Unit) = AppearanceCircle().applyAtCompile(action)
    }
}

/**
 * Renders a solid square.
 *
 * @param color The color of the square. Defaults to white.
 * @param scale The scale of the square compared to the entity being rendered. Defaults to 1.
 * @param offset The offset of the square. Defaults to no offset.
 */
@Serializable
data class AppearanceSquare(
    var color: Color = Color.WHITE,
    var scale: Double = 1.0,
    var offset: Vec2 = Vec2(),
    override var id: String = "",
    override var name: String = ""
): Appearance() {
    companion object {
        operator fun invoke(action: AppearanceSquare.() -> Unit) = AppearanceSquare().applyAtCompile(action)
    }
}