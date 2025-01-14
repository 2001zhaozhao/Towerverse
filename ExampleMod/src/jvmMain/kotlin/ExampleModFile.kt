import yz.cool.towerverse.gameobject.*
import yz.cool.towerverse.official.map.OfficialMap.dirt
import yz.cool.towerverse.official.map.OfficialMap.grass
import yz.cool.towerverse.official.map.OfficialMap.stone
import yz.cool.towerverse.official.map.OfficialWaves
import yz.cool.towerverse.official.tower.OfficialTower.epic
import yz.cool.towerverse.sdk.ModFile
import yz.cool.towerverse.types.TileVec

object ExampleModFile : ModFile(ExampleMod) {
    /**
     * The Example Mod provides a larger version of the game's official map.
     *
     * Note that a GameMap included by the mod will be detected and used by the game instead of the official map.
     */
    val exampleMap by GameMap {
        width = 20
        height = 15
        exits = listOf(
            TileVec(0, 14) // Bottom left corner is the exit
        )
        defaultTileType = grass
        tileTypeWeights = mapOf(grass to 0.6, dirt to 0.2, stone to 0.2)
        spawnpoints = listOf(
            Spawnpoint {
                location = TileVec(19, 0)
            }
        )
        waveDefinition = OfficialWaves.waveDefinition
        startingMoney = 500
        playerHealth = 100.0
    }

    // Note: you can freely mix game objects of different types in the same file!
    // This gives you freedom to organize your mod's files however you want.

    val exampleTower by TowerType {
        description = "An example tower"
        cost = 500
        rarity = epic
        appearance = AppearanceImage {
            // This asset will be loaded from jvm/main/resources when compiling the mod
            image = ImageAsset { path = "exampleTower.png" }
        }
        turret {
            attackDelay = 0.2
            shape = TowerTargetingShapeCircle { radius = 3.0 }
            appearance = AppearanceImage {
                image = ImageAsset { path = "exampleTowerTurret.png" }
            }
            attack {
                projectileSpread = 60.0
                projectile {
                    speed = 0.5
                    size = 0.4
                    appearance = AppearanceImage {
                        image = ImageAsset { path = "exampleTowerProjectile.png" }
                    }
                    damage { damage = 20.0 }
                    splashDamage { damage = 20.0 }
                    splashRadius = 1.2
                    zInitial = 0.01
                    zSpeed = 4.0
                    zAcceleration = -2.0
                }
            }
        }
    }
}