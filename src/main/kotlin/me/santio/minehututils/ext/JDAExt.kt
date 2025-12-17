package me.santio.minehututils.ext

import dev.minn.jda.ktx.interactions.components.TextInput
import dev.minn.jda.ktx.interactions.components.TextInputDefaults
import dev.minn.jda.ktx.interactions.components.row
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.components.label.Label
import net.dv8tion.jda.api.components.textinput.TextInput
import net.dv8tion.jda.api.components.textinput.TextInputStyle
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

fun short(
    id: String,
    label: String,
    required: Boolean = TextInputDefaults.required,
    value: String? = TextInputDefaults.value,
    placeholder: String? = TextInputDefaults.placeholder,
    requiredLength: IntRange? = TextInputDefaults.requiredLength,
): Label {
    val text = TextInput.create(id, TextInputStyle.SHORT)
    text.isRequired = required
    text.value = value
    text.placeholder = placeholder
    requiredLength?.let {
        text.setRequiredRange(it.first, it.last)
    }

    return Label.of(label, text.build())
}

fun paragraph(
    id: String,
    label: String,
    required: Boolean = TextInputDefaults.required,
    value: String? = TextInputDefaults.value,
    placeholder: String? = TextInputDefaults.placeholder,
    requiredLength: IntRange? = TextInputDefaults.requiredLength,
): Label {
    val text = TextInput.create(id, TextInputStyle.PARAGRAPH)
    text.isRequired = required
    text.value = value
    text.placeholder = placeholder
    requiredLength?.let {
        text.setRequiredRange(it.first, it.last)
    }

    return Label.of(label, text.build())
}
