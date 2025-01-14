package yz.cool.towerverse.types

import kotlinx.serialization.Serializable

/**
 * A basic immutable 2D vector of integers.
 */
@Serializable
data class TileVec(val x: Int, val y: Int) {
    constructor() : this(0, 0)
}