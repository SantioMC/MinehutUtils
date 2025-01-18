package me.santio.minehututils.commands

import me.santio.minehututils.commands.exceptions.CommandError
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.build.CommandData

interface SlashCommand {

    fun getData(): CommandData

    /**
     * Called when the command is setup, this is only ever called once and allows for
     * registration of event listeners and other setup tasks.
     * @param bot The bot instance
     */
    suspend fun setup(bot: JDA) {}

    /**
     * Called when the command is executed, this is called every time the command is used.
     * @param event The event that triggered the command
     */
    suspend fun execute(event: SlashCommandInteractionEvent)

    /**
     * Called when the command is auto-completed, this is called every time the user types in the value
     * of a command option, this is debounced to prevent spam.
     * @param event The event that triggered the command
     * @return A list of choices to show to the user
     */
    suspend fun autoComplete(event: CommandAutoCompleteInteractionEvent): List<Command.Choice> {
        return emptyList()
    }

    fun error(message: String, ephemeral: Boolean = true): Nothing {
        throw CommandError(message, ephemeral)
    }

}
