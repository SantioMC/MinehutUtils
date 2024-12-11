package me.santio.minehututils.cooldown

/**
 * Represents the different kinds of cooldowns that a user can have. These will be stored in memory as
 * there is really no reason to persist them.
 * @author santio
 */
enum class Cooldown(val humanName: String) {
    MARKET_OFFERING("Marketplace (Offers)"),
    MARKET_REQUESTS("Marketplace (Requests)"),
    ;

    companion object {
        /**
         * Get a marketplace type cooldown from the type provided
         * @param type The listing type, either 'offer' or 'request'
         * @return The cooldown relating to the type, or null if the type is invalid
         */
        fun getMarketplaceType(type: String): Cooldown {
            return when (type) {
                "offer" -> MARKET_OFFERING
                "request" -> MARKET_REQUESTS
                else -> throw IllegalArgumentException("Invalid marketplace type $type")
            }
        }
    }
}
