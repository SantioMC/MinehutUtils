package me.santio.minehututils.commands.impl

import com.google.auto.service.AutoService
import dev.minn.jda.ktx.interactions.commands.Command
import me.santio.minehututils.commands.SlashCommand
import me.santio.minehututils.ext.reply
import me.santio.minehututils.factories.EmbedFactory
import me.santio.minehututils.minehut.Minehut
import me.santio.minehututils.minehut.Service
import me.santio.minehututils.resolvers.ChannelResolver
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.CommandData

@AutoService(SlashCommand::class)
class StatusCommand : SlashCommand {

    override fun getData(): CommandData {
        return Command("status", "Get the status of Minehut") {
            isGuildOnly = true
        }
    }

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        val status = Minehut.status()

        event.reply(
            EmbedFactory.default(
                """
            | :chart_with_upwards_trend: **Minehut Status**
            | 
            | **Minehut Proxy**: ${status[Service.PROXY]}
            | **Minehut Java**: ${status[Service.JAVA]}
            | **Minehut Bedrock**: ${status[Service.BEDROCK]}
            | **Minehut API**: ${status[Service.API]}
            |
            | *This information is automatic, please refer*
            | *to ${ChannelResolver.getMinehutName("status-and-changelog")} for status updates*
            """.trimMargin()
            )
        ).queue()
    }

}
