package me.santio.minehututils.tags

import kotlinx.coroutines.launch
import me.santio.minehututils.bot
import me.santio.minehututils.coroutines.exceptionHandler
import me.santio.minehututils.scope
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

object TagMentionListener : ListenerAdapter() {

    private val cooldownReaction = Emoji.fromUnicode("⌛")

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (!event.isFromGuild || event.author.isBot || event.isWebhookMessage) return

        val mention = Regex("<@!?${bot.selfUser.id}>")
        val content = event.message.contentRaw
        if (!mention.containsMatchIn(content)) return

        val query = content.replace(mention, "").trim()
        if (query.isEmpty()) return

        val tag = TagManager.find(event.guild.id, query) ?: return

        // Without these the reply would fail, so there's nothing to do in this channel
        val channel = event.guildChannel
        if (!TagSender.canReply(channel)) return

        if (!TagSender.claim(channel.id, tag)) {
            if (event.guild.selfMember.hasPermission(channel, Permission.MESSAGE_ADD_REACTION)) {
                event.message.addReaction(cooldownReaction).queue()
            }
            return
        }

        scope.launch(exceptionHandler) {
            TagSender.reply(event.message, tag, event.author, ping = false)
        }
    }

}
