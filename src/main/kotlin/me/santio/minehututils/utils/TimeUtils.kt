package me.santio.minehututils.utils

import kotlin.math.floor

object TimeUtils {

    fun now(): Long {
        return floor(System.currentTimeMillis() / 1000.0).toLong()
    }

}