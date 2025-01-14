package yz.cool.towerverse.sdk

import yz.cool.towerverse.model.GameEnvironment
import yz.cool.towerverse.gameobject.GameObject
import yz.cool.towerverse.model.GameObjectUtil
import kotlin.reflect.KProperty

/**
 * A mod file which contains content for a mod.
 * Inside the mod file, one can declare game objects in the modding DSL format:
 *
 * ```kotlin
 * val exampleTower by Tower {
 *     cost = 100
 *     // ...
 * }
 * ```
 *
 * @param mod The mod this mod file belongs to
 * @param compilePriority The priority of the mod file when compiling. Lower priority is compiled first.
 * Higher priority files can access nested game objects in lower priority files, whereas lower priority files can only
 * access top-level game objects in higher priority files.
 * @param name The name of the mod file. Defaults to the class name
 */
abstract class ModFile(
    val mod: Mod,
    val compilePriority: Int = 0,
    val name: String? = null
) : GameEnvironment() {
    companion object {
        // ========== Delegate extensions for game objects to be registered inside the mod file via delegates
        // This is to add custom logic inside provideDelegate(),
        // which is a similar pattern to targets like "jvmMain" in the Kotlin Multiplatform Gradle DSL

        operator fun <T : GameObject> T.getValue(modFile: ModFile, property: KProperty<*>): T {
            return this
        }

        operator fun <T : GameObject> T.provideDelegate(modFile: ModFile, property: KProperty<*>): T {
            require(GameObjectUtil.validateIdSegmentFormat(property.name)) {
                "Game object property name contains invalid characters: ${property.name}"
            }

            // Set the game object's ID and name
            // Only set name if it is empty (retain any custom names already set)
            if(name.isEmpty()) name = GameObjectUtil.prettifyString(property.name)
            id = "${modFile.mod.modInformation.modId}:${modFile.name?:modFile::class.simpleName}:${property.name}"

            // Register this game object inside the mod file
            modFile.gameObjects[id] = this
            modFile.setAsEnvironmentFor(this)
            return this
        }
    }

    init {
        require(name == null || GameObjectUtil.validateIdSegmentFormat(name)) {
            "ModFile name contains invalid characters: $name"
        }
    }

    final override val gameObjects: HashMap<String, GameObject> = HashMap()

    /** Compiles the mod file, including all game objects inside it. */
    internal fun compile() {
        // Only directly compile top-level game objects. The nested anonymous objects will be compiled recursively.
        gameObjects.values.toList().forEach {
            GameObject.compile(it)
        }
    }
}