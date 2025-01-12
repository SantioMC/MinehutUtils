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

    fun String.ansiColor(regex: Regex, color: String): String {
        var buffer = this

        regex.findAll(this).forEach {
            buffer = buffer.replace(it.value, color + it.value + "\u001b[0m")
        }

        return buffer
    }

}
