package yz.cool.towerverse.official.map

import yz.cool.towerverse.gameobject.*
import yz.cool.towerverse.official.OfficialMod
import yz.cool.towerverse.official.asset.OfficialAsset
import yz.cool.towerverse.sdk.ModFile
import yz.cool.towerverse.types.TileVec

object OfficialMap : ModFile(OfficialMod) {
    val officialMap by GameMap {
        width = 15
        height = 7
        exits = listOf(
            TileVec(0, 6) // Bottom left corner is the exit
        )
        defaultTileType = grass
        tileTypeWeights = mapOf(grass to 0.6, dirt to 0.2, stone to 0.2)
        spawnpoints = listOf(
            Spawnpoint {
                location = TileVec(14, 0)
            }
        )
        waveDefinition = OfficialWaves.waveDefinition
        startingMoney = 500
        playerHealth = 100.0
    }

    val grass by MapTileType {
        isSolid = false
        isTowerPlaceable = true
        appearance = AppearanceImage { image = OfficialAsset.grassBackground }
    }

    val dirt by MapTileType {
        isSolid = false
        isTowerPlaceable = false
        appearance = AppearanceImage { image = OfficialAsset.dirtBackground }
    }

    val stone by MapTileType {
        isSolid = true
        isTowerPlaceable = false
        appearance = AppearanceImage { image = OfficialAsset.stoneBackground }
    }
}