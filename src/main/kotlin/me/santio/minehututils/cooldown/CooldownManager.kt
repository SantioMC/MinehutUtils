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
     *
     * todo: Unused in bot? Probably remove if unnecessary to prevent confusion
     */
    fun start() {
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                // Use iterator to avoid creating intermediate filtered list
                val iterator = cooldowns.cellSet().iterator()
                while (iterator.hasNext()) {
                    if (iterator.next().value.isElapsed()) {
                        iterator.remove()
                    }
                }
            }
        }, 0, 60000) // Cleanup task
    }

    fun get(user: String, kind: Cooldown): UserCooldown? {
        return cooldowns.get(user, kind).takeIf { it?.isElapsed() == false }
    }

    fun set(user: String, kind: Cooldown, duration: Duration) {
        cooldowns.put(user, kind, UserCooldown(System.currentTimeMillis() / 1000, duration.inWholeSeconds))
    }

    fun clear(user: String, kind: Cooldown? = null) {
        // Use iterator to avoid creating intermediate filtered list and concurrent modification
        val iterator = cooldowns.cellSet().iterator()
        while (iterator.hasNext()) {
            val cell = iterator.next()
            if (cell.rowKey == user && (kind == null || cell.columnKey == kind)) {
                iterator.remove()
            }
        }
    }

    fun reset() {
        cooldowns.clear()
    }
}
