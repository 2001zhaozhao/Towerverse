package yz.cool.towerverse.types

import kotlinx.serialization.Serializable

/**
 * A basic RGBA color class.
 */
@Serializable
data class Color(val r: Int, val g: Int, val b: Int, val a: Int = 255) {
    companion object {
        val BLACK = Color(0, 0, 0)
        val WHITE = Color(255, 255, 255)
        val RED = Color(255, 0, 0)
        val YELLOW = Color(255, 255, 0)
        val GREEN = Color(0, 255, 0)
        val CYAN = Color(0, 255, 255)
        val BLUE = Color(0, 0, 255)
        val MAGENTA = Color(255, 0, 255)
    }

    /** Constructs a gray color with the given lightness. */
    constructor(lightness: Int) : this(lightness, lightness, lightness, 255)

    init {
        require(r in 0..255) {"Invalid red value: $r"}
        require(g in 0..255) {"Invalid green value: $g"}
        require(b in 0..255) {"Invalid blue value: $b"}
        require(a in 0..255) {"Invalid alpha value: $a"}
    }

    /** Multiplies this color by the given alpha value. */
    fun multiplyAlpha(alpha: Double) = if(alpha == 1.0) this else
        Color(r, g, b, (a * alpha).coerceAtMost(255.0).toInt())

    /** Converts this color to a CSS string. */
    fun toCssString() = "rgba($r, $g, $b, $a)"
}