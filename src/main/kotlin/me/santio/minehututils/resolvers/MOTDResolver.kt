package me.santio.minehututils.resolvers

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

    private val miniMessage = MiniMessage.miniMessage()
    private val ansi = ANSIComponentSerializer.builder()
        .colorLevel(ColorLevel.INDEXED_8)
        .build()

    /**
     * Converts MiniMessage format to Plain text
     * @param motd The motd to convert
     * @return The plain text motd
     */
    fun clean(motd: String): String {
        val oldFormat = LegacyComponentSerializer.legacySection().deserialize(motd)
        val text = PlainTextComponentSerializer.plainText().serialize(oldFormat)

        val miniMessage = miniMessage.deserialize(text)
        return PlainTextComponentSerializer.plainText().serialize(miniMessage)
            .trimIndent()
            .split("\n")
            .dropLastWhile { it.isBlank() }
            .joinToString("\n")
            .ifEmpty { "A Minehut Production" }
    }

    /**
     * Converts MiniMessage format to plain text with ANSI colors
     * @param motd The motd to convert
     * @return The ANSI formatted motd
     */
    fun toAnsi(motd: String): String {
        val oldFormat = LegacyComponentSerializer.legacySection().deserialize(motd)
        val text = PlainTextComponentSerializer.plainText().serialize(oldFormat)

        val miniMessage = miniMessage.deserialize(text)
        return ansi.serialize(miniMessage)
            .trimIndent()
            .split("\n")
            .dropLastWhile { it.isBlank() }
            .joinToString("\n\u001B[0m")
            .ifEmpty { "A Minehut Production" }
    }

}
