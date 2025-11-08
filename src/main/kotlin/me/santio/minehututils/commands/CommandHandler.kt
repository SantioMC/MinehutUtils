package me.santio.minehututils.commands

import me.santio.minehututils.commands.exceptions.CommandError
import me.santio.minehututils.factories.EmbedFactory
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import org.slf4j.LoggerFactory

object CommandManager {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val commands = mutableListOf<SlashCommand>()
    private val commandMap = mutableMapOf<String, SlashCommand>()

    fun collect(): List<CommandData> {
        return commands.map { it.getData() }
    }

    fun register(vararg commands: SlashCommand) {
        commands.forEach { 
            this.commands.add(it)
            this.commandMap[it.getData().name] = it
        }
    }

    suspend fun execute(event: SlashCommandInteractionEvent) {
        val command = commandMap[event.name]

        if (command == null) {
            event.replyEmbeds(EmbedFactory.error("Command not found", event.guild).build()).queue()
            return
        }

        kotlin.runCatching {
            command.execute(event)
        }.onFailure { result ->
            when(result) {
                is CommandError -> {
                    event.replyEmbeds(EmbedFactory.error(result.message, event.guild).build())
                        .setEphemeral(result.ephemeral).queue()
                }

                else -> {
                    logger.error("An error occurred while executing the command", result)

                    event.replyEmbeds(EmbedFactory.exception("An error occurred while executing the command", event.guild, result).build())
                        .setEphemeral(true).queue()
                }
            }
        }
    }

    suspend fun autoComplete(event: CommandAutoCompleteInteractionEvent) {
        val command = commandMap[event.name]
            ?: return

        kotlin.runCatching {
            val choices = command.autoComplete(event)
                .filter { it.name.contains(event.focusedOption.value, true) }
                .take(OptionData.MAX_CHOICES)

            event.replyChoices(choices).queue()
        }.onFailure { result ->
            when(result) {
                is CommandError -> {
                    logger.warn("Failed to auto-complete command", result)
                    event.replyChoices(emptyList()).queue()
                }

                else -> {
                    logger.error("An error occurred while auto-completing the command", result)
                    event.replyChoices(emptyList()).queue()
                }
            }
        }
    }

}
