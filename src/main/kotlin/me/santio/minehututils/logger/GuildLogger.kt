package me.santio.minehututils.logger

import me.santio.minehututils.database.DatabaseHandler
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel

@Suppress("unused")
class GuildLogger private constructor(private val guild: Guild, val channel: TextChannel? = null) {

    fun isEnabled() = channel != null

    fun log(vararg message: String): Log {
        return Log(this, message.joinToString("\n **↳** "))
    }

    companion object {
        suspend fun of(guild: Guild): GuildLogger {
            val settings = DatabaseHandler.getSettings(guild.id)
            val channel = settings.logChannel?.let { guild.getTextChannelById(it) }

            return GuildLogger(guild, channel)
        }
    }

}
