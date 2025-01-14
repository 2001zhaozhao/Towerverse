package yz.cool.towerverse.mod

import kotlinx.serialization.Serializable
import yz.cool.towerverse.gameobject.GameObject

/**
 * A serializable compiled mod object. The client and server can load game objects from this mod.
 */
@Serializable
class ModJson(
    val modInformation: ModInformation,
    val gameObjects: HashMap<String, GameObject>
)