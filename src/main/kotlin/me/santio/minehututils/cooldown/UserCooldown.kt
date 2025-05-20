package me.santio.minehututils.cooldown

import java.time.Duration

/**
 * This class represents a user's cooldown state.
 * @author santio
 */
data class UserCooldown(
    val started: Long, // 1672531200
    val duration: Long // 84600
) {
    fun isElapsed() = (System.currentTimeMillis() / 1000) - started > duration
    fun timeLeft(): Duration = Duration.ofSeconds(duration - ((System.currentTimeMillis() / 1000) - started))
}
