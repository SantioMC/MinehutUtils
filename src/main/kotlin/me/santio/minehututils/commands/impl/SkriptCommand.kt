package me.santio.minehututils.commands.impl

import com.google.auto.service.AutoService
import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.interactions.commands.Command
import dev.minn.jda.ktx.interactions.commands.Option
import me.santio.minehututils.commands.SlashCommand
import me.santio.minehututils.factories.EmbedFactory
import me.santio.minehututils.skript.Skript
import me.santio.minehututils.skript.Syntax
import me.santio.minehututils.utils.EnvUtils.env
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle

@AutoService(SlashCommand::class)
class SkriptCommand : SlashCommand {

    override fun getData(): CommandData {
        return Command("skript", "Search the SkriptHub documentation") {
            addOptions(
                Option<String>("query", "The query to search for", true, autocomplete = true)
            )
        }
    }

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        val query = event.getOption("query")?.asString ?: error("No query was provided")

        if (env("SKRIPTHUB_KEY") == null) {
            return error("This command is not currently configured, please try again later.")
        }

        val id = query.toLongOrNull()
        val syntax = if (id != null) {
            Skript.get(id)
        } else {
            Skript.get(query)
        } ?: return error("Failed to get syntax for `$query`!")

        var description = syntax.description.split("\n").joinToString("\n") { "> ${it.trim()}" }

        if (syntax.addon != "Skript") {
            description += "\n\n*:jigsaw: This syntax is provided by the ${syntax.addon} addon*"
        }

        description += "\n\n**Syntax**\n```ansi\n${Syntax.skriptExpression(syntax)}\n```"

        event.replyEmbeds(
            EmbedFactory.default(description)
                .setTitle(syntax.title)
                .setUrl(syntax.link)
                .setFooter("Requested by ${event.user.name} (${event.user.id})", null)
                .build()
        ).addActionRow(
            Button.of(
                ButtonStyle.PRIMARY,
                "minehut:skript:example:${syntax.id}",
                "View Example"
            )
        ).queue()
    }

    override suspend fun setup(bot: JDA) {
        bot.listener<ButtonInteractionEvent> {
            if (it.componentId.startsWith("minehut:skript:example:")) {
                val id = it.componentId.substringAfterLast(":").toLongOrNull() ?: return@listener
                val example = Skript.getExample(id)

                if (example == null) {
                    it.replyEmbeds(EmbedFactory.error("Sorry, there were no examples for this syntax!", it.guild).build()).setEphemeral(true).queue()
                    return@listener
                }

                val body = "```ansi\n${Syntax.skriptSyntax(example.exampleCode)}\n```"

                it.replyEmbeds(
                    EmbedFactory.default(body)
                        .setFooter("Requested by ${it.user.name} (${it.user.id})", null)
                        .build()
                ).setEphemeral(true).queue()
            }
        }
    }

    override suspend fun autoComplete(event: CommandAutoCompleteInteractionEvent): List<Command.Choice> {
        val query = event.getOption("query")?.asString ?: return emptyList()

        val id = query.toLongOrNull()
        return if (id != null) {
            Skript.search(id)
                .filter { it.id.toString().startsWith(query) }
                .map { Command.Choice("[${it.id}] ${it.title}", it.id) }
        } else {
            Skript.search(query)
                .filter { it.title.contains(query, ignoreCase = true) }
                .map { Command.Choice(it.title, it.title) }
        }
    }

}
