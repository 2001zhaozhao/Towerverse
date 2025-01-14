package yz.cool.towerverse.mod

import kotlinx.serialization.Serializable
import yz.cool.towerverse.model.GameObjectUtil

/**
 * Contains basic information of a mod, which can be displayed in-game without
 * having to load the rest of the mod's information.
 *
 * @param modId The string ID of the mod. Each account can only upload one mod with a given ID.
 * Two mods with the same ID cannot be loaded at the same time.
 * @param name The name of the mod. Defaults to the prettified mod ID.
 * @param author The author of the mod. Can be empty.
 * @param description The description of the mod Can be empty.
 * @param dependencies A list of mod IDs that this mod depends on. Defaults to an empty list.
 * @param remove A list of object IDs that this mod removes. Defaults to an empty list.
 * For example, to remove the official game's "soldier" tower, add "towerverse:OfficialTower:soldier" to this list.
 */
@Serializable
data class ModInformation(
    val modId: String,
    val name: String = GameObjectUtil.prettifyString(modId),
    val author: String = "",
    val description: String = "",
    val dependencies: List<String> = listOf(),
    val remove: List<String> = listOf(),
) {
    init {
        require(GameObjectUtil.validateIdSegmentFormat(modId)) {"Mod ID contains invalid characters: $modId"}
    }
}