package me.santio.minehututils.commands.impl

import com.google.auto.service.AutoService
import dev.minn.jda.ktx.interactions.commands.Command
import dev.minn.jda.ktx.interactions.commands.Option
import dev.minn.jda.ktx.interactions.commands.Subcommand
import dev.minn.jda.ktx.interactions.commands.SubcommandGroup
import me.santio.minehututils.commands.SlashCommand
import me.santio.minehututils.database.models.Settings
import me.santio.minehututils.factories.EmbedFactory
import me.santio.minehututils.iron
import me.santio.minehututils.resolvers.DurationResolver
import me.santio.minehututils.resolvers.EmojiResolver
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import kotlin.time.Duration.Companion.seconds

@AutoService(SlashCommand::class)
class SettingsCommand: SlashCommand {

    override fun getData(): CommandData {
        return Command("settings", "Manage specific settings for the bot") {
            defaultPermissions = DefaultMemberPermissions.enabledFor(Permission.MANAGE_CHANNEL)
            isGuildOnly = true

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
                }
            )
        }
    }

    private fun getNullIcon(value: Any?): String {
        return if (value == null) EmojiResolver.crossmark().formatted else EmojiResolver.checkmark().formatted
    }

    private suspend fun viewSettings(event: SlashCommandInteractionEvent) {
        val settings = iron.prepare("SELECT * FROM settings").single<Settings>()

        val marketplaceCooldown = settings.marketplaceCooldown.seconds

        event.replyEmbeds(
            EmbedFactory.default(
                """
                | :gear: Settings
                | 
                | ${getNullIcon(settings.marketplaceChannel)} Marketplace Channel: ${settings.marketplaceChannel?.let { "<#$it>" } ?: "Not set"}
                | ${EmojiResolver.checkmark().formatted} Marketplace Cooldown: ${DurationResolver.pretty(marketplaceCooldown)}
                """.trimMargin()
            ).build()
        ).queue()
    }

    private suspend fun setMarketplaceChannel(event: SlashCommandInteractionEvent) {
        val channel = event.getOption("channel")?.asChannel ?: error("Channel not provided")
        if (channel.type != ChannelType.TEXT) error("Channel must be a text channel", true)

        iron.prepare(
            "UPDATE settings SET marketplace_channel = ? WHERE id = ?",
            channel.id,
            1
        )

        event.replyEmbeds(
            EmbedFactory.success("Successfully set the marketplace channel to ${channel.asMention}", event.guild!!)
                .build()
        ).queue()
    }

    private suspend fun setMarketplaceCooldown(event: SlashCommandInteractionEvent) {
        val cooldown = event.getOption("cooldown")?.asString ?: error("Cooldown not provided")
        val duration = DurationResolver.from(cooldown) ?: error("Invalid cooldown provided", true)

        iron.prepare(
            "UPDATE settings SET marketplace_cooldown = ? WHERE id = 1",
            duration.toSeconds()
        )

        event.replyEmbeds(
            EmbedFactory.success("Successfully set the marketplace cooldown to `${DurationResolver.pretty(duration)}`", event.guild!!)
                .build()
        ).queue()
    }

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        // Router
        when(event.subcommandGroup) {
            "channel" -> when(event.subcommandName) {
                "marketplace" -> setMarketplaceChannel(event)
                else -> error("Subcommand not found", true)
            }
            "cooldown" -> when(event.subcommandName) {
                "marketplace" -> setMarketplaceCooldown(event)
                else -> error("Subcommand not found", true)
            }
            else -> when(event.subcommandName) {
                "view" -> viewSettings(event)
                else -> error("Subcommand not found", true)
            }
        }
    }

}
