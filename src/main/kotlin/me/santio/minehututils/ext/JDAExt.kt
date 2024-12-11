package me.santio.minehututils.ext

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback

fun String.asEmote() = Emoji.fromUnicode(this)
fun Int.asEmote(): Emoji {
    if (this < 0 || this > 9) throw IllegalArgumentException("Number must be between 0 and 9")
    return Emoji.fromUnicode(this.toString() + "\u20E3")
}

fun Long.toTime(relative: Boolean = true): String {
    val seconds = if (this.toString().length == 13) this / 1000 else this
    return if (relative) "<t:${seconds.toInt()}:R>"
    else "<t:${seconds.toInt()}>"
}

fun Int.toTime(relative: Boolean = true): String = this.toLong().toTime(relative)

fun IReplyCallback.reply(embed: EmbedBuilder) = this.replyEmbeds(embed.build())
fun MessageChannel.sendMessage(embed: EmbedBuilder) = this.sendMessageEmbeds(embed.build())
