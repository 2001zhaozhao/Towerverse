package yz.cool.towerverse.gameobject

import kotlinx.serialization.Serializable
import yz.cool.towerverse.model.GameObjectReference
import yz.cool.towerverse.types.Color

/**
 * Represents a type of projectile.
 *
 * @param speed The speed of the projectile in tiles per second. Defaults to 1.
 * @param zSpeed The speed of the projectile on the y-axis.
 * @param zAcceleration The acceleration of the projectile on the y-axis in tiles per second squared.
 * @param zInitial The initial y position of the projectile. Defaults to 0.5 (the center of the tower).
 * @param size The size of the projectile in tiles. Defaults to 0.1.
 * @param maxHits How many times the projectile can hit an enemy before disappearing. Defaults to 1.
 * Set to 0 for infinite.
 * @param multipleHitsToSameEnemy Whether the projectile can hit the same enemy multiple times (once per tick).
 * Defaults to false.
 * @param splashRadius The radius of the splash damage.
 * Any enemy with hitbox intersecting the splash circle will be hit.
 * Setting this to 0 would only hit enemies with the explosion center directly inside their hitbox. Defaults to 0.
 * @param damageParticleColor The color of the damage particle. Defaults to red. Set to null to disable.
 * @param damageParticleSize The size (diameter) of the damage particle. Defaults to 0.05.
 * @param splashParticleColor The color of the splash particle. Defaults to red. Set to null to disable.
 * @param disappearWhenOutOfRange Whether the projectile will disappear if out of the range of the tower that fired it.
 * Note that the calculation will not be exact if the tower's targeting shape is not a circle. Defaults to false.
 * @param splashWhenDisappear Whether the projectile will explode when it disappears. Defaults to false.
 * @param splashWhenHittingEnemy Whether the projectile will explode when it hits an enemy. Defaults to true.
 * @param splashWhenHittingGround Whether the projectile will explode when it hits the ground. Defaults to true.
 */
@Serializable
data class ProjectileType(
    private val appearanceRef: GameObjectReference<Appearance> = GameObjectReference(AppearanceCircle()),
    var speed: Double = 1.0,
    var zSpeed: Double = 0.0,
    var zAcceleration: Double = 0.0,
    var zInitial: Double = 0.5,
    private val damageRef: GameObjectReference<TowerDamage?> = GameObjectReference(),
    private val splashDamageRef: GameObjectReference<TowerDamage?> = GameObjectReference(),
    var size: Double = 0.1,
    var maxHits: Int = 1,
    var multipleHitsToSameEnemy: Boolean = false,
    var splashRadius: Double = 0.0,
    var damageParticleColor: Color? = Color.RED,
    var damageParticleSize: Double = 0.05,
    var splashParticleColor: Color? = Color.RED,
    var disappearWhenOutOfRange: Boolean = false,
    var splashWhenDisappear: Boolean = false,
    var splashWhenHittingEnemy: Boolean = true,
    var splashWhenHittingGround: Boolean = true,
    override var id: String = "",
    override var name: String = ""
) : GameObject() {
    /** The appearance of the projectile. This will be scaled to the projectile's size. */
    var appearance by appearanceRef

    /** The damage behavior when the projectile hits an enemy directly. */
    var damage by damageRef
    /** The damage behavior when the projectile hits an enemy with splash damage. */
    var splashDamage by splashDamageRef

    fun damage(action: TowerDamage.() -> Unit) { damage = TowerDamage(action) }
    fun splashDamage(action: TowerDamage.() -> Unit) { splashDamage = TowerDamage(action) }

    companion object {
        operator fun invoke(action: ProjectileType.() -> Unit) = ProjectileType().applyAtCompile(action)
    }
}