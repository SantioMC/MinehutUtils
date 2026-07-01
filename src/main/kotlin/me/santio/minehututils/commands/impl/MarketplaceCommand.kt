package me.santio.minehututils.commands.impl

import com.google.auto.service.AutoService
import dev.minn.jda.ktx.events.onStringSelect
import dev.minn.jda.ktx.interactions.commands.Command
import me.santio.minehututils.bot
import me.santio.minehututils.commands.SlashCommand
import me.santio.minehututils.cooldown.Cooldown
import me.santio.minehututils.cooldown.CooldownManager
import me.santio.minehututils.database.DatabaseHandler
import me.santio.minehututils.factories.EmbedFactory
import me.santio.minehututils.marketplace.MarketplaceManager
import me.santio.minehututils.resolvers.DurationResolver.discord
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import java.util.*
import kotlin.time.Duration.Companion.seconds

@AutoService(SlashCommand::class)
class MarketplaceCommand : SlashCommand {

    override fun getData(): CommandData {
        return Command("marketplace", "Request or offer services") {
            setContexts(InteractionContextType.GUILD)
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
        ).addComponents(
            ActionRow.of(
                StringSelectMenu.create("minehut:marketplace:type:$id")
                    .setPlaceholder("Select an option")
                    .addOption("Offering", "offer", Emoji.fromFormatted("\uD83D\uDCE2"))
                    .addOption("Requesting", "request", Emoji.fromFormatted("📝"))
                    .build()
            )
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

            MarketplaceManager.handlePosting(it, selected, settings)
        }
    }

}
