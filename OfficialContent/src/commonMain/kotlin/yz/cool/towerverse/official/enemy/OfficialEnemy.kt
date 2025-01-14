package yz.cool.towerverse.official.enemy

import yz.cool.towerverse.gameobject.AppearanceImage
import yz.cool.towerverse.gameobject.EnemyType
import yz.cool.towerverse.official.OfficialMod
import yz.cool.towerverse.official.asset.OfficialAsset
import yz.cool.towerverse.sdk.ModFile

object OfficialEnemy : ModFile(OfficialMod) {
    val basic by EnemyType {
        size = 0.45
        health = 100.0
        speed = 1.0
        weight = 1.0
        height = 1.0
        appearance = AppearanceImage { image = OfficialAsset.basicEnemy; scale = 1.3 }
    }

    val heavy by EnemyType {
        size = 0.6
        health = 300.0
        speed = 0.5
        weight = 3.0
        height = 1.5
        tags += OfficialEnemyTag.heavy
        appearance = AppearanceImage { image = OfficialAsset.heavyEnemy; scale = 1.3 }
    }

    val boss by EnemyType {
        size = 1.2
        health = 5000.0
        speed = 0.2
        weight = 10.0
        height = 2.0
        tags += OfficialEnemyTag.heavy
        spawnOnDeath = listOf(heavy, heavy, basic, basic)
        appearance = AppearanceImage { image = OfficialAsset.bossEnemy; scale = 1.0 }
    }
}