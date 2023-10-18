package me.santio.minehututils.minehut.api

import com.google.gson.annotations.SerializedName
import me.santio.minehututils.minehut.Minehut
import me.santio.minehututils.minehut.ServerPlan

data class ServerModel(
    @SerializedName("_id")
    val id: String,
    val owner: String,
    val name: String,
    @SerializedName("name_lower")
    val nameLower: String,
    @SerializedName("creation")
    val createdAt: Long,
    val platform: String,
    @SerializedName("storage_node")
    val storageNode: String,
    val suspended: Boolean,
    @SerializedName("__v")
    val version: Int,
    val port: Int,
    @SerializedName("last_online")
    val lastOnline: Long,
    val motd: String,
    @SerializedName("credits_per_day")
    val creditsPerDay: Double,
    val visibility: Boolean,
    val offer: String,
    @SerializedName("purchased_icons")
    val purchasedIcons: List<String>,
    @SerializedName("active_icon")
    val activeIcon: String,
    @SerializedName("server_plan")
    val serverPlan: String,
    @SerializedName("backup_slots")
    val backupSlots: Int,
    val categories: List<String>,
    @SerializedName("server_version_type")
    val serverVersionType: String,
    val connectedServers: List<String>,
    val inheritedCategories: List<String>,
    val proxy: Boolean,
    @SerializedName("default_banner_image")
    val defaultBannerImage: String,
    @SerializedName("default_banner_tint")
    val defaultBannerTint: String,
    @SerializedName("using_cosmetics")
    val usingCosmetics: Boolean,
    val joins: Int,
    val icon: String,
    val online: Boolean,
    val maxPlayers: Int,
    val playerCount: Int,
    val rawPlan: String,
    val activeServerPlan: String
) {

    val plan: ServerPlan
        get() = ServerPlan.from(this)

    val ownerUsername: String
        get() = Minehut.servers()?.servers?.find { it.staticInfo.id == id }?.author ?: "Unknown"

}