package me.santio.minehututils.cooldown

import me.santio.minehututils.utils.TimeUtils.now
import java.time.Duration

/**
 * Holds in-memory data about ongoing cooldowns.
 * @see Cooldown
 */
object CooldownRegistry {

    private val _cooldowns = mutableMapOf<String, HashSet<Cooldown>>()

    /**
     * The current stored data for cooldowns
     */
    val cooldowns: Map<String, HashSet<Cooldown>>
        get() = _cooldowns.toMap()

    /**
     * Get the cooldown data for an entity if it isn't expired
     * @param id The id of the entity to get the cooldown from
     * @param key The related cooldown to get
     * @return The valid non-expired cooldown
     */
    fun getCooldown(id: String, key: Cooldown.Kind): Cooldown? {
        return _cooldowns[id]?.firstOrNull { it.key == key && !it.isExpired() }
    }

    /**
     * Put an entity on cooldown
     * @param id The entity to set the cooldown for
     * @param key The related cooldown to set
     */
    fun setCooldown(id: String, key: Cooldown.Kind, duration: Duration? = null) {
        val cooldowns = _cooldowns[id] ?: hashSetOf()

        cooldowns.removeIf { it.key == key }
        cooldowns.add(Cooldown(
            key,
            now(),
            duration?.toSeconds() ?: key.getCooldownTime()
        ))

        _cooldowns[id] = cooldowns
    }

    /**
     * Remove the cooldown for an entity under a certain key
     * @param id The entity to reset the cooldown for
     * @param key The related cooldown to clear
     */
    fun resetCooldown(id: String, key: Cooldown.Kind) {
        val cooldowns = _cooldowns[id] ?: hashSetOf()
        cooldowns.removeIf { it.key == key && it.isExpired() }
        _cooldowns[id] = cooldowns

        if (cooldowns.isEmpty()) _cooldowns.remove(id)
    }

    /**
     * Clean up any outstanding cooldowns to help manage memory usage
     */
    fun cleanup() {
        for ((id, cooldowns) in _cooldowns) {
            cooldowns.removeIf { it.isExpired() }

            if (cooldowns.isEmpty()) {
                _cooldowns.remove(id)
                continue
            }
        }
    }

}