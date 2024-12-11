package me.santio.minehututils.cooldown

import java.util.*
import kotlin.time.Duration

/**
 * This manager holds the cooldown state for all users who have an ongoing cooldown.
 * @author santio
 */
object CooldownManager {

    private val timer = Timer()
    private val cooldowns = mutableSetOf<UserCooldown>()

    /**
     * Starts the cooldown manager, this should be called once the bot is ready.
     */
    fun start() {
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                cooldowns.removeIf { it.isElapsed() }
            }
        }, 0, 60000) // Cleanup task
    }

    fun get(user: String, kind: Cooldown): UserCooldown? {
        return cooldowns.firstOrNull { it.user == user && it.kind == kind && !it.isElapsed() }
    }

    fun set(user: String, kind: Cooldown, duration: Duration) {
        this.clear(user, kind)
        cooldowns.add(UserCooldown(user, kind, System.currentTimeMillis() / 1000, duration.inWholeSeconds))
    }

    fun clear(user: String, kind: Cooldown? = null) {
        cooldowns.removeIf { it.user == user && (kind == null || it.kind == kind) }
    }

    fun reset() {
        cooldowns.clear()
    }

}
