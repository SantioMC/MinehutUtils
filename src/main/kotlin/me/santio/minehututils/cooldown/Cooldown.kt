package me.santio.minehututils.cooldown

import me.santio.minehututils.database
import me.santio.minehututils.minehut.api.ServerModel
import net.dv8tion.jda.api.entities.Member

enum class Cooldown(private val key: String) {
    ADVERTISE_USER("advert"),
    ADVERTISE_SERVER("advert"),
    MARKET_OFFERING("market-offer"),
    MARKET_REQUESTS("market-request"),
    ;

    /**
     * Gets the user's cooldown relating to the specified [Cooldown]
     * @param user The user to get the cooldown for
     * @return The time the cooldown ends, or null if the cooldown doesn't exist or
     * if the cooldown is not related to a user
     */
    fun get(user: Member): Long? {
        if (this == ADVERTISE_SERVER) return null
        return database.cooldownQueries.getCooldown(user.id, key).executeAsOneOrNull()?.time_end
    }

    /**
     * Gets the server's cooldown relating to the specified [Cooldown]
     * @param server The server to get the cooldown for
     * @return The time the cooldown ends, or null if the cooldown doesn't exist or
     * if the cooldown is not related to a server
     */
    fun get(server: ServerModel): Long? {
        if (this != ADVERTISE_SERVER) return null
        return database.cooldownQueries.getCooldown(server.id, key).executeAsOneOrNull()?.time_end
    }

    /**
     * Sets the user's cooldown relating to the specified [Cooldown]
     * @param user The user to set the cooldown for
     * @param time The time the cooldown ends
     */
    fun set(user: Member, time: Long) {
        if (this == ADVERTISE_SERVER) return
        if (time == 0L) return clear(user)
        database.cooldownQueries.setCooldown(user.id, key, time)
    }

    /**
     * Sets the server's cooldown relating to the specified [Cooldown]
     * @param server The server to set the cooldown for
     * @param time The time the cooldown ends
     */
    fun set(server: ServerModel, time: Long) {
        if (this != ADVERTISE_SERVER) return
        if (time == 0L) return clear(server)
        database.cooldownQueries.setCooldown(server.id, key, time)
    }

    /**
     * Clears the user's cooldown relating to the specified [Cooldown]
     * @param user The user to clear the cooldown for
     */
    fun clear(user: Member) {
        if (this == ADVERTISE_SERVER) return
        database.cooldownQueries.resetCooldown(user.id, key)
    }

    /**
     * Clears the server's cooldown relating to the specified [Cooldown]
     * @param server The server to clear the cooldown for
     */
    fun clear(server: ServerModel) {
        if (this != ADVERTISE_SERVER) return
        database.cooldownQueries.resetCooldown(server.id, key)
    }

    companion object {
        /**
         * Get a marketplace type cooldown from the type provided
         * @param type The listing type, either 'offer' or 'request'
         * @return The cooldown relating to the type, or null if the type is invalid
         */
        fun getMarketplaceType(type: String): Cooldown? {
            return when (type) {
                "offer" -> MARKET_OFFERING
                "request" -> MARKET_REQUESTS
                else -> null
            }
        }
    }
}