package yz.cool.towerverse.official.enemy

import yz.cool.towerverse.gameobject.EnemyTag
import yz.cool.towerverse.official.OfficialMod
import yz.cool.towerverse.sdk.ModFile

object OfficialEnemyTag : ModFile(OfficialMod) {
    val heavy by EnemyTag {}

    val armored by EnemyTag {}
}