package me.santio.minehututils.marketplace

import dev.minn.jda.ktx.interactions.components.Modal
import dev.minn.jda.ktx.interactions.components.TextInput
import dev.minn.jda.ktx.interactions.components.button
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import me.santio.minehututils.bot
import me.santio.minehututils.cooldown.Cooldown
import me.santio.minehututils.cooldown.CooldownManager
import me.santio.minehututils.coroutines.await
import me.santio.minehututils.coroutines.exceptionHandler
import me.santio.minehututils.database.DatabaseHandler
import me.santio.minehututils.database.DatabaseHook
import me.santio.minehututils.database.models.MarketplaceMessage
import me.santio.minehututils.database.models.Settings
import me.santio.minehututils.factories.EmbedFactory
import me.santio.minehututils.iron
import me.santio.minehututils.resolvers.AutoModResolver
import me.santio.minehututils.resolvers.DurationResolver.discord
import me.santio.minehututils.resolvers.EmojiResolver
import me.santio.minehututils.scope
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.interactions.callbacks.IModalCallback
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.utils.MarkdownSanitizer
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

object MarketplaceManager: DatabaseHook {

    private val logger = LoggerFactory.getLogger(MarketplaceManager::class.java)

    private val INVITE_REGEX = Regex(
        "(https?://)?(www\\.)?((discord(app)?\\.com/invite)|(discord\\.gg))/(\\w+)",
        RegexOption.IGNORE_CASE
    )

    // Read by message delete events while listings are added and cleared from other threads
    private val messages = ConcurrentHashMap<String, MarketplaceMessage>()

    override suspend fun onHook() {
        this.fetchAll().forEach { messages[it.id] = it }
    }

    suspend fun fetchAll(): List<MarketplaceMessage> {
        return iron.prepare("SELECT * FROM marketplace_logs").all()
    }

    fun getListing(id: String): MarketplaceMessage? {
        return messages[id]
    }

    suspend fun getStickyMessage(guild: Guild): Message? {
        val settings = DatabaseHandler.getSettings(guild.id)
        if (settings.marketplaceChannel == null) return null

        val stickyMessageId = DatabaseHandler.getData(guild.id).stickyMessage
        return stickyMessageId?.let { bot.getTextChannelById(settings.marketplaceChannel)?.retrieveMessageById(it)?.await() }
    }

    suspend fun add(message: MarketplaceMessage) {
        messages[message.id] = message

        iron.prepare(
            "INSERT INTO marketplace_logs(id, posted_by, type, title, content, posted_at) VALUES (:id, :postedBy, :type, :title, :content, :postedAt)",
            message.bindings()
        )
    }

    fun handlePosting(e: IModalCallback, type: String) {
        if (e.isAcknowledged) return

        e.replyModal(Modal("minehut:marketplace:modal:$type", "Customize your listing") {
            label("The title of your listing", child = TextInput("minehut:listing:title", TextInputStyle.SHORT, requiredLength = IntRange(1, 100)))
            // Leaves room for the listing header within Discord's 4096 character embed limit
            label("The description of your listing", child = TextInput("minehut:listing:description", TextInputStyle.PARAGRAPH, requiredLength = IntRange(1, 3800)))
        }).queue()
    }

    suspend fun handleSubmit(event: ModalInteractionEvent, type: String) {
        val settings = DatabaseHandler.getSettings(event.guild!!.id)
        if (!isAvailable(event, settings, type)) return

        val title =
            event.getValue("minehut:listing:title")?.asString ?: error("No title provided")
        val description = event.getValue("minehut:listing:description")?.asString
            ?: error("No description provided")

        // Restrict the number of repetitive empty lines
        val trimmed = description.lines().joinToString("\n") { line -> line.trim() }
        if (trimmed.contains("\n\n\n")) { // Check if there are 3 new line characters in a row
            event.replyEmbeds(
                EmbedFactory.error(
                    "Your message has too many empty lines in a row, try reducing the amount of empty lines.",
                    event.guild!!
                ).build()
            ).setEphemeral(true).queue()
            return
        }

        // Restrict Discord invite links
        if (description.contains(INVITE_REGEX) || title.contains(INVITE_REGEX)) {
            event.replyEmbeds(
                EmbedFactory.error(
                    "Your message contains a Discord invite link, which is not allowed in marketplace listings.",
                    event.guild!!
                ).build()
            ).setEphemeral(true).queue()
            return
        }

        val cooldown = Cooldown.getMarketplaceType(type)
        if (!CooldownManager.trySet(event.user.id, cooldown, settings.marketplaceCooldown.seconds)) {
            val active = CooldownManager.get(event.user.id, cooldown)
            event.replyEmbeds(
                EmbedFactory.error(
                    if (active != null) "You are currently on cooldown, try again ${active.timeLeft().discord(true)}"
                    else "Your other listing is still being posted, try again in a moment.",
                    event.guild!!
                ).build()
            ).setEphemeral(true).queue()
            return
        }

        try {
            // Acknowledge now, checking auto-mod and posting the listing can take longer than Discord allows
            event.deferReply(true).queue()

            // Run in auto-mod, if the rules can't be checked in time the listing is allowed through
            val passes = AutoModResolver.parse(event.guild!!, event.member!!, event, title, description)
                .completeOnTimeout(true, 2, TimeUnit.SECONDS)
                .await()

            if (!passes) {
                CooldownManager.clear(event.user.id, cooldown)
                event.hook.editOriginalEmbeds(
                    EmbedFactory.error(
                        "Your message was caught by the filter and the request has been discarded.",
                        event.guild!!
                    ).build()
                ).queue()
                return
            }

            postListing(type, event, settings, title, description)
        } catch (e: CancellationException) {
            CooldownManager.clear(event.user.id, cooldown)
            throw e
        } catch (e: Exception) {
            listingFailed(event, type, e)
        }
    }

