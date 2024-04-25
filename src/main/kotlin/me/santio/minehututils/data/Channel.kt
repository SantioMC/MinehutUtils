package me.santio.minehututils.data

import me.santio.minehututils.utils.EnvUtils.env

enum class Channel(val display: String, private val envVariable: String) {

    ADVERTISEMENTS("Advertisements", "ADVERT_CHANNEL"),
    MARKETPLACE("Marketplace", "MARKET_CHANNEL"),
    ;

    fun get(): String? {
        return env(this.envVariable)
    }

}