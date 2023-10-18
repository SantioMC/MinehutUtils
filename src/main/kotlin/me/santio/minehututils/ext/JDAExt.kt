package me.santio.minehututils.ext

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback

fun String.asEmote() = Emoji.fromUnicode(this)
fun Int.asEmote(): Emoji {
    if (this < 0 || this > 9) throw IllegalArgumentException("Number must be between 0 and 9")
    return Emoji.fromUnicode(this.toString() + "\u20E3")
}

fun Long.toTime(relative: Boolean = true): String {
    return if (relative) "<t:${(this / 1000).toInt()}:R>"
    else "<t:${(this / 1000).toInt()}>"
}

fun IReplyCallback.reply(embed: EmbedBuilder) = this.replyEmbeds(embed.build())