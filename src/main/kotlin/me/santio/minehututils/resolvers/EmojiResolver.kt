package me.santio.minehututils.resolvers

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.fellbaum.jemoji.EmojiManager

/**
 * Finds a specific emoji that fits best based on what server we're on.
 */
@Suppress("unused")
object EmojiResolver {

    /**
     * Finds an emoji by its alias (ex: :yes: or yes)
     * @param alias The alias to search for
     * @return The emoji, or null if not found
     */
    fun fromAlias(alias: String): Emoji? {
        return EmojiManager.getByAlias(alias).orElse(null)?.let {
            Emoji.fromUnicode(it.emoji)
        }
    }

    /**
     * Finds an emoji by multiple kinds of formats
     * @param query The query to search for
     * @return The emoji, or null if not found
     */
    fun find(query: String): Emoji? {
        return when {
            query.startsWith("<") && query.endsWith(">") -> Emoji.fromFormatted(query)
            query.startsWith(":") && query.endsWith(":") -> fromAlias(query)
            else -> Emoji.fromUnicode(query)
        }
    }

    /**
     * Finds an emote called :yes: in the current guild
     * @param guild The guild to search in.
     * @param default The default emoji to use if not found.
     * @return The emoji, or null if not found.
     */
    fun find(guild: Guild?, emote: String, default: Emoji? = null): Emoji? {
        return guild?.getEmojisByName(emote, true)?.firstOrNull()
            ?: default
    }

    /**
     * Finds an emote called :yes: in the current guild
     * @param guild The guild to search in.
     * @return The emoji, or the default checkmark if not found.
     */
    fun yes(guild: Guild?) = this.find(guild, "yes", checkmark())

    /**
     * Finds an emote called :no: in the current guild
     * @param guild The guild to search in.
     * @return The emoji, or the default crossmark if not found.
     */
    fun no(guild: Guild?) = this.find(guild, "no", crossmark())

    /// Default emojis

    fun checkmark() = Emoji.fromUnicode("✅")
    fun crossmark() = Emoji.fromUnicode("❌")
    fun warning() = Emoji.fromUnicode("⚠️")

}
