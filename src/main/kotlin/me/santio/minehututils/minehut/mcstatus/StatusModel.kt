package me.santio.minehututils.minehut.mcstatus

data class StatusModel(
    val online: Boolean,
    val players: Players?
)

data class Players(
    val online: Int,
    val max: Int
)
