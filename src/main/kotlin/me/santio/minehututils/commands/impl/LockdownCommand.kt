package me.santio.minehututils.commands.impl

import com.google.auto.service.AutoService
import dev.minn.jda.ktx.interactions.commands.Command
import dev.minn.jda.ktx.interactions.commands.Option
import dev.minn.jda.ktx.interactions.commands.Subcommand
import me.santio.minehututils.commands.SlashCommand
import me.santio.minehututils.factories.EmbedFactory
import me.santio.minehututils.lockdown.Lockdown
import me.santio.minehututils.logger.GuildLogger
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.CommandData

@AutoService(SlashCommand::class)
class LockdownCommand : SlashCommand {

    override fun getData(): CommandData {
        return Command("lockdown", "Lock or unlock channels to prevent users from speaking in public channels") {
            setContexts(InteractionContextType.GUILD)
            defaultPermissions = DefaultMemberPermissions.enabledFor(Permission.MESSAGE_MANAGE)

            addSubcommands(
                Subcommand("channel", "Specify a single channel to modify") {
                    addOptions(
                        Option<TextChannel>("channel", "The channel to modify", false),
                        Option<Boolean>("lock", "Whether to lock or unlock the channel", false),
                        Option<String>("reason", "The reason for locking the channel", false)
                    )
                },
                Subcommand("all", "Lock or unlock all channels") {
                    addOptions(
                        Option<Boolean>("lock", "Whether to lock or unlock all channels", false),
                        Option<String>("reason", "The reason for locking all channels", false)
                    )
                }
            )
        }
    }

    private suspend fun channel(event: SlashCommandInteractionEvent) {
        val channel = event.getOption("channel")?.asChannel?.asStandardGuildChannel() ?: event.channel.asTextChannel()
        val currentlyLocked = Lockdown.isLocked(channel)

        val lock = event.getOption("lock")?.asBoolean ?: !currentlyLocked
        val reason = event.getOption("reason")?.asString
        if (!listOf(ChannelType.TEXT, ChannelType.FORUM).contains(channel.type)) error("Channel is not a text channel")

        val channels = Lockdown.getLockdownChannels(channel.guild.id)
        if (channel.id !in channels) error("You are not allowed to lockdown this channel")

        val locked = Lockdown.isLocked(channel)
        if (locked == lock) error("Channel is already ${if (lock) "locked" else "unlocked"}")

        Lockdown.lock(channel, lock, reason)

        GuildLogger.of(event.guild!!).log(
            "A channel was ${if (lock) "locked" else "unlocked"} by ${event.user.asMention}",
            ":identification_card: User: ${event.member?.asMention} *(${event.user.name} - ${event.user.id})*",
            ":package: Channel: ${channel.asMention} *(${channel.name} - ${channel.id})*",
            ":label: Locked: ${if (lock) "Yes" else "No"}",
            ":label: Reason: ${reason?.trim() ?: "No reason provided"}"
        ).withContext(event).titled("Channel Lockdown Changed").post()

        event.replyEmbeds(
            EmbedFactory.success("Successfully ${if (lock) "locked" else "unlocked"} the channel ${channel.asMention}!", event.guild!!).build()
        ).setEphemeral(true).queue()
    }

    private suspend fun all(event: SlashCommandInteractionEvent) {
        val lock = event.getOption("lock")?.asBoolean ?: true
        val reason = event.getOption("reason")?.asString

        Lockdown.lockAll(event.guild!!.id, lock, reason)

        GuildLogger.of(event.guild!!).log(
            "The server was ${if (lock) "locked" else "unlocked"} by ${event.user.asMention}",
            ":identification_card: User: ${event.member?.asMention} *(${event.user.name} - ${event.user.id})*",
            ":label: Locked: ${if (lock) "Yes" else "No"}",
            ":label: Reason: ${reason?.trim() ?: "No reason provided"}"
        ).withContext(event).titled("Server Lockdown Changed").post()

        event.replyEmbeds(
            EmbedFactory.success("Successfully ${if (lock) "locked" else "unlocked"} all channels!", event.guild!!).build()
        ).setEphemeral(true).queue()
    }

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        when (event.subcommandName) {
            "channel" -> channel(event)
            "all" -> all(event)
            else -> error("Subcommand not found", true)
        }
    }

}
