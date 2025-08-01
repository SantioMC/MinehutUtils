package me.santio.minehututils.database.models

import gg.ingot.iron.annotations.Model
import gg.ingot.iron.strategies.NamingStrategy

@Model(table = "guild_data", naming = NamingStrategy.SNAKE_CASE)
class GuildData (
    val guildId: String,
    val stickyMessage: String? = null,
)
