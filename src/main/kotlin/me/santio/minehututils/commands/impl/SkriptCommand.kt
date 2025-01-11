package me.santio.minehututils.commands.impl

import com.google.auto.service.AutoService
import dev.minn.jda.ktx.interactions.commands.Command
import dev.minn.jda.ktx.interactions.commands.Option
import me.santio.minehututils.commands.SlashCommand
import me.santio.minehututils.skript.Skript
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.build.CommandData

@AutoService(SlashCommand::class)
class SkriptCommand : SlashCommand {

    override fun getData(): CommandData {
        return Command("skript", "Search the Skript documentation") {
            isGuildOnly = true

            addOptions(
                Option<String>("query", "The query to search for", true, autocomplete = true)
            )
        }
    }

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        val query = event.getOption("query")?.asString ?: error("No query was provided")

        val results = Skript.search(query)
        if (results.isEmpty()) {
            event.reply("No results found for `$query`").setEphemeral(true).queue()
            return
        }

        val result = results.first()
        result.send(event)
    }

    override suspend fun autoComplete(event: CommandAutoCompleteInteractionEvent): List<Command.Choice> {
        val query = event.getOption("query")?.asString ?: return emptyList()

        return Skript.search(query)
            .filter { it.title.contains(query, ignoreCase = true) }
            .map { Command.Choice(it.title, it.title) }
    }

}
