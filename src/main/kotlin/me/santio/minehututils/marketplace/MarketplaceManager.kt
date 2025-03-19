package me.santio.minehututils.marketplace

import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.interactions.components.Modal
import kotlinx.coroutines.launch
import me.santio.minehututils.bot
import me.santio.minehututils.cooldown.Cooldown
import me.santio.minehututils.cooldown.CooldownManager
import me.santio.minehututils.database.DatabaseHook
import me.santio.minehututils.database.models.MarketplaceMessage
import me.santio.minehututils.database.models.Settings
import me.santio.minehututils.factories.EmbedFactory
import me.santio.minehututils.iron
import me.santio.minehututils.resolvers.AutoModResolver
import me.santio.minehututils.resolvers.EmojiResolver
import me.santio.minehututils.scope
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.interactions.components.selections.StringSelectInteraction
import net.dv8tion.jda.api.utils.MarkdownSanitizer
import java.util.UUID
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object MarketplaceManager: DatabaseHook {

    private val messages = mutableSetOf<MarketplaceMessage>()

    override suspend fun onHook() {
        messages.addAll(this.fetchAll())
    }

    suspend fun fetchAll(): List<MarketplaceMessage> {
        return iron.prepare("SELECT * FROM marketplace_logs").all()
    }

    fun getListing(id: String): MarketplaceMessage? {
        return messages.firstOrNull { it.id == id }
    }

    suspend fun add(message: MarketplaceMessage) {
        messages.add(message)

        iron.prepare(
            "INSERT INTO marketplace_logs(id, posted_by, type, title, content, posted_at) VALUES (?, ?, ?, ?, ?, ?)",
            message.id,
            message.postedBy,
            message.type,
            message.title,
            message.content,
            message.postedAt
        )
    }

    fun handlePosting(e: StringSelectInteraction, type: String, settings: Settings) {
        if (e.isAcknowledged) return
        val id = UUID.randomUUID().toString()

        e.replyModal(Modal("minehut:marketplace:modal:$id", "Customize your listing") {
            short("minehut:listing:title", "The title of your listing", requiredLength = IntRange(1, 100))
            paragraph("minehut:listing:description", "The description of your listing")
        }).queue()

        bot.listener<ModalInteractionEvent>(timeout = 15.minutes) {
            if (it.modalId != "minehut:marketplace:modal:$id") return@listener
            cancel()

            val title =
                it.values.firstOrNull { it.id == "minehut:listing:title" }?.asString ?: error("No title provided")
            val description = it.values.firstOrNull { it.id == "minehut:listing:description" }?.asString
                ?: error("No description provided")

            // Run in auto-mod
            AutoModResolver.parse(it.guild!!, it.member!!, e, title, description).thenAccept { passes ->
                if (!passes) {
                    it.replyEmbeds(
                        EmbedFactory.error(
                            "Your message was caught by the filter and the request has been discarded.",
                            it.guild!!
                        ).build()
                    ).setEphemeral(true).queue()
                    return@thenAccept
                }
            }

            postListing(type, it, settings, title, description)
        }
    }

    fun postListing(
        type: String,
        event: ModalInteractionEvent,
        settings: Settings,
        title: String,
        description: String
    ) {
        val channel = bot.getTextChannelById(settings.marketplaceChannel!!) ?: run {
            event.replyEmbeds(
                EmbedFactory.error(
                    "Failed to find the marketplace channel, was it deleted?",
                    event.guild!!
                ).build()
            )
                .setEphemeral(true).queue()
            return
        }

        val kind = when (type) {
            "offer" -> "Offering"
            "request" -> "Requesting"
            else -> error("Invalid type provided")
        }

        val embed = EmbedFactory.default(
            """
            | ${EmojiResolver.find(event.guild, "minehut")?.formatted ?: ""} **$kind | ${
                MarkdownSanitizer.sanitize(title.replace("*", ""))
                    .ifEmpty { "Untitled" }
            }**
            |
            | $description
            """.trimMargin()) {

            it.setFooter(
                "Listing made by ${event.user.name} (${event.user.id})",
                event.user.avatarUrl
            )

            it.setColor(getEmbedColor(type))
        }

        channel.sendMessage("Listing posted by ${event.user.asMention}")
            .addEmbeds(embed.build())
            .queue {
                scope.launch {
                    add(MarketplaceMessage(
                        id = it.id,
                        postedBy = event.user.id,
                        type = type,
                        title = title,
                        content = description,
                        postedAt = it.timeCreated.toInstant().toEpochMilli()
                    ))
                }

                event.replyEmbeds(
                    EmbedFactory.default(
                        """
                    | **Success** ${EmojiResolver.yes(event.guild)?.formatted ?: ""}
                    |
                    | You have successfully posted your marketplace listing!
                    | Check it out! :point_right: ${it.jumpUrl}
                    """.trimMargin()
                    ).build()
                ).setEphemeral(true).queue()
            }

        CooldownManager.set(event.user.id, Cooldown.getMarketplaceType(type), settings.marketplaceCooldown.seconds)
    }

    fun getEmbedColor(type: String): Int {
        return when (type) {
            "offer" -> 0x70b560
            "request" -> 0xA160B5
            else -> error("Invalid type provided")
        }
    }

    fun clearOldMessages() {
        scope.launch {
            val now = System.currentTimeMillis()
            messages.removeIf { now - it.postedAt > 604800000 } // 7 days
            iron.prepare("DELETE FROM marketplace_logs WHERE posted_at < ?", now - 604800000)
        }
    }

}
