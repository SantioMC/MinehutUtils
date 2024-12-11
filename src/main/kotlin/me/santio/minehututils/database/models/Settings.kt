package me.santio.minehututils.database.models

import gg.ingot.iron.annotations.Model
import gg.ingot.iron.strategies.NamingStrategy

@Model(table = "settings", naming = NamingStrategy.SNAKE_CASE)
data class Settings(
    val id: Int,
    val marketplaceChannel: String?,
    val marketplaceCooldown: Int
)
