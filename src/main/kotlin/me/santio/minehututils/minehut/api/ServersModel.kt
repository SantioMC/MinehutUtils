package me.santio.minehututils.minehut.api

import com.google.gson.annotations.SerializedName

data class ServersModel(
    @SerializedName("total_players")
    val totalPlayers: Int,

    @SerializedName("total_servers")
    val totalServers: Int,

    @SerializedName("total_search_results")
    val totalSearchResults: Int,

    val servers: List<SimpleServerModel>
)