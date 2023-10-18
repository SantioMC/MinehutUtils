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

    fun checkmark() = Emoji.fromUnicode("✅")
    fun crossmark() = Emoji.fromUnicode("❌")
    fun warning() = Emoji.fromUnicode("⚠️")

}