package me.santio.minehututils.commands.impl

import com.google.auto.service.AutoService
import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.events.onStringSelect
import dev.minn.jda.ktx.interactions.commands.Command
import dev.minn.jda.ktx.interactions.components.Modal
import dev.minn.jda.ktx.interactions.components.StringSelectMenu
import dev.minn.jda.ktx.interactions.components.option
import me.santio.minehututils.bot
import me.santio.minehututils.commands.SlashCommand
import me.santio.minehututils.cooldown.Cooldown
import me.santio.minehututils.cooldown.CooldownManager
import me.santio.minehututils.database.DatabaseHandler
import me.santio.minehututils.database.models.Settings
import me.santio.minehututils.factories.EmbedFactory
import me.santio.minehututils.resolvers.AutoModResolver
import me.santio.minehututils.resolvers.DurationResolver.discord
import me.santio.minehututils.resolvers.EmojiResolver
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.components.selections.StringSelectInteraction
import net.dv8tion.jda.api.utils.MarkdownSanitizer
import java.util.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@AutoService(SlashCommand::class)
class MarketplaceCommand : SlashCommand {

    override fun getData(): CommandData {
        return Command("marketplace", "Request or offer services") {
            isGuildOnly = true
        }
    }

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        val settings = DatabaseHandler.getSettings(event.guild!!.id)
        val id = UUID.randomUUID().toString()

        if (settings.marketplaceChannel == null || settings.marketplaceCooldown < 0L) {
            event.replyEmbeds(
                EmbedFactory.error(
                    "The marketplace channel is not currently configured, come back later!",
                    event.guild!!
                ).build()
            )
                .setEphemeral(true)
                .queue()
            return
        }

        event.replyEmbeds(
            EmbedFactory.default("Are you looking to offer or request?")
                .build()
        ).addActionRow(
            StringSelectMenu("minehut:marketplace:type:$id", "Select an option") {
                option("Offering", "offer", emoji = Emoji.fromFormatted("\uD83D\uDCE2"))
                option("Requesting", "request", emoji = Emoji.fromFormatted("📝"))
            }
        ).setEphemeral(true).queue()

        bot.onStringSelect("minehut:marketplace:type:$id", timeout = 30.seconds) {
            cancel()

            // Check if the user is on cooldown
            val selected = it.selectedOptions.firstOrNull()?.value ?: error("No option selected")
            val cooldown = CooldownManager.get(event.user.id, Cooldown.getMarketplaceType(selected))
            if (cooldown != null) {
                it.replyEmbeds(
                    EmbedFactory.error(
                        "You are currently on cooldown, try again ${cooldown.timeLeft().discord(true)}",
                        event.guild!!
                    ).build()
                ).setEphemeral(true).queue()
                return@onStringSelect
            }

            handlePosting(it, selected, settings)
        }
    }

    private fun handlePosting(e: StringSelectInteraction, type: String, settings: Settings) {
        if (e.isAcknowledged) return
        val id = UUID.randomUUID().toString()

        e.replyModal(Modal("minehut:marketplace:modal:$id", "Customize your listing") {
            short("minehut:listing:title", "The title of your listing")
            paragraph("minehut:listing:description", "The description of your listing")
        }).queue()

        bot.listener<ModalInteractionEvent>(timeout = 15.minutes) {
            if (it.modalId != "minehut:marketplace:modal:$id") return@listener
            cancel()

            val title =
                it.values.firstOrNull { it.id == "minehut:listing:title" }?.asString ?: error("No title provided")
            val description = it.values.firstOrNull { it.id == "minehut:listing:description" }?.asString
                ?: error("No description provided")

            // Run in auto-mod
            AutoModResolver.parse(it.guild!!, it.member!!, e, title, description).thenAccept { passes ->
                if (!passes) {
                    it.replyEmbeds(
                        EmbedFactory.error(
                            "Your message was caught by the filter and the request has been discarded.",
                            it.guild!!
                        ).build()
                    ).setEphemeral(true).queue()
                    return@thenAccept
                }
            }

            postListing(type, it, settings, title, description)
        }
    }

    private fun postListing(
        type: String,
        event: ModalInteractionEvent,
        settings: Settings,
        title: String,
        description: String
    ) {
        val channel = bot.getTextChannelById(settings.marketplaceChannel!!) ?: run {
            event.replyEmbeds(
                EmbedFactory.error(
                    "Failed to find the marketplace channel, was it deleted?",
                    event.guild!!
                ).build()
            )
                .setEphemeral(true).queue()
            return
        }

        val kind = when (type) {
            "offer" -> "Offering"
            "request" -> "Requesting"
            else -> error("Invalid type provided")
        }

        val embed = EmbedFactory.default(
            """
            | ${EmojiResolver.find(event.guild, "minehut")?.formatted ?: ""} **$kind | ${
                MarkdownSanitizer.sanitize(title.replace("*", ""))
                    .ifEmpty { "Untitled" }
            }**
            |
            | $description
            """.trimMargin()) {

            it.setFooter(
                "Listing made by ${event.user.name} (${event.user.id})",
                event.user.avatarUrl
            )

            it.setColor(getEmbedColor(type))
        }

        channel.sendMessage("Listing posted by ${event.user.asMention}")
            .addEmbeds(embed.build())
            .queue {
                event.replyEmbeds(
                    EmbedFactory.default(
                        """
                    | **Success** ${EmojiResolver.yes(event.guild)?.formatted ?: ""}
                    |
                    | You have successfully posted your marketplace listing!
                    | Check it out! :point_right: ${it.jumpUrl}
                    """.trimMargin()
                    ).build()
                ).setEphemeral(true).queue()
            }

        CooldownManager.set(event.user.id, Cooldown.getMarketplaceType(type), settings.marketplaceCooldown.seconds)
    }

    private fun getEmbedColor(type: String): Int {
        return when (type) {
            "offer" -> 0xff057a
            "request" -> 0xA160B5
            else -> error("Invalid type provided")
        }
    }

}
