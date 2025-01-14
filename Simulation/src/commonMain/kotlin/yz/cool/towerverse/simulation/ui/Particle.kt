package yz.cool.towerverse.simulation.ui

import yz.cool.towerverse.types.Color

/**
 * Represents a particle that can be rendered in some way and removed after a time.
 */
abstract class Particle(
    val lifetime: Double
) {
    var remainingLifetime: Double = lifetime
}

/** In-world floating text particle */
class FloatingTextParticle(
    val x: Double,
    val y: Double,
    val text: String,
    val size: Double = 0.4,
    val color: Color = Color.WHITE,
    lifetime: Double = 2.0
) : Particle(lifetime)

/** A circle at a given location */
class CircleParticle(
    val x: Double,
    val y: Double,
    val radius: Double,
    val color: Color = Color.WHITE,
    val isSolid: Boolean = true,
    lifetime: Double = 2.0
) : Particle(lifetime)

/** A line between two points */
class LineParticle(
    val x1: Double,
    val y1: Double,
    val x2: Double,
    val y2: Double,
    val color: Color = Color.WHITE,
    val lineWidth: Double = 1.0,
    lifetime: Double = 2.0
) : Particle(lifetime)

/** A particle for splash damage which grows slightly and fades over time */
class SplashParticle(
    val x: Double,
    val y: Double,
    val radius: Double,
    val color: Color = Color.WHITE,
    lifetime: Double = 2.0
) : Particle(lifetime)