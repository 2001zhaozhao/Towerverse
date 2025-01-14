package yz.cool.towerverse.official

import yz.cool.towerverse.gameobject.GameObject
import yz.cool.towerverse.mod.ModInformation
import yz.cool.towerverse.official.asset.OfficialAsset
import yz.cool.towerverse.official.enemy.OfficialEnemy
import yz.cool.towerverse.official.enemy.OfficialEnemyTag
import yz.cool.towerverse.official.map.OfficialMap
import yz.cool.towerverse.official.map.OfficialWaves
import yz.cool.towerverse.official.tower.OfficialTower
import yz.cool.towerverse.sdk.Mod
import yz.cool.towerverse.sdk.ModFile

/**
 * The official game content's "mod" object.
 */
object OfficialMod : Mod(
    ModInformation(
        modId = "towerverse",
        author = "Towerverse",
        description = "Towerverse's official game content."
    )
) {
    override val modFiles: List<ModFile> = listOf(
        OfficialMap,
        OfficialTower,
        OfficialEnemy,
        OfficialEnemyTag,
        OfficialWaves,
        OfficialAsset
    )
}

/**
 * Special code for loading the Towerverse official content. Return a map of all game objects.
 *
 * Note: this function can be called even if "OfficialContent" module is loaded in Gradle as "implementation"
 * (meaning that ModdingSDK is not transitively available) rather than "api".
 * This avoids polluting scope in client & server modules with modding-related classes.
 */
fun loadOfficialContent(): HashMap<String, GameObject> {
    OfficialMod.compile()
    return OfficialMod.mergeModFiles()
}