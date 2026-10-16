package me.santio.minehututils.commands

import me.santio.minehututils.commands.exceptions.CommandError
import me.santio.minehututils.factories.EmbedFactory
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.requests.ErrorResponse
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.channels.UnresolvedAddressException
import java.util.concurrent.ConcurrentHashMap

object CommandManager {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val commands = ConcurrentHashMap<String, SlashCommand>()
    private val permissionErrors = setOf(ErrorResponse.MISSING_PERMISSIONS, ErrorResponse.MISSING_ACCESS)

    fun collect(): List<CommandData> {
        return commands.values.map { it.getData() }
    }

    fun register(vararg commands: SlashCommand) {
        commands.forEach { this.commands[it.getData().name] = it }
    }

    suspend fun execute(event: SlashCommandInteractionEvent) {
        val command = commands[event.name]

        if (command == null) {
            event.replyEmbeds(EmbedFactory.error("Command not found", event.guild).build()).queue()
            return
        }

        kotlin.runCatching {
            command.execute(event)
        }.onFailure { result ->
            when {
                result is CommandError -> {
                    replyError(event, EmbedFactory.error(result.message, event.guild).build(), result.ephemeral)
                }

                result is ErrorResponseException && result.errorResponse == ErrorResponse.UNKNOWN_INTERACTION -> {
                    logger.warn("The interaction for '{}' expired before it could be acknowledged", event.commandString)
                }

                result is InsufficientPermissionException -> {
                    logger.info("Missing the {} permission while running '{}'", result.permission.getName(), event.commandString)
                    replyError(event, EmbedFactory.error(
                        "I'm missing the **${result.permission.getName()}** permission needed to do that here.",
                        event.guild
                    ).build())
                }

                result is ErrorResponseException && result.errorResponse in permissionErrors -> {
                    logger.info("Discord denied an action while running '{}': {}", event.commandString, result.message)
                    replyError(event, EmbedFactory.error("I don't have permission to do that here.", event.guild).build())
                }

                result.isNetworkFailure() -> {
                    logger.warn("A request failed while running '{}': {}", event.commandString, result.toString())
                    replyError(event, EmbedFactory.error(
                        "A network request failed, please try again shortly.",
                        event.guild
                    ).build())
                }

                else -> {
                    logger.error("An error occurred while executing the command", result)
                    replyError(event, EmbedFactory.exception("An error occurred while executing the command", event.guild, result).build())
                }
            }
        }
    }

    /**
     * Sends an error to the user, using a followup if the command already acknowledged the interaction
     * (for example after deferring while waiting on the Minehut API).
     */
    private fun replyError(event: SlashCommandInteractionEvent, embed: MessageEmbed, ephemeral: Boolean = true) {
        if (event.isAcknowledged) {
            event.hook.sendMessageEmbeds(embed).setEphemeral(ephemeral).queue()
        } else {
            event.replyEmbeds(embed).setEphemeral(ephemeral).queue()
        }
    }

    // Ktor's request and connect timeouts are IOExceptions, a failed DNS lookup surfaces as UnresolvedAddressException
    private fun Throwable.isNetworkFailure(): Boolean {
        return this is IOException || this is UnresolvedAddressException
    }

    suspend fun autoComplete(event: CommandAutoCompleteInteractionEvent) {
        val command = commands[event.name]
            ?: return

        kotlin.runCatching {
            val choices = command.autoComplete(event)
                .filter { command.filtersChoices || it.name.contains(event.focusedOption.value, true) }
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
