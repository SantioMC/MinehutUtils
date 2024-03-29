package me.santio.minehututils.commands

import me.santio.coffee.common.annotations.Command
import me.santio.coffee.jda.annotations.Description
import me.santio.minehututils.ext.formatted
import me.santio.minehututils.ext.reply
import me.santio.minehututils.ext.toTime
import me.santio.minehututils.factories.EmbedFactory
import me.santio.minehututils.minehut.Minehut
import me.santio.minehututils.minehut.api.ServerModel
import me.santio.minehututils.resolvers.DurationResolver
import me.santio.minehututils.resolvers.EmojiResolver
import me.santio.minehututils.resolvers.MOTDResolver
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import java.time.Duration
import kotlin.math.round

@Command
@Description("Get the status of Minehut")
class ServerCommand {

    companion object {
        fun buildServerEmbed(guild: Guild, server: ServerModel): EmbedBuilder {
            val motd = MOTDResolver.toAnsi(server.motd.replace("`", "'"))
            val check = EmojiResolver.find(guild, "yes", EmojiResolver.checkmark())!!.formatted
            val cross = EmojiResolver.find(guild, "no", EmojiResolver.crossmark())!!.formatted
            val status = if (server.online) "online" else "offline"

            val timeLimit = when {
                server.hasDailyLimit && server.outOfTime -> "Server is out of time"
                server.hasDailyLimit -> "${DurationResolver.pretty(Duration.ofMillis(server.timeRemaining))} left"
                else -> null
            }

            return EmbedFactory.default(
                """ 
                ${if (server.suspended) "| :warning: This server is currently suspended!" else ""}
                | ```ansi
                | $motd```
                | :chart_with_upwards_trend: **Players:** ${server.playerCount.formatted()} *(${server.joins.formatted()} total joins)*
                | :calendar: **Created:** ${server.createdAt.toTime()}
                | :file_folder: **Categories:** ${server.categories.joinToString(", ").ifEmpty { "None" }}
                ${if (timeLimit != null) 
                    ":stopwatch: **Daily Time**: $timeLimit *(Resets <t:${Minehut.getDailyTimeReset()}:R>)*\n" +
                    "[*Want to remove the daily uptime limit?*](https://app.minehut.com/shop/plans)"
                else ""}
                """.trimMargin()
            )
                .setTitle(server.name + (
                    if (server.proxy) " (Server Network)" else ""
                ))
                .addField(
                    "Server Status",
                    """
                    | Server is `$status` ${if (server.online) check else cross}
                    | Started ${server.lastOnline.toTime()}
                    | Created ${server.createdAt.toTime()}
                    """.trimMargin(),
                    true
                )
                .addBlankField(true)
                .addField(
                    "Server Information",
                    """
                    | Owned by `${server.ownerUsername}`
                    | The plan is `${server.plan.name.lowercase()}` *(${round(server.creditsPerDay).toInt()} credits/d)*
                    | Server is using **${server.serverVersionType.lowercase()}**
                    """.trimMargin(),
                    true
                ).setFooter("Server ID: ${server.id}")
        }
    }

    fun main(
        e: SlashCommandInteractionEvent,
        server: ServerModel
    ) {
        val guild = e.guild ?: return
        e.reply(buildServerEmbed(guild, server)).queue()
    }

}