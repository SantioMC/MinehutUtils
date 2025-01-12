package me.santio.minehututils.skript

import me.santio.minehututils.skript.models.SkriptSyntax
import me.santio.minehututils.utils.TextHelper.ansiColor

/**
 * A rough representation of the Skript syntax, allowing for coloring and formatting
 * @author santio
 */
object Syntax {

    val scopeKeywords = listOf(
        Regex("on [\\w ]+:"), // events
        Regex("every [\\w ]+:"), // timers
        Regex("variables:"), // variables
        Regex("command /?\\w+:"), // commands
        Regex("(local )?function ?\\w+\\(.+\\)?:"), // functions
    )

    val identifiers = listOf(
        Regex("\\{[\\w:_%$* ]+}"), // variables
        Regex("(the )?((loop|event)-)?player(-\\d)?"), // player
        Regex("victim|target|attacker"), // victim
        Regex("\"[^\n]+\"") // string
    )

    val primaryKeywords = Regex(
        "(else )?if .+:" + // If statements
        "|else:" + // Else statements
        "|stop" + // Stop (return)
        "|loop .+:" + // Loops
        "|chance of \\d%?:?" + // Chance
        "|cancel event" // Cancel event
    )

    val comments = Regex("#[^\"\n]+")

    /**
     * Takes a syntax expression and returns the ansi-colored syntax
     * @param syntax The syntax to color
     * @return The colored syntax
     */
    fun skriptExpression(syntax: SkriptSyntax): String {
        var result = StringBuilder()
        var inTemplate = false
        var colorHistory = mutableListOf<String>()

        fun color(color: String, text: Char) {
            colorHistory.add(color)
            result.append(color + text)
        }

        fun endColor(text: Char) {
            colorHistory.removeLast()
            val color = colorHistory.lastOrNull() ?: "\u001b[0m"
            result.append(text + color)
        }

        syntax.syntaxPattern.forEach {
            when(it) {
                '[' -> color("\u001b[0;36m", it)
                '(' -> color("\u001b[0;34m", it)
                '%' -> {
                    inTemplate = !inTemplate
                    if (!inTemplate) endColor(it)
                    else color("\u001b[0;33m", it)
                }
                ']' -> endColor(it)
                ')' -> endColor(it)
                else -> result.append(it)
            }
        }

        return result.toString()
    }

    fun skriptSyntax(syntax: String): String {
        var result = syntax
        scopeKeywords.forEach {
            result = result.ansiColor(it, "\u001b[0;34m")
        }
        identifiers.forEach {
            result = result.ansiColor(it, "\u001b[0;33m")
        }

        result = result.ansiColor(primaryKeywords, "\u001b[0;31m")
        result = result.ansiColor(comments, "\u001b[0;30m")

        return result
    }

}
