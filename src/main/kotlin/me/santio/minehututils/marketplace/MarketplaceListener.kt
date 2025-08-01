package me.santio.minehututils.marketplace

import kotlinx.coroutines.launch
import me.santio.minehututils.bot
import me.santio.minehututils.cooldown.Cooldown
import me.santio.minehututils.cooldown.CooldownManager
import me.santio.minehututils.database.DatabaseHandler
import me.santio.minehututils.factories.EmbedFactory
import me.santio.minehututils.logger.GuildLogger
import me.santio.minehututils.resolvers.DurationResolver.discord
import me.santio.minehututils.scope
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.message.MessageBulkDeleteEvent
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.utils.FileUpload

object MarketplaceListener : ListenerAdapter() {

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (!event.isFromGuild) return

        scope.launch {
            val settings = DatabaseHandler.getSettings(event.guild!!.id)
            if (settings.marketplaceChannel == null || settings.marketplaceCooldown < 0L) {
                event.replyEmbeds(
                    EmbedFactory.error(
                        "The marketplace channel is not currently configured, come back later!",
                        event.guild!!
                    ).build()
                )
                    .setEphemeral(true)
                    .queue()
                return@launch
            }

            // minehut:marketplace:post:<type>
            val type = event.componentId.substringAfter("minehut:marketplace:post:")
            if (type != "offer" && type != "request") return@launch

            val cooldown = CooldownManager.get(event.user.id, Cooldown.getMarketplaceType(type))
            if (cooldown != null) {
                event.replyEmbeds(
                    EmbedFactory.error(
                        "You are currently on cooldown, try again ${cooldown.timeLeft().discord(true)}",
                        event.guild!!
                    ).build()
                ).setEphemeral(true).queue()
                return@launch
            }

            MarketplaceManager.handlePosting(event, type, settings)
        }
    }

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
        val type = message.type
        scope.launch {
            val postedByUser = bot.retrieveUserById(postedBy).complete()
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
