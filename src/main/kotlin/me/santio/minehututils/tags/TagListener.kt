package me.santio.minehututils.tags

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.santio.minehututils.coroutines.exceptionHandler
import net.dv8tion.jda.api.entities.Message.MessageFlag
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.concurrent.schedule

object TagListener: ListenerAdapter() {

    private val logger = LoggerFactory.getLogger(TagListener::class.java)
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val cooldownReaction = Emoji.fromUnicode("⌛")
    private val timer = Timer()
    private val recentlySent = mutableListOf<String>()

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (!event.isFromGuild) return
        val tags = TagManager.getTags(event.guild.id)

        val message = event.message
        val tag = tags.firstOrNull { it.isIncluded(message.contentRaw) }
            ?: return

        val id = "${event.channel.id}-${tag.id}"
        if (recentlySent.contains(id)) {
            message.addReaction(cooldownReaction).queue()
            return
        }

        recentlySent.add(id)

        runCatching {
            val silent = message.flags.contains(MessageFlag.NOTIFICATIONS_SUPPRESSED)
            tag.send(message, silent)
        }.onFailure { result ->
            logger.error("Failed to send tag message", result)
        }

        coroutineScope.launch(exceptionHandler) {
            TagManager.addUse(tag)
        }

        timer.schedule(10000) {
            recentlySent.remove(id)
        }
    }

}
