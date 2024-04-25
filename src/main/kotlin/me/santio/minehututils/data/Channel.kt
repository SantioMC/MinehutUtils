package me.santio.minehututils.data

import me.santio.minehututils.utils.EnvUtils.env

enum class Channel(private val envVariable: String) {

    ADVERTISEMENTS("ADVERT_CHANNEL"),
    MARKETPLACE("MARKET_CHANNEL"),
    ;

    fun get(): String? {
        return env(this.envVariable)
    }

}