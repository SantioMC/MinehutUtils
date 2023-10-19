package me.santio.minehututils.commands

import me.santio.coffee.common.annotations.Command
import me.santio.coffee.jda.annotations.Description
import me.santio.minehututils.ext.reply
import me.santio.minehututils.factories.EmbedFactory
import me.santio.minehututils.minehut.Minehut
import me.santio.minehututils.minehut.Service
import me.santio.minehututils.resolvers.ChannelResolver
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

@Command
@Description("Get the status of Minehut")
class StatusCommand {

    fun main(e: SlashCommandInteractionEvent) {
        val status = Minehut.status()

        e.reply(EmbedFactory.default(
            """
            | :chart_with_upwards_trend: **Minehut Status**
            | 
            | **Minehut Proxy**: ${status[Service.PROXY]}
            | **Minehut Java**: ${status[Service.JAVA]}
            | **Minehut Bedrock**: ${status[Service.BEDROCK]}
            | **Minehut API**: ${status[Service.API]}
            |
            | *This information is automatic, please refer to ${ChannelResolver.getMinehutName("announcements")} for status updates*
            """.trimMargin()
        )).queue()
    }

}