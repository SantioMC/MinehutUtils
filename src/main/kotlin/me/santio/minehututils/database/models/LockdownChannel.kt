package me.santio.minehututils.database.models

import gg.ingot.iron.annotations.Model
import gg.ingot.iron.strategies.NamingStrategy

@Model(table = "lockdown_channels", naming = NamingStrategy.SNAKE_CASE)
data class LockdownChannel(
    val guildId: String,
    val channelId: String
)
