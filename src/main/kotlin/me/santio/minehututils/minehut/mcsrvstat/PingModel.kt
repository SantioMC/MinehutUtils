package me.santio.minehututils.minehut.mcsrvstat

import com.google.gson.annotations.SerializedName

data class PingModel(
    val ip: String,
    val port: Int,
    val debug: Debug,
    val motd: Motd?,
    val players: Players?,
    val version: String?,
    val online: Boolean,
    val protocol: Protocol?,
    val hostname: String,
    val icon: String?,
    val software: String?,
    @SerializedName("eula_blocked")
    val eulaBlocked: Boolean?
)

data class Protocol(
    val version: Int,
)

data class Players(
    val online: Int,
    val max: Int
)

data class Motd(
    val raw: List<String>,
    val clean: List<String>,
    val html: List<String>
)

data class Debug(
    val ping: Boolean,
    val query: Boolean,
    val srv: Boolean,
    @SerializedName("querymismatch")
    val queryMismatch: Boolean,
    @SerializedName("ipinsrv")
    val ipInSrv: Boolean,
    @SerializedName("cnameinsrv")
    val cnameInSrv: Boolean,
    @SerializedName("animatedmotd")
    val animatedMotd: Boolean,
    @SerializedName("cachetime")
    val cacheTime: Int,
    @SerializedName("apiversion")
    val apiVersion: Int
)
