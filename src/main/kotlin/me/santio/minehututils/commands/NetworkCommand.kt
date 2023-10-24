package me.santio.minehututils.commands

import me.santio.coffee.common.annotations.Command
import me.santio.coffee.jda.annotations.Description
import me.santio.coffee.jda.annotations.Permission
import me.santio.minehututils.ext.formatted
import me.santio.minehututils.ext.reply
import me.santio.minehututils.factories.EmbedFactory
import me.santio.minehututils.minehut.Minehut
import me.santio.minehututils.resolvers.ChannelResolver
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import javax.swing.text.html.HTML.Tag.P
import kotlin.math.ceil

@Command
@Description("View statistics about Minehut")
class NetworkCommand {

    fun main(e: SlashCommandInteractionEvent) {
        val playerDist = Minehut.players() ?: run {
            e.reply(EmbedFactory.error("Failed to fetch player statistics", e.guild)).setEphemeral(true).queue()
            return
        }

        val status = Minehut.network() ?: run {
            e.reply(EmbedFactory.error("Failed to fetch network statistics", e.guild)).setEphemeral(true).queue()
            return
        }

        e.reply(EmbedFactory.default(
            """
            | :bar_chart: **Network Stats**
            | 
            | **Players**: ${status.playerCount.formatted()}
            | → Java: ${playerDist.javaTotal.formatted()} (Lobby: ${playerDist.javaLobby.formatted()}, Servers: ${playerDist.javaPlayerServer.formatted()})
            | → Bedrock: ${playerDist.bedrockTotal.formatted()} (Lobby: ${playerDist.bedrockLobby.formatted()}, Servers: ${playerDist.bedrockPlayerServer.formatted()})
            |
            | **Servers**: ${status.serverCount}/${status.serverMax}
            | **RAM**: ${ceil(status.ramCount / 1000.0).toInt()}GB/${status.ramMax}GB
            |
            | *View player statistics at [Minehut Track](https://track.minehut.com)*
            """.trimMargin()
        )).queue()
    }

}