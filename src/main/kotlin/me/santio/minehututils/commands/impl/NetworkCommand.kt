package me.santio.minehututils.commands.impl

import com.google.auto.service.AutoService
import dev.minn.jda.ktx.interactions.commands.Command
import me.santio.minehututils.commands.SlashCommand
import me.santio.minehututils.ext.formatted
import me.santio.minehututils.ext.reply
import me.santio.minehututils.factories.EmbedFactory
import me.santio.minehututils.minehut.Minehut
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import kotlin.math.ceil

@AutoService(SlashCommand::class)
class NetworkCommand : SlashCommand {

    override fun getData(): CommandData {
        return Command("network", "View statistics about Minehut") {
            setContexts(InteractionContextType.GUILD)
        }
    }

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        val playerDist = Minehut.players() ?: run {
            event.reply(EmbedFactory.error("Failed to fetch player statistics", event.guild)).setEphemeral(true).queue()
            return
        }

        val status = Minehut.network() ?: run {
            event.reply(EmbedFactory.error("Failed to fetch network statistics", event.guild)).setEphemeral(true)
                .queue()
            return
        }

        event.reply(
            EmbedFactory.default(
                """
            | :bar_chart: **Network Stats**
            | 
            | **Servers**: ${status.serverCount}/${status.serverMax}
            | **Ram**: ${ceil((status.ramCount ?: 0) / 1000.0).toInt()}GB
            |
            | **Players**: ${status.playerCount?.formatted()}
            | → Java: ${playerDist.javaTotal?.formatted()} (Lobby: ${playerDist.javaLobby?.formatted()}, Servers: ${playerDist.javaPlayerServer?.formatted()})
            | → Bedrock: ${playerDist.bedrockTotal?.formatted()} (Lobby: ${playerDist.bedrockLobby?.formatted()}, Servers: ${playerDist.bedrockPlayerServer?.formatted()})
            |
            | *View player statistics at [Minehut Track](https://track.gamersafer.systems/)*
            """.trimMargin()
            )
        ).queue()
    }

}
