package me.santio.minehututils.lockdown

import com.google.auto.service.AutoService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.santio.minehututils.bot
import me.santio.minehututils.coroutines.await
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
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import java.util.concurrent.CopyOnWriteArrayList

/**
 * The lockdown manager for handling the locking of channels and state. In case a channel was locked
 * manually, we want to be able to detect that and unlock or lock it when needed so that we never mess up
 * channel permissions.
 * @author santio
 */
object Lockdown: DatabaseHook {

    private val lockdownChannels = CopyOnWriteArrayList<LockdownChannel>()
    private val lockdownPermissions = setOf(
        Permission.MESSAGE_SEND,
        Permission.MESSAGE_SEND_IN_THREADS,
        Permission.MESSAGE_ADD_REACTION,
    )

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
        } ?: withContext(Dispatchers.IO) {
            channel.upsertPermissionOverride(role).complete()
        }
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
        return this.lockdownChannels.filter { it.guildId == string }.map { it.channelId }
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
     * @return A warning for the moderator if the channel was changed but the notice couldn't be posted
     */
    suspend fun lock(channel: StandardGuildChannel, lock: Boolean, reason: String? = null): String? {
        val permissions = getPermissionOverride(channel.guild, channel)
        val channels = getLockdownChannels(channel.guild.id)

        if (channel.id !in channels) error("Attempted to lockdown a channel that I shouldn't have tried to.")

        if (lock && !permissions.denied.contains(Permission.MESSAGE_SEND)) {
            val manager = permissions.manager // Fails early if we can't change the channel's permissions
            var warning: String? = null

            // Post the notice before locking, as locking could also stop the bot from speaking
            if (channel is TextChannel) {
                val notice = EmbedFactory.default(
                    """
                        :lock: The channel has been locked by a moderator.
                        
                        ${reason ?: ""}
                        """.trim()
                ).build()

                runCatching { channel.sendMessageEmbeds(notice).await() }
                    .onFailure {
                        if (it is CancellationException) throw it
                        warning = "The channel was locked, but I couldn't post the lock notice."
                    }
            }

            // Explicitly deny the @everyone role from speaking
            manager.deny(lockdownPermissions).await()
            return warning
        } else if (!lock && permissions.denied.contains(Permission.MESSAGE_SEND)) {
            // Default to the guild default, cleaning up our mess
            runCatching { permissions.manager.clear(lockdownPermissions) }.fold(
                { it.await() },
                { err ->
                    // Locking also removed MESSAGE_SEND from the bot in this channel, and PermissionOverrideAction
                    // refuses to change permissions the bot doesn't currently have there, even though Discord allows
                    // it. The channel manager only checks the permissions that are kept, so retry through it.
                    if (err !is InsufficientPermissionException || err.permission != Permission.MANAGE_PERMISSIONS) throw err

                    val holder = permissions.permissionHolder ?: throw err
                    val allowed = permissions.allowedRaw
                    val denied = permissions.deniedRaw and Permission.getRaw(lockdownPermissions).inv()

                    val channelManager = channel.manager
                    val manager = runCatching { channelManager.putPermissionOverride(holder, allowed, denied) }
                        .getOrElse { putErr ->
                            if (putErr !is InsufficientPermissionException) throw putErr
                            val missing = Permission.getPermissions(allowed or denied) - (channel.guild.selfMember.getPermissions(channel) - Permission.MANAGE_PERMISSIONS)
                            if (missing.isEmpty()) throw putErr

                            val names = missing.joinToString(", ") { it.getName() }
                            val name = (holder as? Role)?.name ?: "the lockdown role"
                            error(
                                "The $name permissions in this channel also change $names, which I don't have here, " +
                                    "so I can't unlock it without losing those. Give me Manage Permissions on this channel, " +
                                    "or remove those from the channel's $name permissions."
                            )
                        }

                    manager.await()
                }
            )

            if (channel is TextChannel) {
                // If our message was the last message in the channel, delete it, otherwise we'll send a new one.
                // The last message may have been deleted since, in which case there's nothing to clean up.
                val lastMessage = channel.latestMessageId.takeIf { it != "0" }
                    ?.let { runCatching { channel.retrieveMessageById(it).await() }.getOrNull() }

                val deleting = lastMessage?.author?.id == bot.selfUser.id
                return runCatching {
                    if (deleting) {
                        lastMessage!!.delete().await()
                    } else {
                        channel.sendMessageEmbeds(EmbedFactory.default(
                            ":unlock: The channel has been unlocked by a moderator.",
                        ).build()).await()
                    }
                }.exceptionOrNull()?.let {
                    if (it is CancellationException) throw it
                    if (deleting) "The channel was unlocked, but I couldn't remove the lock notice."
                    else "The channel was unlocked, but I couldn't post the unlock notice."
                }
            }
        }

        return null
    }

    suspend fun lockAll(guild: String, lock: Boolean, reason: String? = null): Set<String> {
        val channels = getLockdownChannels(guild)
        val errors = mutableSetOf<String>()

        for (channel in channels) {
            val channel = bot.getGuildChannelById(channel) ?: continue
            runCatching {
                this.lock(channel as StandardGuildChannel, lock, reason)
            }.getOrElse { err ->
                val error = when(err) {
                    is InsufficientPermissionException -> "Missing permission ${err.permission.name}"
                    else -> err.message ?: err.javaClass.simpleName
                }

                errors += "Failed to ${if (lock) "lock" else "unlock"} ${channel.name}: ${error}"
            }
        }

        return errors
    }

}

@AutoService(DatabaseHook::class)
class LockdownProxy: DatabaseHook by Lockdown
