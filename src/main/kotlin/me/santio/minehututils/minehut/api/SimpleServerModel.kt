package me.santio.minehututils.minehut.api

import com.google.gson.annotations.SerializedName

data class SimpleServerModel(
    val staticInfo: StaticInfoModel,
    val maxPlayers: Int,
    val name: String,
    val motd: String,
    val icon: String?,
    val playerData: PlayerDataModel,
    val connectable: Boolean,
    val visibility: Boolean,
    val allCategories: List<String>,
    val usingCosmetics: Boolean,
    val author: String?,
    val authorRank: String?
)

data class StaticInfoModel(
    @SerializedName("_id")
    val id: String,
    val serverPlan: String,
    val serviceStartDate: Long,
    val platform: String,
    val planMaxPlayers: Int?,
    val planRam: Int?,
    val alwaysOnline: Boolean,
    val rawPlan: String,
    val connectedServers: List<String>
)

data class PlayerDataModel(
    val playerCount: Int,
    val timeNoPlayers: Long,
)

