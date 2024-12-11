package me.santio.minehututils.tags

import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

object TagListener: ListenerAdapter() {

    override fun onMessageReceived(event: MessageReceivedEvent) {
        val tags = TagManager.all()

        val tag = tags.firstOrNull { it.isIncluded(event.message.contentRaw) }
            ?: return

        tag.send(event.message)
    }

}
