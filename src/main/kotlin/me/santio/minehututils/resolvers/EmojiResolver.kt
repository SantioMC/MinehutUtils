package me.santio.minehututils.resolvers

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.emoji.Emoji

/**
 * Finds a specific emoji that fits best based on what server we're on.
 */
@Suppress("unused")
object EmojiResolver {

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