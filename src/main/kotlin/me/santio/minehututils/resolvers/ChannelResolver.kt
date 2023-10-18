package me.santio.minehututils.resolvers

import me.santio.minehututils.bot
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel

/**
 * Used to dynamically resolve channels from channel names
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
object ChannelResolver {

    private const val MINEHUT_GUILD = 239599059415859200L

    /**
     * Resolve a specific channel from the Minehut guild
     * @param name The name of the channel
     * @return The channel, or null if not found
     */
    fun fromMinehut(name: String): GuildChannel? {
        val guild = bot.getGuildById(MINEHUT_GUILD) ?: return null
        return guild.channels.firstOrNull { it.name.equals(name, true) }
    }

    /**
     * Resolve a specific channel from the Minehut guild
     * @param id The id of the channel
     * @return The channel, or null if not found
     */
    fun fromMinehut(id: Long): GuildChannel? {
        val guild = bot.getGuildById(MINEHUT_GUILD) ?: return null
        return guild.getGuildChannelById(id)
    }

    /**
     * Returns an embeddable piece of text that shows either the channel mention or
     * the channel name if the channel is not found
     * @param name The name of the channel to look for
     * @return The embeddable text, or '#<query>' if not found
     */
    fun findName(name: String): String {
        val channel = fromMinehut(name) ?: return "#$name"
        return channel.asMention
    }

}