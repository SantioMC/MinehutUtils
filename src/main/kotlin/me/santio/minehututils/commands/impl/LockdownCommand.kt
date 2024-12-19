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
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.CommandData

@AutoService(SlashCommand::class)
class LockdownCommand : SlashCommand {

    override fun getData(): CommandData {
        return Command("lockdown", "Lock or unlock channels to prevent users from speaking in public channels") {
            isGuildOnly = true
            defaultPermissions = DefaultMemberPermissions.enabledFor(Permission.MESSAGE_MANAGE)

            addSubcommands(
                Subcommand("channel", "Specify a single channel to modify") {
                    addOptions(
                        Option<TextChannel>("channel", "The channel to modify", true),
                        Option<Boolean>("lock", "Whether to lock or unlock the channel", true)
                    )
                },
                Subcommand("all", "Lock or unlock all channels") {
                    addOptions(
                        Option<Boolean>("lock", "Whether to lock or unlock all channels", true)
                    )
                }
            )
        }
    }

    private suspend fun channel(event: SlashCommandInteractionEvent) {
        val channel = event.getOption("channel")?.asChannel ?: error("Channel not provided")
        val lock = event.getOption("lock")?.asBoolean ?: error("Lock not provided")
        if (!listOf(ChannelType.TEXT, ChannelType.FORUM).contains(channel.type)) error("Channel is not a text channel")

        val locked = Lockdown.isLocked(channel.asStandardGuildChannel())
        if (locked == lock) error("Channel is already ${if (lock) "locked" else "unlocked"}")

        Lockdown.lock(channel.asStandardGuildChannel(), lock)

        GuildLogger.of(event.guild!!).log(
            "A channel was ${if (lock) "locked" else "unlocked"} by ${event.user.asMention}",
            ":identification_card: User: ${event.member?.asMention} *(${event.user.name} - ${event.user.id})*",
            ":package: Channel: ${channel.asMention} *(${channel.name} - ${channel.id})*",
            ":label: Locked: ${if (lock) "Yes" else "No"}"
        ).withContext(event).titled("Channel Lockdown Changed").post()

        event.replyEmbeds(
            EmbedFactory.success("Successfully ${if (lock) "locked" else "unlocked"} the channel ${channel.asMention}!", event.guild!!).build()
        ).setEphemeral(true).queue()
    }

    private suspend fun all(event: SlashCommandInteractionEvent) {
        val lock = event.getOption("lock")?.asBoolean ?: error("Lock not provided")

        Lockdown.lockAll(event.guild!!.id, lock)

        GuildLogger.of(event.guild!!).log(
            "The server was ${if (lock) "locked" else "unlocked"} by ${event.user.asMention}",
            ":identification_card: User: ${event.member?.asMention} *(${event.user.name} - ${event.user.id})*",
            ":label: Locked: ${if (lock) "Yes" else "No"}"
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
