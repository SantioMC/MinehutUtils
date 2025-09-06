package me.santio.minehututils.database.models

import gg.ingot.iron.annotations.Model
import gg.ingot.iron.bindings.Bindings
import gg.ingot.iron.strategies.NamingStrategy

@Model(table = "booster_pass", naming = NamingStrategy.SNAKE_CASE)
data class BoosterPass(
    val id: Int? = null,
    val guildId: String,
    val giver: String,
    val receiver: String,
    val givenAt: Long = System.currentTimeMillis()
): Bindings
