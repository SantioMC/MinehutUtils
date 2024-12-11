package me.santio.minehututils.commands

import me.santio.minehututils.commands.exceptions.CommandError
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.build.CommandData

interface SlashCommand {

    fun getData(): CommandData

    suspend fun execute(event: SlashCommandInteractionEvent)
    suspend fun autoComplete(event: CommandAutoCompleteInteractionEvent): List<Command.Choice> {
        return emptyList()
    }

    fun error(message: String, ephemeral: Boolean = false): Nothing {
        throw CommandError(message, ephemeral)
    }

}
