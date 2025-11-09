package me.santio.minehututils.database.models

import gg.ingot.iron.annotations.Model
import gg.ingot.iron.bindings.Bindings
import gg.ingot.iron.strategies.NamingStrategy

@Model(table = "marketplace_logs", naming = NamingStrategy.SNAKE_CASE)
data class MarketplaceMessage(
    var id: String,
    val postedBy: String,
    val type: String,
    val title: String,
    val content: String,
    val paid: Boolean,
    val postedAt: Long
): Bindings
