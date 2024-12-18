package me.santio.minehututils.commands.impl

import com.google.auto.service.AutoService
import dev.minn.jda.ktx.interactions.commands.Command
import me.santio.minehututils.commands.SlashCommand
import me.santio.minehututils.ext.formatted
import me.santio.minehututils.ext.reply
import me.santio.minehututils.ext.toTime
import me.santio.minehututils.factories.EmbedFactory
import me.santio.minehututils.minehut.Minehut
import me.santio.minehututils.minehut.Minehut.server
import me.santio.minehututils.resolvers.EmojiResolver
import me.santio.minehututils.resolvers.MOTDResolver
import me.santio.minehututils.utils.TextHelper.titlecase
import me.santio.sdk.minehut.models.Server
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import kotlin.math.round

@AutoService(SlashCommand::class)
class ServerCommand : SlashCommand {

    suspend fun buildServerEmbed(guild: Guild, server: Server): EmbedBuilder {
        val motd = MOTDResolver.toAnsi(server.motd?.replace("`", "'") ?: "A Minehut production")
        val check = EmojiResolver.find(guild, "yes", EmojiResolver.checkmark())!!.formatted
        val cross = EmojiResolver.find(guild, "no", EmojiResolver.crossmark())!!.formatted
        val status = if (server.online == true) "online" else "offline"

        val plan = server.serverPlan?.lowercase()?.replace("_", " ") ?: "unknown"
        val owner = Minehut.servers().firstOrNull {
            it.staticInfo?.id == server.id
        }?.author ?: "Unknown"

        return EmbedFactory.default(
            """ 
             ${if (server.suspended == true) "| :warning: This server is currently suspended!" else ""}
             | ```ansi
             | $motd```
             | :chart_with_upwards_trend: **Players:** ${server.playerCount?.formatted()} *(${server.joins?.formatted() ?: "0"} total joins)*
             | :calendar: **Created:** ${server.creation?.toTime() ?: "Unknown"}
             | :file_folder: **Categories:** ${
                server.categories?.joinToString(", ")?.takeIf { it.isNotEmpty() } ?: "None"
            }
             """.trimMargin()
        )
            .setTitle(
                server.name + (
                    if (server.proxy == true) " (Server Network)" else ""
                    )
            )
            .addField(
                "Server Status",
                """
                 | Server is `${status.titlecase()}` ${if (server.online == true) check else cross}
                 | Started ${server.lastOnline?.toTime() ?: "Unknown"}
                 | Created ${server.creation?.toTime() ?: "Unknown"}
                 """.trimMargin(),
                true
            )
            .addBlankField(true)
            .addField(
                "Server Information",
                """
                 | Owned by `${owner}`
                 | The plan is `$plan` *(${round(server.creditsPerDay?.toDouble() ?: 0.0).toInt()} credits/d)*
                 | Server is using **${server.serverVersionType?.titlecase() ?: "Unknown"}**
                 """.trimMargin(),
                true
            ).setFooter("Server ID: ${server.id ?: "Unknown"}")
    }

    override fun getData(): CommandData {
        return Command("server", "Get information about a server") {
            isGuildOnly = true
            addOption(OptionType.STRING, "server", "The server to get information about", true, true)
        }
    }

    override suspend fun autoComplete(event: CommandAutoCompleteInteractionEvent): List<Command.Choice> {
        return Minehut.servers()
            .mapNotNull { it.name }
            .map {
                Command.Choice(it, it)
            }
    }

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        val serverId = event.getOption("server")?.asString ?: error("Server not provided")

        val guild = event.guild ?: return
        val data = server(serverId) ?: run {
            event.reply(EmbedFactory.error("Failed to find the server", guild)).queue()
            return
        }

        event.reply(buildServerEmbed(guild, data)).queue()
    }

}
