package me.santio.minehututils.tags

import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.util.*
import kotlin.concurrent.schedule

object TagListener: ListenerAdapter() {

    private val cooldownReaction = Emoji.fromUnicode("⌛")
    private val timer = Timer()
    private val recentlySent = mutableListOf<String>()

    override fun onMessageReceived(event: MessageReceivedEvent) {
        val tags = TagManager.all()

        val tag = tags.firstOrNull { it.isIncluded(event.message.contentRaw) }
            ?: return

        val id = "${event.channel.id}-${tag.id}"
        if (recentlySent.contains(id)) {
            event.message.addReaction(cooldownReaction).queue()
            return
        }

        recentlySent.add(id)
        tag.send(event.message)

        timer.schedule(10000) {
            recentlySent.remove(id)
        }
    }

}
