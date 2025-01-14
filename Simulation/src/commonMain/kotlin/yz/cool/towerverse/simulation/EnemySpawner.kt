package yz.cool.towerverse.simulation

import kotlinx.serialization.Serializable
import yz.cool.towerverse.gameobject.EnemyType
import yz.cool.towerverse.gameobject.Spawnpoint
import yz.cool.towerverse.gameobject.Wave
import yz.cool.towerverse.simulation.game.Enemy
import yz.cool.towerverse.simulation.ui.FloatingTextParticle
import yz.cool.towerverse.types.Color

/**
 * Represents the enemy spawner containing its own internal state for wave progression in the game.
 *
 * This object does not store any reference to [GameSimulation].
 */
@Serializable
class EnemySpawner {
    var waveNumber = 0
    /** Total duration of the current wave in seconds. */
    var currentWaveDuration = 10.0
    /** The time elapsed since the start of the current wave, in seconds. */
    var timeSinceWaveStart = 0.0

    /** Queued enemies for the current wave, ordered in **decreasing** order of spawn time.
     * When an enemy is spawned, it will be removed from the end of this list. */
    val queuedEnemies = ArrayList<QueuedEnemy>()
    @Serializable
    class QueuedEnemy(
        val spawnTime: Double,
        val spawnpoint: Spawnpoint,
        val type: EnemyType,
        val level: Int
    )

    /**
     * Ticks this [EnemySpawner].
     */
    fun GameSimulation.tick() {
        timeSinceWaveStart += 0.05
        // Check whether we should spawn the next wave
        if(timeSinceWaveStart >= currentWaveDuration && !isFinalWave) {
            // Spawn the next wave
            waveNumber += 1
            spawnNextWave()
            timeSinceWaveStart = 0.0

            // Render text particle
            ui?.let {
                it.particles += FloatingTextParticle(
                    map.width * 0.5, map.height * 0.5, "Wave $waveNumber", map.height * 0.1, Color.YELLOW,
                    lifetime = 5.0
                )
            }
        }

        // Spawn queued enemies that are due to be spawned now
        while(queuedEnemies.isNotEmpty()) {
            val queuedEnemy = queuedEnemies.last()
            if(queuedEnemy.spawnTime <= timeSinceWaveStart) {
                spawnEnemy(queuedEnemy.spawnpoint, queuedEnemy.type, queuedEnemy.level)
                queuedEnemies.removeLast()
            }
            else break
        }
    }

    /**
     * Spawns the wave at the current wave number, including instantly spawning queued enemies from the last wave.
     */
    fun GameSimulation.spawnNextWave() {
        queuedEnemies.forEach {
            spawnEnemy(it.spawnpoint, it.type, it.level)
        }
        queuedEnemies.clear()

        // Add queued enemies
        val wave = map.waveDefinition.getWaveAt(waveNumber)
        if(wave != null) {
            currentWaveDuration = map.waveDefinition.waveInterval * wave.waveDurationMultiplier

            spawnWave(wave)
        }
        else currentWaveDuration = map.waveDefinition.waveInterval

        // Give money reward
        val moneyReward = randomlyRound(
            map.waveDefinition.baseWaveReward *
                    (1.0 + map.waveDefinition.rewardScalingRate * (waveNumber - 1))
        )
        money += moneyReward
        if(moneyReward > 0) {
            ui?.apply {
                particles += FloatingTextParticle(map.width * 0.5, map.height * 0.6, "+$$moneyReward",
                    color = Color.GREEN, size = map.height * 0.06,
                    lifetime = 5.0
                )
            }
        }
    }

    /**
     * Spawns the given wave.
     */
    fun GameSimulation.spawnWave(wave: Wave) {
        val amount = map.waveDefinition.baseAmount *
                (1.0 + map.waveDefinition.amountScalingRate * (waveNumber - 1)) *
                wave.amountMultiplier
        val level = 1.0 + map.waveDefinition.levelScalingRate * (waveNumber - 1) + wave.levelModifier
        val spawnpoints = if(wave.spawnpoints.isEmpty()) map.spawnpoints else map.spawnpoints.filter { spawnpoint ->
                wave.spawnpoints.any { it in spawnpoint.tags }
            }.ifEmpty { map.spawnpoints }

        if(wave.isSpecialWave) { // Special wave: entire wave will spawn the same type, and this type is random
            val type = wave.getWeightedRandomEnemyType(random)
            for(i in 0 until amount.toInt()) {
                queuedEnemies += QueuedEnemy(
                    spawnTime = wave.spawnDelay + wave.spawnDuration * random.nextDouble(),
                    spawnpoint = spawnpoints.random(random),
                    type = type,
                    level = randomlyRound(level)
                )
            }
        }
        else { // Normal wave: each enemy will be a potentially different random type
            for(i in 0 until amount.toInt()) {
                val type = wave.getWeightedRandomEnemyType(random)
                queuedEnemies += QueuedEnemy(
                    spawnTime = wave.spawnDelay + wave.spawnDuration * random.nextDouble(),
                    spawnpoint = spawnpoints.random(random),
                    type = type,
                    level = randomlyRound(level)
                )
            }
        }
        queuedEnemies.sortByDescending { it.spawnTime }

        for(childWave in wave.childWaves) {
            spawnWave(childWave)
        }
    }

    fun GameSimulation.spawnEnemy(spawnpoint: Spawnpoint, type: EnemyType, level: Int) {
        enemies += Enemy(type, spawnpoint.location.x + 0.5, spawnpoint.location.y + 0.5, level)
    }
}