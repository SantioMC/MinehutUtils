package me.santio.minehututils.cooldown

import me.santio.minehututils.resolvers.DurationResolver
import me.santio.minehututils.resolvers.DurationResolver.pretty
import me.santio.minehututils.utils.EnvUtils.env
import me.santio.minehututils.utils.TimeUtils.now
import java.time.Duration

/**
 * The cooldown data for a user
 * @param key The cooldown key this cooldown is for
 * @param timestamp When the cooldown started
 * @param duration The duration of the cooldown in seconds
 */
data class Cooldown(
    val key: Kind,
    val timestamp: Long,
    val duration: Long
) {

    /**
     * @return Whether the cooldown is expired
     */
    fun isExpired(): Boolean {
        return now() > (timestamp + duration)
    }

    /**
     * @return When the cooldown expires
     */
    fun expiresAt(): Long {
        return timestamp + duration
    }

    /**
     * @return The amount of time remaining pretty printed
     * @see DurationResolver.pretty
     */
    fun remaining(): String {
        val remaining = (timestamp + duration) - now()
        return pretty(Duration.ofSeconds(remaining))
    }

    enum class Kind(val display: String, private val envVariable: String) {
        ADVERTISEMENT_SERVER("Advertisement (Server)", "ADVERT_COOLDOWN"),
        ADVERTISEMENT_USER("Advertisement (User)", "ADVERT_COOLDOWN"),
        MARKET_OFFER("Marketplace (Offer)", "MARKET_COOLDOWN"),
        MARKET_REQUEST("Marketplace (Request)", "MARKET_COOLDOWN"),
        ;

        fun getDuration(): Duration {
            val value = env(this.envVariable, "24h")
            return DurationResolver.from(value)
                ?: Duration.ofHours(24)
        }

        fun getCooldownTime(): Long {
            return getDuration().toSeconds()
        }

    }

}