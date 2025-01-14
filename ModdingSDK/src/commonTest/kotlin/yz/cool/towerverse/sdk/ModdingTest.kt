package yz.cool.towerverse.sdk

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import yz.cool.towerverse.gameobject.*
import yz.cool.towerverse.mod.ModInformation
import kotlin.test.*

class ModdingTest {
    object TestMod : Mod(
        ModInformation(
            modId = "testMod"
        )
    ) {
        override val modFiles: List<ModFile> = listOf(
            TestModFile
        )
    }

    object TestModFile : ModFile(TestMod) {
        val testTowerType by TowerType {
            cost = 100
            appearance = AppearanceCompound {
                children += AppearanceImage {
                    image = ImageAsset { path = "path/to/image.png" }
                }
            }
        }

        val grass by MapTileType {
            isSolid = false
            isTowerPlaceable = true
        }

        val dirt by MapTileType {
            isSolid = false
            isTowerPlaceable = false
        }

        val rock by MapTileType {
            isSolid = true
            isTowerPlaceable = false
        }

        val testGameMap by GameMap {
            width = 10
            height = 10
            tileTypeWeights = mapOf(
                grass to 0.7,
                dirt to 0.2,
                rock to 0.1
            )
        }
    }

    @Test
    fun moddingTest() {
        TestMod.compile()

        println(TestModFile.gameObjects)

        println(Json.encodeToString(TestModFile.gameObjects))
    }
}
