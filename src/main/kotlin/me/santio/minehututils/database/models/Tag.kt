package me.santio.minehututils.database.models

import gg.ingot.iron.annotations.Model
import gg.ingot.iron.strategies.NamingStrategy
import me.santio.minehututils.factories.EmbedFactory
import me.santio.minehututils.tags.SearchAlgorithm
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import java.net.URI
import java.net.URL

@Model(table = "tags", naming = NamingStrategy.SNAKE_CASE)
data class Tag(
    var id: Int? = null,
    private var searchAlg: String,
    var searchValue: String,
    var body: String,
    var uses: Int,
    val createdBy: String,
    val createdAt: Long,
    var updatedAt: Long
) {

    var regex: Regex? = null
        private set

    val name: String
        get() = "${searchAlg().name.lowercase()}: $searchValue"

    init {
        precompileRegex()
    }

    private fun precompileRegex() {
        if (searchAlg == SearchAlgorithm.REGEX.id) {
            this.regex = Regex(searchValue, RegexOption.IGNORE_CASE)
        } else {
            this.regex = null
        }
    }

    /**
     * Get the search algorithm for the tag
     * @return The search algorithm
     */
    fun searchAlg(): SearchAlgorithm {
        return SearchAlgorithm.from(searchAlg)
            ?: throw IllegalStateException("Invalid search algorithm: $searchAlg")
    }

    /**
     * Set the search algorithm for the tag
     * @param searchAlg The search algorithm to set
     */
    fun searchAlg(searchAlg: SearchAlgorithm) {
        this.searchAlg = searchAlg.id
        precompileRegex()
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
    fun send(message: Message) {
        var lines = body.lines().toMutableList()
        var buttons = mutableListOf<Button>()

        for (line in body.lines()) {
            if (buttons.size >= 5) break

            val match = buttonRegex.find(line) ?: continue
            lines.remove(line)

            var button = Button.of(
                ButtonStyle.LINK,
                match.groupValues[3],
                match.groupValues[2],
            )

            buttons.add(button)
        }

        val lastLine = lines.lastOrNull() ?: return
        var image: URL? = null

        try {
            image = URI.create(lastLine).toURL()
            lines.remove(lastLine)
        } catch (e: Exception) {}

        val reply = message.replyEmbeds(
            EmbedFactory.default(lines.joinToString("\n"))
                .setFooter("Requested by ${message.author.name} (${message.author.id})")
                .setImage(image?.toString())
                .build()
        )

        if (buttons.isNotEmpty()) reply.addActionRow(*buttons.toTypedArray())
        reply.queue()
    }

    private companion object {
        val buttonRegex = Regex("^\\[(:.+:)?(.+)]\\((https://.+)\\)", RegexOption.IGNORE_CASE)
    }

}
