package me.santio.minehututils.lockdown

import com.google.auto.service.AutoService
import me.santio.minehututils.bot
import me.santio.minehututils.database.DatabaseHandler
import me.santio.minehututils.database.DatabaseHook
import me.santio.minehututils.database.models.LockdownChannel
import me.santio.minehututils.factories.EmbedFactory
import me.santio.minehututils.iron
import me.santio.minehututils.lockdown.Lockdown.lock
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.PermissionOverride
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildChannel

/**
 * The lockdown manager for handling the locking of channels and state. In case a channel was locked
 * manually, we want to be able to detect that and unlock or lock it when needed so that we never mess up
 * channel permissions.
 * @author santio
 */
object Lockdown: DatabaseHook {

    private val lockdownChannels = mutableListOf<LockdownChannel>()

    override suspend fun onHook() {
        val channels = iron.prepare("SELECT * FROM lockdown_channels").all<LockdownChannel>()
        lockdownChannels.addAll(channels)
    }

    private suspend fun getModifyingRole(guild: Guild): Role {
        return DatabaseHandler.getSettings(guild.id).lockdownRole?.let { guild.getRoleById(it) }
            ?: guild.roles.firstOrNull { it.name == "@everyone" }
            ?: error("Failed to find the @everyone role in the guild")
    }

    private suspend fun getPermissionOverride(guild: Guild, channel: StandardGuildChannel): PermissionOverride {
        val role = getModifyingRole(guild)

        return channel.rolePermissionOverrides.firstOrNull {
            it.role == role
        } ?: channel.upsertPermissionOverride(role).complete()
    }

    /**
     * Set the channels to lockdown when a user attempts to issue a lockdown
     * @param guild The guild holding the channels
     * @param channel The channels to lockdown
     */
    suspend fun setChannels(guild: String, channel: List<String>) {
        val channels = channel.map { LockdownChannel(guild, it) }

        iron.transaction {
            prepare("DELETE FROM lockdown_channels WHERE guild_id = ?", guild)

            for (channel in channels) {
                prepare(
                    "INSERT INTO lockdown_channels(guild_id, channel_id) VALUES (?, ?)",
                    guild,
                    channel.channelId
                )
            }
        }

        lockdownChannels.removeIf { it.guildId == guild }
        lockdownChannels.addAll(channels)
    }

    fun getLockdownChannels(string: String): List<String> {
        return this.lockdownChannels.mapNotNull { if (it.guildId == string) it.channelId else null }
    }

    /**
     * Check if the @everyone role in the channel has permission to send messages explicitly denied, if it
     * doesn't then the channel is opened, otherwise we assume it's locked.
     * @param textChannel The text channel to check
     * @return Whether the channel is locked or not
     */
    suspend fun isLocked(textChannel: StandardGuildChannel): Boolean {
        val permissions = getPermissionOverride(textChannel.guild, textChannel)
        return permissions.denied.contains(Permission.MESSAGE_SEND)
    }

    /**
     * Lock or unlock a channel
     * @param channel The text channel to lock or unlock
     * @param lock Whether to lock or unlock the channel
     */
    suspend fun lock(channel: StandardGuildChannel, lock: Boolean, reason: String? = null) {
        val permissions = getPermissionOverride(channel.guild, channel)
        val channels = getLockdownChannels(channel.guild.id)

        if (channel.id !in channels) error("Attempted to lockdown a channel that I shouldn't have tried to.")

        if (lock && !permissions.denied.contains(Permission.MESSAGE_SEND)) {
            permissions.manager.setDenied(
                permissions.denied
                    + Permission.MESSAGE_SEND
                    + Permission.MESSAGE_SEND_IN_THREADS
            ).queue() // Explicitly deny the @everyone role from speaking

            if (channel is TextChannel) {
                channel.sendMessageEmbeds(
                    EmbedFactory.default(
                        """
                        :lock: The channel has been locked by a moderator.
                        
                        ${reason ?: ""}
                        """.trim()
                    ).build()
                ).queue()
            }
        } else if (!lock && permissions.denied.contains(Permission.MESSAGE_SEND)) {
            permissions.manager.clear(Permission.MESSAGE_SEND, Permission.MESSAGE_SEND_IN_THREADS).queue() // Default to the guild default, cleaning up our mess

            if (channel is TextChannel) {
                // If our message was the last message in the channel, delete it, otherwise we'll send a new one
                val lastMessage = channel.latestMessageId.takeIf { it != "0" }
                    ?.let { channel.retrieveMessageById(it).complete() }

                if (lastMessage?.author?.id == bot.selfUser.id) {
                    lastMessage.delete().queue()
                } else {
                    channel.sendMessageEmbeds(EmbedFactory.default(
                        ":unlock: The channel has been unlocked by a moderator.",
                    ).build()).queue()
                }
            }

        }
    }

    suspend fun lockAll(guild: String, lock: Boolean, reason: String? = null) {
        val channels = getLockdownChannels(guild)

        for (channel in channels) {
            val channel = bot.getGuildChannelById(channel) ?: continue
            this.lock(channel as StandardGuildChannel, lock, reason)
        }
    }

}

@AutoService(DatabaseHook::class)
class LockdownProxy: DatabaseHook by Lockdown
