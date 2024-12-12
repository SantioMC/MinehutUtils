package me.santio.minehututils.resolvers

import java.time.Duration
import kotlin.time.toJavaDuration

/**
 * Handles parsing a string to a duration
 */
object DurationResolver {

    /**
     * Converts a string like '1D 2M3s' to a duration
     * @param query The string to parse
     * @return The duration or null if invalid
     */
    fun from(query: String): Duration? {
        val input = query.trim().replace(" ", "")
            .lowercase()

        val parts = Regex("(\\d+)([smhd])")
            .findAll(input)
            .map { it.groupValues.drop(1) }
            .map { Pair(it[0].toLong(), it[1].lowercase()) }
            .toList()

        if (parts.isEmpty()) return null
        var duration = Duration.ZERO

        for (part in parts) {
            val amount = part.first
            val unit = part.second

            duration = duration.plus(
                when (unit) {
                    "s" -> Duration.ofSeconds(amount)
                    "m" -> Duration.ofMinutes(amount)
                    "h" -> Duration.ofHours(amount)
                    "d" -> Duration.ofDays(amount)
                    else -> return null
                }
            )
        }

        return duration
    }

    /**
     * Converts a duration to a pretty string
     * @param duration The duration to convert
     * @return The pretty string, in the format of '1 day 2 hours 3 minutes 4 seconds'
     */
    fun pretty(duration: Duration): String {
        if (duration.isZero) return "0 seconds"

        val days = duration.toDaysPart().toInt()
        val hours = duration.toHoursPart()
        val minutes = duration.toMinutesPart()
        val seconds = duration.toSecondsPart()

        return buildString {
            if (days > 0) append(asPlural(days, "day"))
            if (hours > 0) append(asPlural(hours, "hour"))
            if (minutes > 0) append(asPlural(minutes, "minute"))
            if (seconds > 0) append(asPlural(seconds, "second"))
        }.trim()
    }

    /**
     * Converts a Kotlin duration to a pretty string
     * @param duration The duration to convert
     * @return The pretty string, in the format of '1 day 2 hours 3 minutes 4 seconds'
     */
    fun pretty(duration: kotlin.time.Duration): String {
        return pretty(duration.toJavaDuration())
    }

    /**
     * An extension function for converting a duration to discord's time format
     */
    fun Duration.discord(relative: Boolean = false): String {
        val time = this.toSeconds() + (System.currentTimeMillis() / 1000)

        return if (relative) "<t:$time:R>"
        else "<t:$time>"
    }

    private fun asPlural(amount: Int, singular: String): String {
        return "$amount ${if (amount > 1) "${singular}s" else singular} "
    }

}
