package yz.cool.towerverse.official.map

import yz.cool.towerverse.gameobject.Wave
import yz.cool.towerverse.gameobject.WaveDefinition
import yz.cool.towerverse.official.OfficialMod
import yz.cool.towerverse.official.enemy.OfficialEnemy
import yz.cool.towerverse.sdk.ModFile

private typealias E = OfficialEnemy
object OfficialWaves : ModFile(OfficialMod) {
    val waveDefinition by WaveDefinition {
        finalWave = 5
        waveInterval = 10.0
        baseWaveReward = 200.0
        amountScalingRate = 0.2
        rewardScalingRate = 1.0
        levelScalingRate = 1.0

        // Dynamically load all waves registered in this file
        // Because the init function call is delayed to compilation,
        // all top-level wave references will have already been loaded so we can access them
        waves = this@OfficialWaves.gameObjects.values.filterIsInstance<Wave>()
    }

    val default1 by Wave {
        waveNumber = 1
        priority = 1
        frequency = 1
        spawnDuration = 5.0
        enemies = mapOf(
            E.basic to 1.0,
            E.heavy to 0.5
        )
    }

    val special1 by Wave {
        waveNumber = 4
        priority = 10
        frequency = 4
        amountMultiplier = 4.0
        spawnDuration = 10.0
        waveDurationMultiplier = 2.0
        enemies = mapOf(
            E.basic to 1.0,
            E.heavy to 1.0
        )
    }

    val final by Wave {
        waveNumber = 5
        amountMultiplier = 0.3
        spawnDelay = 10.0
        spawnDuration = 5.0
        enemies = mapOf(
            E.boss to 1.0
        )
        childWaves = listOf(
            default1, // Spawn wave 1-3 and wave 4 again alongside the boss
            special1
        )
    }
}