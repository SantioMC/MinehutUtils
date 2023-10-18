package me.santio.minehututils.minehut.api

import com.google.gson.annotations.SerializedName

data class SimpleStatsModel(
    @SerializedName("player_count")
    val playerCount: Int,
    @SerializedName("server_count")
    val serverCount: Int,
    @SerializedName("server_max")
    val serverMax: Int,
    @SerializedName("ram_count")
    val ramCount: Int,
    @SerializedName("ram_max")
    val ramMax: Int,
)