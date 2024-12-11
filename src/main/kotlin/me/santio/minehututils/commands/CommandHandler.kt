package me.santio.minehututils.commands

import me.santio.minehututils.commands.exceptions.CommandError
import me.santio.minehututils.factories.EmbedFactory
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import org.slf4j.LoggerFactory

object CommandManager {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val commands = mutableListOf<SlashCommand>()

    fun collect(): List<CommandData> {
        return commands.map { it.getData() }
    }

    fun register(vararg commands: SlashCommand) {
        commands.forEach { this.commands.add(it) }
    }

    suspend fun execute(event: SlashCommandInteractionEvent) {
        val command = commands.find { it.getData().name == event.name }
        if (command == null) {
            event.replyEmbeds(EmbedFactory.error("Command not found", event.guild).build()).queue()
            return
        }

        try {
            command.execute(event)
        } catch (e: CommandError) {
            event.replyEmbeds(EmbedFactory.error(e.message, event.guild).build()).setEphemeral(e.ephemeral).queue()
        } catch (e: Exception) {
            event.replyEmbeds(
                EmbedFactory.exception("An error occurred while executing the command", event.guild, e).build()
            ).queue()
        }
    }

    suspend fun autoComplete(event: CommandAutoCompleteInteractionEvent) {
        val command = commands.find { it.getData().name == event.name }
        if (command == null) return

        try {
            val choices = command.autoComplete(event)
                .filter { it.name.startsWith(event.focusedOption.value, true) }
                .take(10)

            event.replyChoices(choices).queue()
        } catch (e: CommandError) {
            logger.warn("Failed to auto-complete command", e)
            event.replyChoices(emptyList()).queue()
        } catch (e: Exception) {
            logger.error("An error occurred while auto-completing the command", e)
            event.replyChoices(emptyList()).queue()
        }
    }

}
