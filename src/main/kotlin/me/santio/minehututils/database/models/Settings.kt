package me.santio.minehututils.database.models

import gg.ingot.iron.annotations.Model
import gg.ingot.iron.strategies.NamingStrategy

@Model(table = "settings", naming = NamingStrategy.SNAKE_CASE)
data class Settings(
    val guildId: String,
    val marketplaceChannel: String? = null,
    val marketplaceCooldown: Int = 86400,
    val lockdownRole: String? = null
)
