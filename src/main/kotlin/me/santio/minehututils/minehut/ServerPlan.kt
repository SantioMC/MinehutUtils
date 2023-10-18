package me.santio.minehututils.minehut

import me.santio.minehututils.minehut.api.ServerModel

enum class ServerPlan {

    FREE,
    CUSTOM,
    DAILY,
    MH20,
    MH35,
    MH75,
    MHUnlimited,
    EXTERNAL,
    ;

    companion object {
        fun from(server: ServerModel): ServerPlan {
            val data = server.serverPlan.split("_")
            val plan = if (data[0] == "CUSTOM") "CUSTOM" else data[data.size - 1].uppercase()

            return when (plan) {
                "FREE" -> FREE
                "DAILY" -> DAILY
                "2GB" -> MH20
                "3GB" -> MH35
                "6GB" -> MH75
                "10GB" -> MHUnlimited
                "CUSTOM" -> CUSTOM
                "EXTERNAL" -> EXTERNAL
                else -> FREE
            }
        }
    }

}