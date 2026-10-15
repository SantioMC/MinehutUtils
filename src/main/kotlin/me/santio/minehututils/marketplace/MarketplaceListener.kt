package me.santio.minehututils.marketplace

import kotlinx.coroutines.launch
import me.santio.minehututils.bot
import me.santio.minehututils.coroutines.await
import me.santio.minehututils.coroutines.exceptionHandler
import me.santio.minehututils.database.DatabaseHandler
import me.santio.minehututils.logger.GuildLogger
import me.santio.minehututils.scope
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.events.message.MessageBulkDeleteEvent
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.utils.FileUpload

object MarketplaceListener : ListenerAdapter() {

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (!event.isFromGuild) return

        // minehut:marketplace:post:<type>, other buttons are handled by their own listeners
        if (!event.componentId.startsWith("minehut:marketplace:post:")) return
        val type = event.componentId.substringAfter("minehut:marketplace:post:")
        if (type != "offer" && type != "request") return

        scope.launch(exceptionHandler) {
            val settings = DatabaseHandler.getSettings(event.guild!!.id)
            if (MarketplaceManager.isAvailable(event, settings, type)) MarketplaceManager.handlePosting(event, type)
        }
    }

    override fun onStringSelectInteraction(event: StringSelectInteractionEvent) {
        if (!event.isFromGuild || event.componentId != "minehut:marketplace:type") return
        val type = event.values.firstOrNull()
        if (type != "offer" && type != "request") return

        scope.launch(exceptionHandler) {
            val settings = DatabaseHandler.getSettings(event.guild!!.id)
            if (MarketplaceManager.isAvailable(event, settings, type)) MarketplaceManager.handlePosting(event, type)
        }
    }

    override fun onModalInteraction(event: ModalInteractionEvent) {
        if (!event.isFromGuild || !event.modalId.startsWith("minehut:marketplace:modal:")) return
        val type = event.modalId.substringAfter("minehut:marketplace:modal:")
        if (type != "offer" && type != "request") return

        scope.launch(exceptionHandler) {
            MarketplaceManager.handleSubmit(event, type)
        }
    }

    override fun onMessageDelete(event: MessageDeleteEvent) {
        if (!event.isFromGuild || event.channelType != ChannelType.TEXT) return
        logMarketplaceDelete(event.channel.asTextChannel(), event.messageId)
    }

    override fun onMessageBulkDelete(event: MessageBulkDeleteEvent) {
        if (event.channel.type != ChannelType.TEXT) return
        event.messageIds.forEach { logMarketplaceDelete(event.channel.asTextChannel(), it) }
    }

    private fun logMarketplaceDelete(channel: TextChannel, messageId: String) {
        val message = MarketplaceManager.getListing(messageId) ?: return

        val title = message.title
        val content = message.content
        val postedBy = message.postedBy
        val type = message.type
        scope.launch(exceptionHandler) {
            val postedByUser = bot.retrieveUserById(postedBy).await()
            val log = GuildLogger.of(channel.guild).log(
                """
                    :identification_card: User: ${postedByUser?.asMention} *(${postedByUser?.name} - ${postedByUser?.id})*
                    :label: Type: $type
                    :name_badge: Title: $title
                """.trimIndent()
            ).withContext(channel)
                .withFile(FileUpload.fromData(content.toByteArray().inputStream(), "listing-$messageId.txt"))
                .titled("Marketplace Listing Deleted")

            if (postedByUser != null) log.withContext(postedByUser)
            log.post()
        }
    }

}
