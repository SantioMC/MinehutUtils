package me.santio.minehututils.logger

import me.santio.minehututils.resolvers.ChannelResolver
import me.santio.minehututils.utils.EnvUtils.env
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel

@Suppress("unused")
class GuildLogger private constructor(private val guild: Guild) {

    fun channel() = ChannelResolver.fromName(guild, env("LOG_CHANNEL", "logs")) as? TextChannel
    fun isEnabled() = channel() != null

    fun log(vararg message: String): Log {
        return Log(this, message.joinToString("\n **↳** "))
    }

    companion object {
        fun of(guild: Guild) = GuildLogger(guild)
    }

}
