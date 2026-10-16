package me.santio.minehututils.tags

import kotlinx.coroutines.launch
import me.santio.minehututils.coroutines.await
import me.santio.minehututils.coroutines.exceptionHandler
import me.santio.minehututils.factories.EmbedFactory
import me.santio.minehututils.scope
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands

object TagContextMenu : ListenerAdapter() {

    private const val NAME = "Find tag"

    val data: CommandData = Commands.message(NAME).setContexts(InteractionContextType.GUILD)

    override fun onMessageContextInteraction(event: MessageContextInteractionEvent) {
        if (event.name != NAME) return
        val guild = event.guild ?: return
        val target = event.target

        val content = target.contentRaw.trim()
        if (content.isEmpty()) {
            event.replyEmbeds(EmbedFactory.error("That message has no text to find a tag for", guild).build())
                .setEphemeral(true).queue()
            return
        }

        val tag = TagManager.find(guild.id, content) ?: run {
            event.replyEmbeds(EmbedFactory.error("No tag matches that message, try `/tags send` instead", guild).build())
                .setEphemeral(true).queue()
            return
        }

        if (!TagSender.claim(event.guildChannel.id, tag)) {
            event.replyEmbeds(EmbedFactory.error("That tag was just sent here", guild).build())
                .setEphemeral(true).queue()
            return
        }

        if (!TagSender.canReply(event.guildChannel)) {
            scope.launch(exceptionHandler) {
                event.reply(tag.toMessage(event.user, target.author)).await()
                TagManager.addUse(tag)
            }
            return
        }

        event.deferReply(true).queue()
        scope.launch(exceptionHandler) {
            val sent = TagSender.reply(target, tag, event.user, ping = true)
            val embed = if (sent) EmbedFactory.success("Sent the tag `${tag.name}`", guild)
            else EmbedFactory.error("Failed to send the tag, please try again", guild)

            event.hook.editOriginalEmbeds(embed.build()).queue()
        }
    }

}
