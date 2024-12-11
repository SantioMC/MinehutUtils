package me.santio.minehututils.tags

import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object TagListener: ListenerAdapter() {

    private val cooldownReaction = Emoji.fromUnicode("⌛")
    private val executor = Executors.newSingleThreadScheduledExecutor()
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

        executor.schedule({
            recentlySent.remove(id)
        }, 10, TimeUnit.SECONDS)
    }

}