    /**
     * Checks the marketplace is configured and the user isn't on cooldown, replying with the reason if not
     * @return Whether the user can post a listing of this type
     */
    fun isAvailable(event: IReplyCallback, settings: Settings, type: String): Boolean {
        if (settings.marketplaceChannel == null || settings.marketplaceCooldown < 0L) {
            event.replyEmbeds(
                EmbedFactory.error(
                    "The marketplace channel is not currently configured, come back later!",
                    event.guild!!
                ).build()
            ).setEphemeral(true).queue()
            return false
        }

        val cooldown = CooldownManager.get(event.user.id, Cooldown.getMarketplaceType(type))
        if (cooldown != null) {
            event.replyEmbeds(
                EmbedFactory.error(
                    "You are currently on cooldown, try again ${cooldown.timeLeft().discord(true)}",
                    event.guild!!
                ).build()
            ).setEphemeral(true).queue()
            return false
        }

        return true
    }

    private fun listingFailed(event: ModalInteractionEvent, type: String, err: Throwable) {
        CooldownManager.clear(event.user.id, Cooldown.getMarketplaceType(type))
        logger.warn("Failed to post a marketplace listing: {}", err.toString())
        event.hook.editOriginalEmbeds(
            EmbedFactory.error("Failed to post your listing, please try again later.", event.guild!!).build()
        ).queue()
    }

    fun postListing(
        type: String,
        event: ModalInteractionEvent,
        settings: Settings,
        title: String,
        description: String
    ) {
        val channel = bot.getTextChannelById(settings.marketplaceChannel!!) ?: run {
            CooldownManager.clear(event.user.id, Cooldown.getMarketplaceType(type))
            event.hook.editOriginalEmbeds(
                EmbedFactory.error(
                    "Failed to find the marketplace channel, was it deleted?",
                    event.guild!!
                ).build()
            ).queue()
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
            .queue({
                CooldownManager.set(event.user.id, Cooldown.getMarketplaceType(type), settings.marketplaceCooldown.seconds)

                scope.launch(exceptionHandler) {
                    add(MarketplaceMessage(
                        id = it.id,
                        postedBy = event.user.id,
                        type = type,
                        title = title,
                        content = description,
                        postedAt = it.timeCreated.toInstant().toEpochMilli()
                    ))

                    sendStickyEmbed(channel)
                }

                event.hook.editOriginalEmbeds(
                    EmbedFactory.default(
                        """
                    | **Success** ${EmojiResolver.yes(event.guild)?.formatted ?: ""}
                    |
                    | You have successfully posted your marketplace listing!
                    | Check it out! :point_right: ${it.jumpUrl}
                    """.trimMargin()
                    ).build()
                ).queue()
            }, { err -> listingFailed(event, type, err) })
    }

    suspend fun sendStickyEmbed(channel: TextChannel) {
        // The previous sticky may have been deleted by hand, that shouldn't stop a new one being posted
        val previous = runCatching { getStickyMessage(channel.guild) }.getOrElse {
            if (it is ErrorResponseException && it.errorResponse == ErrorResponse.UNKNOWN_MESSAGE) null else throw it
        }
        previous?.delete()?.queue()

        val offerButton = button("minehut:marketplace:post:offer", "Post an Offering", Emoji.fromFormatted("\uD83D\uDCE2"), style = ButtonStyle.SUCCESS)
        val requestButton = button("minehut:marketplace:post:request", "Post a Request", Emoji.fromFormatted("📝"), style = ButtonStyle.PRIMARY)

        val message = channel.sendMessageEmbeds(
            EmbedFactory.default(
                """
                ${EmojiResolver.find(channel.guild, "minehut")?.formatted ?: ""} **OFFERS AND REQUESTS**
                
                To post something here, press the button below, select either **Offering** or **Requesting** then provide a title and description.

                Once you create a post:
                🕐 You will need to wait 24 hours before posting again
                📝 You won't be able to edit it again.

                Read the pinned message in this channel to learn more!
                """.trimMargin()
            ).build()
        ).addComponents(ActionRow.of(offerButton, requestButton)).complete()

        iron.prepare(
            "UPDATE guild_data SET sticky_message = ? WHERE guild_id = ?",
            message.id,
            channel.guild.id
        )
    }

    fun getEmbedColor(type: String): Int {
        return when (type) {
            "offer" -> 0x70b560
            "request" -> 0xA160B5
            else -> error("Invalid type provided")
        }
    }

    fun clearOldMessages() {
        scope.launch(exceptionHandler) {
            val now = System.currentTimeMillis()
            messages.values.removeIf { now - it.postedAt > 604800000 } // 7 days
            iron.prepare("DELETE FROM marketplace_logs WHERE posted_at < ?", now - 604800000)
        }
    }

}
