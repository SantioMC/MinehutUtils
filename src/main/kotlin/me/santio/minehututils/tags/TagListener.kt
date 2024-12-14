package me.santio.minehututils.tags

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.santio.minehututils.coroutines.exceptionHandler
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.util.*
import kotlin.concurrent.schedule

object TagListener: ListenerAdapter() {

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val cooldownReaction = Emoji.fromUnicode("⌛")
    private val timer = Timer()
    private val recentlySent = mutableListOf<String>()

    override fun onMessageReceived(event: MessageReceivedEvent) {
        val tags = TagManager.getTags(event.guild.id)

        val tag = tags.firstOrNull { it.isIncluded(event.message.contentRaw) }
            ?: return

        val id = "${event.channel.id}-${tag.id}"
        if (recentlySent.contains(id)) {
            event.message.addReaction(cooldownReaction).queue()
            return
        }

        recentlySent.add(id)

        try {
            tag.send(event.message)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        coroutineScope.launch(exceptionHandler) {
            TagManager.addUse(tag)
        }

        timer.schedule(10000) {
            recentlySent.remove(id)
        }
    }

}
