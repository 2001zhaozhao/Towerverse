package yz.cool.towerverse.sdk

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import yz.cool.towerverse.gameobject.GameObject
import yz.cool.towerverse.mod.ModInformation
import yz.cool.towerverse.mod.ModJson

/**
 * Represents a mod in the game. Note that Towerverse's official game content is also technically a mod.
 *
 * Mods can contain multiple [ModFile]-s, and can be compiled, serialized to JSON, and uploaded to Towerverse servers
 * to be shared with other players.
 *
 * @param modInformation Information about the mod, such as its name, author, and description.
 */
abstract class Mod(
    val modInformation: ModInformation
) {
    abstract val modFiles: List<ModFile>

    /**
     * Helper to merge all mod files into a single GameObject map. Should be called after compiling all mod files.
     */
    fun mergeModFiles(): HashMap<String, GameObject> {
        val gameObjects = HashMap<String, GameObject>()
        modFiles.forEach { gameObjects.putAll(it.gameObjects) }
        return gameObjects
    }

    /**
     * Compiles all mod files in this mod in increasing order of their compilation priority.
     */
    fun compile() {
        val sortedFiles = modFiles.sortedBy { it.compilePriority }
        sortedFiles.forEach { it.compile() }
    }

    /**
     * Serializes this mod into a JSON string that can be loaded by the game client and server.
     */
    fun serialize(): String {
        return Json.encodeToString(ModJson(modInformation, mergeModFiles()))
    }
}