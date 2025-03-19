package me.santio.minehututils.marketplace

import kotlinx.coroutines.launch
import me.santio.minehututils.bot
import me.santio.minehututils.logger.GuildLogger
import me.santio.minehututils.scope
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.message.MessageBulkDeleteEvent
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

object MarketplaceListener : ListenerAdapter() {

    override fun onMessageDelete(event: MessageDeleteEvent) {
        if (!event.isFromGuild) return
        logMarketplaceDelete(event.channel.asTextChannel(), event.messageId)
    }

    override fun onMessageBulkDelete(event: MessageBulkDeleteEvent) {
        event.messageIds.forEach { logMarketplaceDelete(event.channel.asTextChannel(), it) }
    }

    private fun logMarketplaceDelete(channel: TextChannel, messageId: String) {
        val message = MarketplaceManager.getListing(messageId) ?: return

        val title = message.title
        val content = message.content
        val postedBy = message.postedBy
        val postedByUser = bot.getUserById(postedBy)
        val type = message.type
        scope.launch {
            val log = GuildLogger.of(channel.guild).log(
                """
                    :identification_card: User: ${postedByUser?.asMention} *(${postedByUser?.name} - ${postedByUser?.id})*
                    :label: Type: $type
                    :name_badge: Title: $title
                    :newspaper: Post Content:
                    ${content.take(3500)}
                """.trimIndent()
            ).withContext(channel).titled("Marketplace Listing Deleted")

            if (postedByUser != null) log.withContext(postedByUser)
            log.post()
        }
    }

}
