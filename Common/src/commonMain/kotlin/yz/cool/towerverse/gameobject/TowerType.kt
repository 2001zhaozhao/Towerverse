package yz.cool.towerverse.gameobject

import kotlinx.serialization.Serializable
import yz.cool.towerverse.model.GameObjectListReference
import yz.cool.towerverse.model.GameObjectMapReference
import yz.cool.towerverse.model.GameObjectReference
import yz.cool.towerverse.types.Color

/**
 * Represents a type of tower which can be placed by the player to attack enemies.
 *
 * @param description The tower type's description. Optional.
 * @param cost The base cost of the tower.
 * @param isSolid Whether the tower blocks pathfinding. If false, then enemies can walk over it. Defaults to true.
 * @param baseDamageModifier The un-upgraded base damage modifier of all the tower's turrets.
 * @param baseRangeModifier The un-upgraded range modifier of all the tower's turrets.
 * Each turret may decide to respond to this value in a different way.
 * @param baseAttackRateModifier The un-upgraded attack rate modifier of all the tower's turrets.
 * @param upgradeCostModifier Flat modifier to the cost of upgrades to this tower.
 * @param upgradeCostGrowthModifier Modifier to the exponent used to calculate the cost of upgrades above level 2.
 * @param upgradeDamageEffect The multiplier to how much the damage of the tower is affected by upgrades.
 * @param upgradeRangeModifierEffect The multiplier to how much the range modifier of the tower is affected by upgrades.
 * @param upgradeAttackRateModifierEffect The multiplier to how much the attack rate modifier of the tower
 * is affected by upgrades.
 * @param refundPercentage The percentage of the tower's total cost refunded when deleting a tower of this type.
 * Defaults to 0.5.
 */
@Serializable
data class TowerType(
    var description: String? = null,
    var cost: Int = 0,
    var isSolid: Boolean = true,
    var baseDamageModifier: Double = 1.0,
    var baseRangeModifier: Double = 1.0,
    var baseAttackRateModifier: Double = 1.0,
    var upgradeCostModifier: Double = 1.0,
    var upgradeCostGrowthModifier: Double = 1.0,
    var upgradeDamageEffect: Double = 1.0,
    var upgradeRangeModifierEffect: Double = 1.0,
    var upgradeAttackRateModifierEffect: Double = 1.0,
    var refundPercentage: Double = 0.5,
    private val turretsRef: GameObjectListReference<TowerTurret> = GameObjectListReference(),
    private val appearanceRef: GameObjectReference<Appearance> = GameObjectReference(AppearanceCircle()),
    private val rarityRef: GameObjectReference<TowerRarity> = GameObjectReference(TowerRarity()),
    override var id: String = "",
    override var name: String = ""
) : GameObject() {
    /** The tower type's turrets. */
    var turrets by turretsRef
    /** The tower type's appearance. Defaults to a white circle. */
    var appearance by appearanceRef
    /** The tower type's rarity. */
    var rarity by rarityRef

    fun turret(action: TowerTurret.() -> Unit) { turrets += TowerTurret(action) }

    companion object {
        operator fun invoke(action: TowerType.() -> Unit) = TowerType().applyAtCompile(action)
    }
}

/**
 * Represents a tower's rarity. This determines the tower's scaling when upgraded.
 *
 * @param color The rarity's color
 * @param rarityValue The rarity's value, i.e. how "rare" the rarity is.
 * @param maxLevel The maximum level of towers of this rarity. Defaults to 1.
 */
@Serializable
data class TowerRarity(
    var color: Color = Color.WHITE,
    var rarityValue: Double = 0.0,
    var maxLevel: Int = 1,
    override var id: String = "",
    override var name: String = ""
) : GameObject() {
    companion object {
        operator fun invoke(action: TowerRarity.() -> Unit) = TowerRarity().applyAtCompile(action)
    }
}

