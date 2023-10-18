package me.santio.minehututils.minehut.api

data class PlayerStatsModel(
    val bedrockTotal: Int,
    val javaTotal: Int,
    val bedrockLobby: Int,
    val bedrockPlayerServer: Int,
    val javaLobby: Int,
    val javaPlayerServer: Int,
)