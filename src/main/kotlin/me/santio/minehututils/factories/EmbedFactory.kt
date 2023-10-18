package me.santio.minehututils.factories

import net.dv8tion.jda.api.EmbedBuilder

/**
 * The factory for creating embed messages that the bot sends.
 * @see EmbedBuilder
 */
object EmbedFactory {

    /**
     * The base embed that all embeds are based off of.
     * @param block The block that is used to modify the embed.
     * @return The embed that is created.
     */
    private fun baseEmbed(block: (EmbedBuilder) -> Unit): EmbedBuilder {
        val embed = EmbedBuilder()
        embed.setTitle(" ")
        embed.setColor(0x00FF00)
        block(embed)
        return embed
    }

    /**
     * A default embed that is used for general messages.
     * @param text The text that is displayed in the embed.
     * @param block The block that is used to modify the embed.
     * @return The embed that is created.
     */
    fun default(text: String, block: (EmbedBuilder) -> Unit = {}): EmbedBuilder {
        return baseEmbed {
            it.setDescription(text)
            block(it)
        }
    }

    /**
     * An embed that is used for success messages.
     * @param text The text that is displayed in the embed.
     * @param block The block that is used to modify the embed.
     * @return The embed that is created.
     */
    fun success(text: String, block: (EmbedBuilder) -> Unit = {}): EmbedBuilder {
        return baseEmbed {
            it.setDescription(text)
            block(it)
        }
    }

    /**
     * An embed that is used for error messages.
     * @param text The text that is displayed in the embed.
     * @param block The block that is used to modify the embed.
     * @return The embed that is created.
     */
    fun error(text: String, block: (EmbedBuilder) -> Unit = {}): EmbedBuilder {
        return baseEmbed {
            it.setDescription(text)
            block(it)
        }
    }

}