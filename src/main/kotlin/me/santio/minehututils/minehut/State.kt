package me.santio.minehututils.minehut

import me.santio.minehututils.ext.asEmote
import net.dv8tion.jda.api.entities.emoji.Emoji

enum class State(private val icon: Emoji, private val text: String) {
    ONLINE("\uD83D\uDFE2".asEmote(), "Online"),
    OFFLINE("\uD83D\uDD34".asEmote(), "Offline"),
    FAILED("\uD83D\uDD34".asEmote(), "Failed to fetch"),
    DEGRADED("\uD83D\uDFE1".asEmote(), "Degraded"),
    OUTDATED("\uD83D\uDFE2".asEmote(), "Outdated"),
    ;

    override fun toString(): String {
        return "$text ${icon.formatted}"
    }
}