/**
 * Represents a turret (targeting behavior) of a tower.
 *
 * @param attackDelay The delay between attacks.
 * @param simultaneousTargets The number of enemies that can be targeted at the same time.
 * Set to 0 for infinite targets (the tower will attack all enemies in the given range
 * but will not rotate to face any given enemy).
 * @param avoidSameTargetAsOtherTurrets Whether this turret will not pick a target that other turrets of the same tower
 * are currently targeting.
 * @param onlyTargetTargetsOfOtherTurrets Whether this turret will only target enemies currently targeted by other
 * turrets of the same tower.
 * @param offsetX The offset of the center of the turret on the x-axis compared to the center of the tower.
 * This affects the position where the turret's attack originates, as well as the turret's appearance.
 * @param offsetY The offset of the center of the turret on the y-axis compared to the center of the tower.
 * This affects the position where the turret's attack originates, as well as the turret's appearance.
 */
@Serializable
data class TowerTurret(
    var attackDelay: Double = 0.0,
    var simultaneousTargets: Int = 1,
    var avoidSameTargetAsOtherTurrets: Boolean = false,
    var onlyTargetTargetsOfOtherTurrets: Boolean = false,
    var offsetX: Double = 0.0,
    var offsetY: Double = 0.0,
    private val shapeRef: GameObjectReference<TowerTargetingShape> = GameObjectReference(),
    private val attackRef: GameObjectReference<TowerAttack> = GameObjectReference(),
    private val appearanceRef: GameObjectReference<Appearance?> = GameObjectReference(),
    override var id: String = "",
    override var name: String = ""
) : GameObject() {
    /** The shape that the tower uses to target enemies. */
    var shape by shapeRef
    /** The attack behavior of the turret. */
    var attack by attackRef
    /** The appearance of the tower's turret. This will be rendered on top of the tower.
     * The turret will rotate to face the first targeted enemy. By default, the turret will not be visible. */
    var appearance by appearanceRef

    fun attack(action: TowerAttack.() -> Unit) { attack = TowerAttack(action) }

    companion object {
        operator fun invoke(action: TowerTurret.() -> Unit) = TowerTurret().applyAtCompile(action)
    }
}

/**
 * A shape that towers can use to target enemies.
 */
@Serializable
sealed class TowerTargetingShape : GameObject()

/**
 * A compound shape for tower targeting that is made up of multiple shapes.
 * Enemies will be targeted if they are inside any of the shapes.
 */
@Serializable
data class TowerTargetingShapeCompound(
    private val shapesRef: GameObjectListReference<TowerTargetingShape> = GameObjectListReference(),
    override var id: String = "",
    override var name: String = ""
) : TowerTargetingShape() {
    /** The shapes that make up the compound shape. */
    var shapes by shapesRef

    companion object {
        operator fun invoke(action: TowerTargetingShapeCompound.() -> Unit) = TowerTargetingShapeCompound().applyAtCompile(action)
    }
}

/**
 * Represents a tower targeting shape.
 *
 * @param radius The radius of the circle.
 * @param radiusRangeModifierEffect The multiplier to how much radius is affected by the range modifier.
 * Defaults to 1. Set to 0 to remove any effect.
 * @param innerRadiusRangeModifierEffect The multiplier to how much inner radius is affected by the range modifier.
 * Defaults to 1. Set to 0 to remove any effect.
 */
@Serializable
data class TowerTargetingShapeCircle(
    var radius: Double = 0.0,
    var radiusRangeModifierEffect: Double = 1.0,
    var innerRadiusRangeModifierEffect: Double = 1.0,
    override var id: String = "",
    override var name: String = ""
) : TowerTargetingShape() {

    companion object {
        operator fun invoke(action: TowerTargetingShapeCircle.() -> Unit) = TowerTargetingShapeCircle().applyAtCompile(action)
    }
}

