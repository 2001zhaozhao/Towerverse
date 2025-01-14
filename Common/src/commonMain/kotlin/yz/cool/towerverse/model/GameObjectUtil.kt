package yz.cool.towerverse.model

/**
 * Utilities for Towerverse's game object model
 */
object GameObjectUtil {
    /** Returns whether the given string is a valid game object ID. */
    fun validateIdFormat(id: String): Boolean = id.all{ it.isLetterOrDigit() || it == '_' || it == '-' || it == ':' }

    /** Returns whether the given string is a valid single segment of a game object ID.
     * (This means that the string cannot contain any colons.) */
    fun validateIdSegmentFormat(id: String): Boolean = id.all{ it.isLetterOrDigit() || it == '_' || it == '-' }

    /** Converts a camelCase string to a string with spaces between words. */
    fun prettifyString(s: String): String {
        val sb = StringBuilder()
        s.forEachIndexed { i, c ->
            if(i > 0 && c.isUpperCase() && s[i - 1].isLowerCase()) {
                sb.append(' ')
            }

            if(i == 0) sb.append(c.uppercase())
            else sb.append(c)
        }
        return sb.toString()
    }
}