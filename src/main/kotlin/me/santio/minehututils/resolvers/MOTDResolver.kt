package me.santio.minehututils.resolvers

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.KeybindComponent
import net.kyori.adventure.text.SelectorComponent
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.kyori.ansi.ColorLevel


/**
 * Resolves motds from MiniMessage format (Adventure API) to Plain Text
 */
@Suppress("unused")
object MOTDResolver {

    private const val MAX_LENGTH = 4000

    private val miniMessage = MiniMessage.miniMessage()
    private val ansi = ANSIComponentSerializer.builder()
        .colorLevel(ColorLevel.TRUE_COLOR)
        .build()

    private val lineBreak = Regex("\n|<(?i:br|newline)>")

    /**
     * Parses a motd in MiniMessage or legacy format. Each line, split at new lines and at `<br>` or `<newline>`
     * tags, is parsed on its own, so tags and gradients don't carry over to the next line. Motds longer than
     * [MAX_LENGTH] characters are cut off.
     * @param motd The motd to parse
     * @return The parsed motd
     */
    fun toComponent(motd: String): Component {
        val oldFormat = LegacyComponentSerializer.legacySection()
            .deserialize(motd.take(MAX_LENGTH).dropLastWhile { it.isHighSurrogate() })
        val text = PlainTextComponentSerializer.plainText().serialize(oldFormat)
        return Component.join(JoinConfiguration.newlines(), text.split(lineBreak).map { miniMessage.deserialize(it) })
    }

    /**
     * Converts MiniMessage format to Plain text
     * @param motd The motd to convert
     * @return The plain text motd
     */
    fun clean(motd: String): String = clean(toComponent(motd))

    /**
     * Converts a parsed motd to plain text
     * @param motd The parsed motd
     * @return The plain text motd
     */
    fun clean(motd: Component): String {
        return StringBuilder().apply { appendText(motd) }.toString()
            .trimIndent()
            .split("\n")
            .dropLastWhile { it.isBlank() }
            .joinToString("\n")
    }

    /**
     * Appends a component's text the same way the plain text serializer does, without its limit of 512 nested
     * components
     */
    private fun StringBuilder.appendText(component: Component) {
        when (component) {
            is TextComponent -> append(component.content())
            is TranslatableComponent -> append(component.key())
            is KeybindComponent -> append(component.keybind())
            is SelectorComponent -> append(component.pattern())
            else -> {}
        }
        component.children().forEach { appendText(it) }
    }

    /**
     * Converts MiniMessage format to plain text with ANSI colors
     * @param motd The motd to convert
     * @return The ANSI formatted motd
     */
    fun toAnsi(motd: String): String {
        val miniMessage = toComponent(motd)
        return ansi.serialize(miniMessage)
            .trimIndent()
            .split("\n")
            .dropLastWhile { it.isBlank() }
            .joinToString("\n\u001B[0m")
    }

}
