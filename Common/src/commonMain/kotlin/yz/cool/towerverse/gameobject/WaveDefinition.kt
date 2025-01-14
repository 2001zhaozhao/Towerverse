package yz.cool.towerverse.gameobject

import kotlinx.serialization.Serializable
import yz.cool.towerverse.model.GameObjectListReference
import yz.cool.towerverse.model.GameObjectMapReference
import kotlin.random.Random

/**
 * Defines all waves within a map. This is inspired by the Mob-Arena plugin for Minecraft.
 *
 * @param finalWave The number of the last wave in the map.
 * Once the last wave spawns, no more waves will spawn and clearing all enemies will end the game.
 * Defaults to 10. Setting this to 0 will make the map endless.
 * @param waveInterval The base time between waves in seconds. Defaults to 30.
 * @param baseAmount The base number of enemies to spawn per wave.
 * @param baseWaveReward The base money reward when starting a wave.
 * @param levelScalingRate The rate at which enemies level up past wave 1, in levels per wave.
 * @param amountScalingRate The rate at which enemy spawns will increase past wave 1,
 * in percent of base value per wave (linear).
 * @param rewardScalingRate The rate at which the reward to player will scale per wave.
 */
@Serializable
data class WaveDefinition(
    var finalWave: Int = 10,
    var waveInterval: Double = 30.0,
    var baseAmount: Double = 5.0,
    var baseWaveReward: Double = 50.0,
    var levelScalingRate: Double = 0.5,
    var amountScalingRate: Double = 0.1,
    var rewardScalingRate: Double = 0.1,
    private val wavesRef: GameObjectListReference<Wave> = GameObjectListReference(),
    override var id: String = "",
    override var name: String = ""
) : GameObject() {
    /** All waves within the map. Some waves can be repeated.
     * If a wave number does not match any wave, no enemies will spawn in that wave. */
    var waves: List<Wave> by wavesRef

    /**
     * Get the wave that should be spawned at the given wave number.
     */
    fun getWaveAt(waveNumber: Int): Wave? {
        // Try to find wave with highest priority that matches wave number
        val waves = this.waves
        let {
            var maxPriority = Int.MIN_VALUE
            var maxWave: Wave? = null
            for (wave in waves) {
                if (wave.waveNumber == waveNumber && wave.priority > maxPriority) {
                    maxPriority = wave.priority
                    maxWave = wave
                }
            }
            if(maxWave != null) return maxWave
        }
        // Try to find wave with highest priority that recurs at this wave number
        let {
            var maxPriority = Int.MIN_VALUE
            var maxWave: Wave? = null
            for (wave in waves) {
                if (
                    wave.frequency != 0 &&
                    (waveNumber - wave.waveNumber) % wave.frequency == 0 &&
                    wave.priority > maxPriority
                ) {
                    maxPriority = wave.priority
                    maxWave = wave
                }
            }
            if(maxWave != null) return maxWave
        }
        // No wave found
        return null
    }

    companion object {
        operator fun invoke(action: WaveDefinition.() -> Unit): WaveDefinition = WaveDefinition().applyAtCompile(action)
    }
}

/**
 * Represents a single wave in which enemies may spawn.
 *
 * @param waveNumber The first wave number where this wave will spawn.
 * The wave will always spawn on this wave number unless another wave also has this number as its wave number.
 * Ignored for child waves.
 * @param priority The priority of the wave. Between repeating waves that can spawn on the same number, the
 * one with higher priority is spawned first. Ignored for child waves.
 * @param frequency The number of waves before this wave will spawn again.
 * Set to 0 to disable, meaning that the wave will only spawn once. Ignored for child waves.
 * @param amountMultiplier The multiplier to how many enemies spawn in the wave,
 * compared to the general wave scaling rules.
 * @param levelModifier The modifier to the level of all enemies in the wave.
 * @param waveDurationMultiplier The multiplier to the time given until the next wave spawns.
 * @param spawnDelay The time it takes for enemies to start spawning in this wave.
 * @param spawnDuration The time it takes for all enemies to spawn in the wave once [spawnDelay] has elapsed.
 * Note that if the next wave starts before enemies finish spawning,
 * all remaining enemies in this wave will be instantly spawned.
 * @param isSpecialWave If true, then only one type of enemy will spawn and the type will be randomly chosen according
 * to weights in the [enemies] map. If false, a mix of enemies will spawn according to the weights.
 */
@Serializable
data class Wave(
    var waveNumber: Int = 0,
    var priority: Int = 0,
    var frequency: Int = 0,
    var amountMultiplier: Double = 1.0,
    var levelModifier: Int = 0,
    var waveDurationMultiplier: Double = 1.0,
    var spawnDelay: Double = 0.0,
    var spawnDuration: Double = 0.0,
    var isSpecialWave: Boolean = false,
    private val enemiesRef: GameObjectMapReference<EnemyType, Double> = GameObjectMapReference(),
    private val spawnpointsRef: GameObjectListReference<SpawnpointTag> = GameObjectListReference(),
    private val childWavesRef: GameObjectListReference<Wave> = GameObjectListReference(),
    override var id: String = "",
    override var name: String = ""
) : GameObject() {
    /** A map containing enemy spawn weights in the wave. */
    var enemies: Map<EnemyType, Double> by enemiesRef

    /** A list of spawnpoint tags to spawn enemies at in the wave.
     * If this is empty or no spawnpoints are found with one of the given tags,
     * then all spawnpoints in the map will be eligible. */
    var spawnpoints: List<SpawnpointTag> by spawnpointsRef

    /** A list of child waves that will also be spawned when this wave is spawned. */
    var childWaves: List<Wave> by childWavesRef

    /** Get a random enemy type from the weighted list. */
    fun getWeightedRandomEnemyType(random: Random): EnemyType {
        var totalWeight = 0.0
        for (entry in enemies) {
            totalWeight += entry.value
        }
        var weight = random.nextDouble() * totalWeight
        for (entry in enemies) {
            weight -= entry.value
            if (weight <= 0.0) {
                return entry.key
            }
        }
        error("This should not happen")
    }

    companion object {
        operator fun invoke(action: Wave.() -> Unit): Wave = Wave().applyAtCompile(action)
    }
}