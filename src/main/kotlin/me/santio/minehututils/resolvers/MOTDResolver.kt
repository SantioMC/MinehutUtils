package me.santio.minehututils.resolvers

import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

/**
 * Resolves motds from MiniMessage format (Adventure API) to Plain Text
 */
object MOTDResolver {

    /**
     * Converts MiniMessage format to Plain text
     */
    fun clean(motd: String): String {
        val oldFormat = LegacyComponentSerializer.legacySection().deserialize(motd)
        val text = PlainTextComponentSerializer.plainText().serialize(oldFormat)

        val miniMessage = MiniMessage.miniMessage().deserialize(text)
        return PlainTextComponentSerializer.plainText().serialize(miniMessage)
            .trimIndent()
            .split("\n")
            .dropLastWhile { it.isBlank() }
            .joinToString("\n")
            .ifEmpty { "A Minehut Production" }
    }

}