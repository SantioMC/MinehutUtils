package me.santio.minehututils.commands.impl

import com.google.auto.service.AutoService
import dev.minn.jda.ktx.interactions.commands.Command
import dev.minn.jda.ktx.interactions.commands.Option
import dev.minn.jda.ktx.interactions.commands.Subcommand
import me.santio.minehututils.commands.SlashCommand
import me.santio.minehututils.coroutines.await
import me.santio.minehututils.factories.EmbedFactory
import me.santio.minehututils.tags.TagManager
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.commands.build.CommandData

@AutoService(SlashCommand::class)
class TagsCommand : SlashCommand {

    override fun getData(): CommandData {
        return Command("tags", "See and send tags") {
            setContexts(InteractionContextType.GUILD)

            addSubcommands(
                Subcommand("list", "List all tags"),
                Subcommand("send", "Send a tag") {
                    addOptions(
                        Option<String>("id", "The tag to send", true, true),
                        Option<User>("user", "The user to mention with the tag")
                    )
                }
            )
        }
    }

    override val filtersChoices = true

    override suspend fun autoComplete(event: CommandAutoCompleteInteractionEvent): List<Choice> {
        val guild = event.guild ?: return emptyList()
        if (event.focusedOption.name != "id") return emptyList()

        return TagManager.choices(guild.id, event.focusedOption.value)
    }

    private fun listTags(event: SlashCommandInteractionEvent) {
        val tags = TagManager.getTags(event.guild!!.id).sortedBy { it.id }

        event.replyEmbeds(
            EmbedFactory.default(
                """
                | :label: Tags
                |
                | ${tags.joinToString("\n") {
                    var message = "[${it.id}] ${it.name}"

                    if (it.guildId == null) message += " (Global)"
                    if (it.uses == 0) message += " (Never used)"

                    message
                }}
                """.trimMargin()
            ).build()
        ).setEphemeral(true).queue()
    }

    private suspend fun sendTag(event: SlashCommandInteractionEvent) {
        val id = event.getOption("id")?.asString ?: error("Tag id not provided")
        val tag = TagManager.get(id.toIntOrNull() ?: error("Tag not found"), event.guild!!.id)
            ?: error("Tag not found")

        event.reply(tag.toMessage(event.user, event.getOption("user")?.asUser)).await()
        TagManager.addUse(tag)
    }

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        when (event.subcommandName) {
            "list" -> listTags(event)
            "send" -> sendTag(event)
            else -> error("Subcommand not found", true)
        }
    }

}
