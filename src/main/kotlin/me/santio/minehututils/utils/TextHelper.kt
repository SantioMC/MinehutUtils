package me.santio.minehututils.utils

/**
 * Some simple helper utilities for text
 * @author santio
 */
object TextHelper {

    /**
     * Converts the provided text to title case
     * e.g. hello world -> Hello World
     * @return The title cased text
     */
    fun String.titlecase(): String {
        return this.lowercase().split(" ").joinToString(" ") { it.replaceFirstChar { it.uppercase() } }
    }

    /**
     * Finds all matches in the string and replaces them with the provided color, this isn't a perfect solution
     * for coloring, however for the time being this works well.
     * @param regex The regex to find
     * @param color The color to replace the matches with
     * @return The string with the matches replaced with the color
     */
    fun String.ansiColor(regex: Regex, color: String): String {
        var buffer = this

        regex.findAll(this).forEach {
            buffer = buffer.replace(it.value, color + it.value + "\u001b[0m")
        }

        return buffer
    }

}
