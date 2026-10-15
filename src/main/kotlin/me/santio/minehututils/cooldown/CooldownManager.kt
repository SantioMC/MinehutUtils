package me.santio.minehututils.cooldown

import com.google.common.collect.HashBasedTable
import java.util.*
import kotlin.time.Duration

/**
 * This manager holds the cooldown state for all users who have an ongoing cooldown.
 * @author santio
 */
object CooldownManager {

    private val timer = Timer()
    private val cooldowns: HashBasedTable<String, Cooldown, UserCooldown> = HashBasedTable.create()

    /**
     * Starts the cooldown manager, this should be called once the bot is ready.
     */
    fun start() {
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() = synchronized(this@CooldownManager) {
                cooldowns.cellSet().filter { it.value.isElapsed() }.forEach {
                    cooldowns.remove(it.rowKey, it.columnKey)
                }
            }
        }, 0, 60000) // Cleanup task
    }

    // The table isn't thread-safe, and cooldowns are read and written from different event threads
    @Synchronized
    fun get(user: String, kind: Cooldown): UserCooldown? {
        return cooldowns.get(user, kind).takeIf { it?.isElapsed() == false }
    }

    @Synchronized
    fun set(user: String, kind: Cooldown, duration: Duration) {
        cooldowns.put(user, kind, UserCooldown(System.currentTimeMillis() / 1000, duration.inWholeSeconds))
    }

    /**
     * Sets a cooldown if the user doesn't already have one
     * @return Whether the cooldown was set
     */
    @Synchronized
    fun trySet(user: String, kind: Cooldown, duration: Duration): Boolean {
        if (get(user, kind) != null) return false
        set(user, kind, duration)
        return true
    }

    @Synchronized
    fun clear(user: String, kind: Cooldown? = null) {
        cooldowns.cellSet().filter { it.rowKey == user && (kind == null || it.columnKey == kind) }
            .forEach { cooldowns.remove(it.rowKey, it.columnKey) }
    }

    @Synchronized
    fun reset() {
        cooldowns.clear()
    }
}
