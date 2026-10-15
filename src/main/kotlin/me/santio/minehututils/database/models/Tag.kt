package me.santio.minehututils.database.models

import gg.ingot.iron.annotations.Model
import gg.ingot.iron.strategies.NamingStrategy
import me.santio.minehututils.factories.EmbedFactory
import me.santio.minehututils.resolvers.EmojiResolver
import me.santio.minehututils.tags.SearchAlgorithm
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.entities.Message
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URL

@Model(table = "tags", naming = NamingStrategy.SNAKE_CASE)
data class Tag(
    var id: Int? = null,
    private var searchAlg: String,
    var searchValue: String,
    var body: String,
    var uses: Int,
    var guildId: String?,
    val createdBy: String,
    val createdAt: Long,
    var updatedAt: Long,
    var lastUsed: Long = 0,
) {

    var regex: Regex? = null
        private set

    private var algorithm: SearchAlgorithm? = null
    var terms: List<String> = emptyList()
        private set

    val name: String
        get() = "${searchAlg().name.lowercase()}: $searchValue"

    init {
        precompile()
    }

    private fun precompile() {
        algorithm = SearchAlgorithm.from(searchAlg)

        // Blank segments (from "a|" or "a||b") would otherwise match every message
        terms = searchValue.split('|').filter { it.isNotBlank() }

        if (searchAlg == SearchAlgorithm.REGEX.id) {
            // An invalid stored pattern shouldn't stop every other tag from loading, it just never matches
            this.regex = runCatching { Regex(searchValue, RegexOption.IGNORE_CASE) }
                .onFailure { logger.warn("Tag {} has an invalid regex: {}", id, searchValue) }
                .getOrNull()
        } else {
            this.regex = null
        }
    }

    /**
     * Get the search algorithm for the tag
     * @return The search algorithm
     */
    fun searchAlg(): SearchAlgorithm {
        return algorithm
            ?: throw IllegalStateException("Invalid search algorithm: $searchAlg")
    }

    /**
     * Set the search algorithm for the tag
     * @param searchAlg The search algorithm to set
     */
    fun searchAlg(searchAlg: SearchAlgorithm) {
        this.searchAlg = searchAlg.id
        precompile()
    }

    /**
     * Check if the tag is included in the specified message
     * @param message The message to check
     * @return True if the tag is included, false otherwise
     */
    fun isIncluded(message: String): Boolean {
        return searchAlg().predicate(this, message)
    }

    /**
     * Send the tag to the specified message
     * @param message The message to send the tag to
     */
    fun send(message: Message, silent: Boolean = false) {
        // Deleting someone else's message needs Manage Messages, without it we still reply
        if (silent && message.guild.selfMember.hasPermission(message.guildChannel, Permission.MESSAGE_MANAGE)) {
            message.delete().queue()
        }
        
        val lines = body.lines().toMutableList()
        val buttons = mutableListOf<Button>()

        for (line in body.lines()) {
            if (buttons.size >= 5) break

            val match = buttonRegex.find(line) ?: continue
            lines.remove(line)

            val emoji = match.groupValues[1].takeIf { it.isNotBlank() }
                ?.let { EmojiResolver.find(it) }

            var button = Button.of(
                ButtonStyle.LINK,
                match.groupValues[3],
                match.groupValues[2],
            )

            if (emoji != null) {
                button = button.withEmoji(emoji)
            }

            buttons.add(button)
        }

        // A body made only of buttons has no lines left, but should still be sent
        val lastLine = lines.lastOrNull()
        var image: URL? = null

        // The last line is used as the image if it's a URL, anything else is just text
        if (lastLine != null) runCatching {
            image = URI.create(lastLine).toURL()
            lines.remove(lastLine)
        }

        val embed = EmbedFactory.default(lines.joinToString("\n"))
            .setFooter("Requested by ${message.author.name} (${message.author.id})")
            .setImage(image?.toString())
            .build()

        val reply = if (silent) {
            message.channel.sendMessageEmbeds(embed)
        } else {
            message.replyEmbeds(embed).mentionRepliedUser(false)
        }

        if (buttons.isNotEmpty()) reply.setComponents(ActionRow.of(buttons))
        reply.queue(null) { logger.warn("Failed to send tag {} in {}: {}", id, message.channel.id, it.toString()) }
    }

    private companion object {
        val logger = LoggerFactory.getLogger(Tag::class.java)
        val buttonRegex = Regex("^\\[(:.+:|<.+>)?(.+)]\\((https://.+|<https://.*>)\\)", RegexOption.IGNORE_CASE)
    }

}
