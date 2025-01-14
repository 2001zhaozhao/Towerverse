package yz.cool.towerverse.official.tower

import yz.cool.towerverse.gameobject.*
import yz.cool.towerverse.official.OfficialMod
import yz.cool.towerverse.official.asset.OfficialAsset
import yz.cool.towerverse.sdk.ModFile
import yz.cool.towerverse.types.Color

object OfficialTower : ModFile(OfficialMod) {
    // ========== Rarities
    val common by TowerRarity {
        color = Color(160, 160, 160)
        rarityValue = 0.0
        maxLevel = 5
    }

    val rare by TowerRarity {
        color = Color(255, 128, 0)
        rarityValue = 1.0
        maxLevel = 4
    }

    val epic by TowerRarity {
        color = Color(200, 0, 255)
        rarityValue = 2.0
        maxLevel = 3
    }

    val legendary by TowerRarity {
        color = Color(180, 255, 255)
        rarityValue = 3.0
        maxLevel = 2
    }

    // ========== Towers
    val soldier by TowerType {
        description = "A basic tower that shoots bullets at enemies."
        cost = 100
        rarity = common
        appearance = AppearanceImage { image = OfficialAsset.soldierImage }
        turret {
            attackDelay = 1.0
            shape = TowerTargetingShapeCircle { radius = 3.0 }
            appearance = AppearanceImage { image = OfficialAsset.soldierTurretImage; scale = 1.2 }
            attack {
                projectile {
                    speed = 8.0
                    size = 0.1
                    appearance = AppearanceCircle { color = Color.YELLOW }
                    damage { damage = 20.0 }
                }
            }
        }
    }

    val cannon by TowerType {
        description = "Deals significant damage with some splash damage over a long range. Has a slow fire rate."
        cost = 150
        rarity = rare
        appearance = AppearanceImage { image = OfficialAsset.cannonImage }
        turret {
            attackDelay = 5.0
            shape = TowerTargetingShapeCircle { radius = 5.5 }
            appearance = AppearanceImage { image = OfficialAsset.cannonTurretImage }
            attack {
                projectile {
                    speed = 8.0
                    size = 0.4
                    appearance = AppearanceCircle { color = Color(50) }
                    damage { damage = 60.0 }
                    splashDamage { damage = 15.0 }
                    splashRadius = 1.5
                    disappearWhenOutOfRange = true
                    splashWhenDisappear = true
                }
            }
        }
    }
}