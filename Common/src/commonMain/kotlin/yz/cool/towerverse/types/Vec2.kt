package yz.cool.towerverse.types

import kotlinx.serialization.Serializable

/**
 * A basic immutable 2D vector.
 */
@Serializable
data class Vec2(val x: Double, val y: Double) {
    constructor() : this(0.0, 0.0)
}