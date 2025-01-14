package yz.cool.towerverse.official

import yz.cool.towerverse.sdk.compileAndSave

/**
 * Sample main function for a mod.
 *
 * User-created mods may use a similar main function, calling "compileAndSave" on the mod to save it into a JSON file.
 * You can then upload this JSON to the game client to play the game with the mod enabled.
 */
fun main() {
    OfficialMod.compileAndSave("OfficialMod.json")
}