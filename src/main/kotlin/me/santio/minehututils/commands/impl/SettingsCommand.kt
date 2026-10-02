package me.santio.minehututils.commands.impl

import com.google.auto.service.AutoService
import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.interactions.commands.Command
import dev.minn.jda.ktx.interactions.commands.Option
import dev.minn.jda.ktx.interactions.commands.Subcommand
import dev.minn.jda.ktx.interactions.commands.SubcommandGroup
import dev.minn.jda.ktx.interactions.components.EntitySelectMenu
import me.santio.minehututils.commands.SlashCommand
import me.santio.minehututils.database.DatabaseHandler
import me.santio.minehututils.factories.EmbedFactory
import me.santio.minehututils.iron
import me.santio.minehututils.lockdown.Lockdown
import me.santio.minehututils.logger.GuildLogger
import me.santio.minehututils.resolvers.DurationResolver
import me.santio.minehututils.resolvers.EmojiResolver
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.selections.EntitySelectMenu
import net.dv8tion.jda.api.components.selections.EntitySelectMenu.SelectTarget
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import java.time.OffsetDateTime
import kotlin.time.Duration.Companion.seconds

@AutoService(SlashCommand::class)
class SettingsCommand: SlashCommand {

    override fun getData(): CommandData {
        return Command("settings", "Manage specific settings for the bot") {
            defaultPermissions = DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER)
            setContexts(InteractionContextType.GUILD)

            addSubcommands(
                Subcommand("view", "View the current settings")
            )

            addSubcommandGroups(
                SubcommandGroup("channel", "Manage channel settings") {
                    addSubcommands(
                        Subcommand("marketplace", "Set the channel to post listings in") {
                            addOptions(
                                Option<TextChannel>("channel", "The channel to post listings in", true)
                            )
                        }
                    )
                },
                SubcommandGroup("cooldown", "Manage the cooldown settings") {
                    addSubcommands(
                        Subcommand("marketplace", "Set the cooldown for posting listings") {
                            addOptions(
                                Option<String>("cooldown", "The cooldown for posting listings", true)
                            )
                        }
                    )
                },
                SubcommandGroup("lockdown", "Manage the lockdown settings") {
                    addSubcommands(
                        Subcommand("role", "Set the role to lock channels with") {
                            addOptions(
                                Option<Role>("role", "The role to lock channels with")
                            )
                        },
                        Subcommand("channels", "Which channels to restrict the lockdown command to")
                    )
                },
                SubcommandGroup("log", "Manage the log settings") {
                    addSubcommands(
                        Subcommand("channel", "Set the channel to post logs in") {
                            addOptions(
                                Option<TextChannel>("channel", "The channel to post logs in", true)
                            )
                        }
                    )
                },
            )
        }
    }

    private fun getNullIcon(value: Any?): String {
        return if (value == null) EmojiResolver.crossmark().formatted else EmojiResolver.checkmark().formatted
    }

    private suspend fun viewSettings(event: SlashCommandInteractionEvent) {
        val settings = DatabaseHandler.getSettings(event.guild!!.id)
        val marketplaceCooldown = settings.marketplaceCooldown.seconds
        val lockdownRole = settings.lockdownRole?.let { event.guild!!.getRoleById(it) }

        event.replyEmbeds(
            EmbedFactory.default(
                """
                | :gear: Settings
                | 
                | ${getNullIcon(settings.logChannel)} Log Channel: ${settings.logChannel?.let { "<#$it>" } ?: "Not set"}
                | ${getNullIcon(settings.marketplaceChannel)} Marketplace Channel: ${settings.marketplaceChannel?.let { "<#$it>" } ?: "Not set"}
                | ${EmojiResolver.checkmark().formatted} Marketplace Cooldown: ${DurationResolver.pretty(marketplaceCooldown)}
                | ${EmojiResolver.checkmark().formatted} Lockdown Role: ${lockdownRole?.asMention ?: "@everyone"}
                """.trimMargin()
            ).build()
        ).setEphemeral(true).queue()
    }

    private suspend fun setMarketplaceChannel(event: SlashCommandInteractionEvent) {
        val channel = event.getOption("channel")?.asChannel ?: error("Channel not provided")
        if (channel.type != ChannelType.TEXT) error("Channel must be a text channel")

        iron.prepare(
            "UPDATE settings SET marketplace_channel = ? WHERE guild_id = ?",
            channel.id,
            event.guild!!.id
        )

        GuildLogger.of(event.guild!!).log(
            "The marketplace channel was set to ${channel.asMention} by ${event.user.asMention}",
            ":identification_card: User: ${event.member?.asMention} *(${event.user.name} - ${event.user.id})*",
            ":package: Channel: ${channel.asMention} *(${channel.name} - ${channel.id})*"
        ).withContext(event).titled("Marketplace Channel Changed").post()

        event.replyEmbeds(
            EmbedFactory.success("Successfully set the marketplace channel to ${channel.asMention}", event.guild!!)
                .build()
        ).setEphemeral(true).queue()
    }

    private suspend fun setMarketplaceCooldown(event: SlashCommandInteractionEvent) {
        val cooldown = event.getOption("cooldown")?.asString ?: error("Cooldown not provided")
        val duration = DurationResolver.from(cooldown) ?: error("Invalid cooldown provided")

        iron.prepare(
            "UPDATE settings SET marketplace_cooldown = ? WHERE guild_id = ?",
            duration.toSeconds(),
            event.guild!!.id
        )

        GuildLogger.of(event.guild!!).log(
            "The marketplace cooldown was set to `${DurationResolver.pretty(duration)}` by ${event.user.asMention}",
            ":identification_card: User: ${event.member?.asMention} *(${event.user.name} - ${event.user.id})*",
            ":stopwatch: Cooldown was set to `${DurationResolver.pretty(duration)}`"
        ).withContext(event).titled("Marketplace Cooldown Changed").post()

        event.replyEmbeds(
            EmbedFactory.success("Successfully set the marketplace cooldown to `${DurationResolver.pretty(duration)}`", event.guild!!)
                .build()
        ).setEphemeral(true).queue()
    }

    private suspend fun setLockdownRole(event: SlashCommandInteractionEvent) {
        val role = event.getOption("role")?.asRole

        iron.prepare(
            "UPDATE settings SET lockdown_role = ? WHERE guild_id = ?",
            role?.id,
            event.guild!!.id
        )

        GuildLogger.of(event.guild!!).log(
            "The lockdown role was set to ${role?.asMention ?: "@everyone"} by ${event.user.asMention}",
            ":identification_card: User: ${event.member?.asMention} *(${event.user.name} - ${event.user.id})*",
            ":label: Role: ${role?.asMention ?: "@everyone"} *(${role?.name} - ${role?.id})*"
        ).withContext(event).titled("Lockdown Role Changed").post()

        event.replyEmbeds(
            EmbedFactory.success("Successfully set the lockdown role to ${role?.asMention ?: "@everyone"}", event.guild!!)
                .build()
        ).setEphemeral(true).queue()
    }

    private fun setLockdownChannels(event: SlashCommandInteractionEvent) {
        val channels = Lockdown.getLockdownChannels(event.guild!!.id)

        event.replyEmbeds(
            EmbedFactory.default("Which channels do you want to restrict the lockdown command to?")
                .build()
        ).addComponents(
            ActionRow.of(
                EntitySelectMenu("minehut:settings:lockdown:channels", listOf(SelectTarget.CHANNEL)) {
                    setChannelTypes(ChannelType.TEXT, ChannelType.FORUM)
                    setMaxValues(25)
                    setDefaultValues(channels.map {
                        EntitySelectMenu.DefaultValue.channel(it)
                    })
                }
            )
        ).setEphemeral(true).queue()
    }

    override suspend fun setup(bot: JDA) {
        bot.listener<EntitySelectInteractionEvent> {
            if (it.componentId != "minehut:settings:lockdown:channels") return@listener
            val guild = it.guild ?: return@listener

            if (it.message.interactionMetadata?.user?.idLong != it.user.idLong) {
                it.replyEmbeds(EmbedFactory.error("Only the person who opened this menu can use it.", guild).build())
                    .setEphemeral(true).queue()
                return@listener
            }

            if (it.message.timeCreated.isBefore(OffsetDateTime.now().minusMinutes(15))) {
                it.editMessageEmbeds(EmbedFactory.error("This menu expired, run the command again.", guild).build())
                    .setComponents().queue()
                return@listener
            }

            val channels = it.values.map { value -> value.id }
            Lockdown.setChannels(guild.id, channels)

            it.editMessageEmbeds(
                EmbedFactory.default("Successfully updated the lockdown channels!")
                    .build()
            ).setComponents().queue()

            GuildLogger.of(guild).log(
                "The lockdown channels were modified by ${it.user.asMention}",
                ":identification_card: User: ${it.member?.asMention} *(${it.user.name} - ${it.user.id})*",
                ":package: Channel IDs: ${channels.joinToString(", ")} *(${it.values.joinToString(", ") { value -> value.asMention }})*"
            ).withContext(it).titled("Lockdown Channels Modified").post()
        }
    }

    private suspend fun setLogChannel(event: SlashCommandInteractionEvent) {
        val channel = event.getOption("channel")?.asChannel ?: error("Channel not provided")
        if (channel.type != ChannelType.TEXT) error("Channel must be a text channel")
        val guild = event.guild ?: return

        iron.prepare(
            "UPDATE settings SET log_channel = ? WHERE guild_id = ?",
            channel.id,
            guild.id
        )

        GuildLogger.of(guild).log(
            "The log channel was set to ${channel.asMention} by ${event.user.asMention}",
            ":identification_card: User: ${event.member?.asMention} *(${event.user.name} - ${event.user.id})*",
            ":package: Channel: ${channel.asMention} *(${channel.name} - ${channel.id})*"
        ).withContext(event).titled("Log Channel Changed").post()

        event.replyEmbeds(
            EmbedFactory.default("Successfully set the log channel to ${channel.asMention}!")
                .build()
        ).setEphemeral(true).queue()
    }

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        DatabaseHandler.createIfNotExists(event.guild!!.id)

        // Router
        when(event.subcommandGroup) {
            "channel" -> when(event.subcommandName) {
                "marketplace" -> setMarketplaceChannel(event)
                else -> error("Subcommand not found")
            }
            "cooldown" -> when(event.subcommandName) {
                "marketplace" -> setMarketplaceCooldown(event)
                else -> error("Subcommand not found")
            }
            "lockdown" -> when(event.subcommandName) {
                "role" -> setLockdownRole(event)
                "channels" -> setLockdownChannels(event)
                else -> error("Subcommand not found")
            }
            "log" -> when(event.subcommandName) {
                "channel" -> setLogChannel(event)
                else -> error("Subcommand not found")
            }
            else -> when(event.subcommandName) {
                "view" -> viewSettings(event)
                else -> error("Subcommand not found")
            }
        }
    }

}
