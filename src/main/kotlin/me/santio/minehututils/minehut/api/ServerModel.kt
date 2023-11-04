package me.santio.minehututils.minehut.api

import com.google.gson.annotations.SerializedName
import me.santio.minehututils.minehut.Minehut
import me.santio.minehututils.minehut.ServerPlan

@Suppress("MemberVisibilityCanBePrivate", "unused")
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
    @SerializedName("daily_online_time")
    private val dailyUptime: Map<String, Int>? = null,
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

    val uptime: Long
        get() = (dailyUptime?.values?.first() ?: 0) + (System.currentTimeMillis() - lastOnline)

    val timeRemaining: Long
        get() = Minehut.dailyTimeLimit.toMillis() - uptime

    val outOfTime: Boolean
        get() = timeRemaining <= 0

    val hasDailyLimit: Boolean
        get() = dailyUptime.isNullOrEmpty().not()
}