/**
 * Represents a tower targeting shape.
 *
 * @param width The width of the rectangle.
 * @param height The height of the rectangle.
 * @param offsetX The offset of the center of the rectangle on the x-axis compared to the center of the tower.
 * @param offsetY The offset of the center of the rectangle on the y-axis compared to the center of the tower.
 * @param widthRangeModifierEffect The multiplier to how much width is affected by the range modifier.
 * Defaults to 1. Set to 0 to remove any effect.
 * @param heightRangeModifierEffect The multiplier to how much height is affected by the range modifier.
 * Defaults to 1. Set to 0 to remove any effect.
 * @param offsetXRangeModifierEffect The multiplier to how much offsetX is affected by the range modifier.
 * Defaults to 1. Set to 0 to remove any effect.
 * @param offsetYRangeModifierEffect The multiplier to how much offsetY is affected by the range modifier.
 * Defaults to 1. Set to 0 to remove any effect.
 */
@Serializable
data class TowerTargetingShapeRectangle(
    var width: Double = 0.0,
    var height: Double = 0.0,
    var offsetX: Double = 0.0,
    var offsetY: Double = 0.0,
    var widthRangeModifierEffect: Double = 1.0,
    var heightRangeModifierEffect: Double = 1.0,
    var offsetXRangeModifierEffect: Double = 1.0,
    var offsetYRangeModifierEffect: Double = 1.0,
    override var id: String = "",
    override var name: String = ""
) : TowerTargetingShape() {

    companion object {
        operator fun invoke(action: TowerTargetingShapeRectangle.() -> Unit) = TowerTargetingShapeRectangle().applyAtCompile(action)
    }
}

/**
 * Represents a tower attack behavior.
 *
 * @param damageParticleColor The color of the damage particle. Defaults to yellow. Set to null to disable.
 * @param damageParticleLineWidth The width of the damage particle. Defaults to 0.02.
 * @param splashParticleColor The color of the splash particle. Defaults to red. Set to null to disable.
 * @param splashRadius The radius of the splash damage.
 * Any enemy with hitbox intersecting the splash circle will be hit.
 * Setting this to 0 would only hit enemies with the explosion center directly inside their hitbox. Defaults to 0.
 * @param projectileSpread Projectile spread in degrees. Defaults to 0.
 */
@Serializable
data class TowerAttack(
    private val damageRef: GameObjectReference<TowerDamage?> = GameObjectReference(),
    private val splashDamageRef: GameObjectReference<TowerDamage?> = GameObjectReference(),
    var damageParticleColor: Color? = Color.YELLOW,
    var damageParticleLineWidth: Double = 0.02,
    var splashParticleColor: Color? = Color.RED,
    var splashRadius: Double = 0.0,
    private val projectileRef: GameObjectReference<ProjectileType?> = GameObjectReference(),
    var projectileSpread: Double = 0.0,
    override var id: String = "",
    override var name: String = ""
) : GameObject() {
    /** The instant damage behavior of the tower. By default, there is no damage. */
    var damage by damageRef
    /** The instant splash damage behavior of the tower. By default, there is no splash damage. */
    var splashDamage by splashDamageRef
    /** The projectile launched by the attack in the enemy's direction. By default, there is no projectile. */
    var projectile by projectileRef

    fun damage(action: TowerDamage.() -> Unit) { damage = TowerDamage(action) }
    fun splashDamage(action: TowerDamage.() -> Unit) { splashDamage = TowerDamage(action) }
    fun projectile(action: ProjectileType.() -> Unit) { projectile = ProjectileType(action) }

    companion object {
        operator fun invoke(action: TowerAttack.() -> Unit) = TowerAttack().applyAtCompile(action)
    }
}

/**
 * Represents a damage behavior that can be dealt by towers to enemies, either directly or indirectly via projectiles.
 * The damage dealt will be modified by the tower's damage multiplier.
 */
@Serializable
data class TowerDamage(
    var damage: Double = 0.0,
    private val damageMultiplierRef: GameObjectMapReference<EnemyTag, Double> = GameObjectMapReference(),
    override var id: String = "",
    override var name: String = ""
) : GameObject() {
    /** A map containing damage multipliers of this damage behavior to different enemy tags. */
    var damageMultiplier by damageMultiplierRef

    companion object {
        operator fun invoke(action: TowerDamage.() -> Unit) = TowerDamage().applyAtCompile(action)
    }
}