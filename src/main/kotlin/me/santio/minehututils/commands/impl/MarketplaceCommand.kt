package me.santio.minehututils.commands.impl

import com.google.auto.service.AutoService
import dev.minn.jda.ktx.interactions.commands.Command
import dev.minn.jda.ktx.interactions.components.StringSelectMenu
import dev.minn.jda.ktx.interactions.components.option
import me.santio.minehututils.commands.SlashCommand
import me.santio.minehututils.database.DatabaseHandler
import me.santio.minehututils.factories.EmbedFactory
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.build.CommandData

@AutoService(SlashCommand::class)
class MarketplaceCommand : SlashCommand {

    override fun getData(): CommandData {
        return Command("marketplace", "Request or offer services") {
            setContexts(InteractionContextType.GUILD)
        }
    }

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        val settings = DatabaseHandler.getSettings(event.guild!!.id)

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
                StringSelectMenu("minehut:marketplace:type", placeholder = "Select an option") {
                    option("Offering", "offer", emoji = Emoji.fromFormatted("\uD83D\uDCE2"))
                    option("Requesting", "request", emoji = Emoji.fromFormatted("📝"))
                }
            )
        ).setEphemeral(true).queue()
    }

}
