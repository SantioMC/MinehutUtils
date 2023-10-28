package me.santio.minehututils.ext

import java.time.Duration
import kotlin.math.floor

fun Duration.toCooldown(): Long {
    return floor(System.currentTimeMillis() / 1000.0).toLong() + this.toSeconds()
}

fun Long.isElapsed(): Boolean {
    return this <= floor(System.currentTimeMillis() / 1000.0).toLong()
}