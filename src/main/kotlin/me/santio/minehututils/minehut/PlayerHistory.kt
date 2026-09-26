package me.santio.minehututils.minehut

import me.santio.sdk.minehut.models.ListedServer
import kotlin.math.roundToInt

/**
 * Keeps each server's player counts over the last week in memory, as hourly totals and peaks taken from the
 * server list refreshes. Servers missing from a refresh are offline and count as zero players, and servers are
 * only tracked once they have had a player.
 */
object PlayerHistory {

    private const val HOURS = 24 * 7
    private const val MINUTE = 60 * 1000L
    private const val HOUR = 60 * MINUTE

    private class Record {
        val totals = IntArray(HOURS)
        val peaks = ShortArray(HOURS)
        var lastActive = 0L
    }

    /**
     * The player count average and peak over a stretch of time
     * @param average The average player count
     * @param peak The highest player count
     * @param minutes How many minutes of data the numbers cover
     */
    data class Stats(val average: Int, val peak: Int, val minutes: Int)

    private val records = HashMap<String, Record>()
    private val slotHours = LongArray(HOURS) { -1 }
    private val slotSamples = IntArray(HOURS)
    private var firstSample = -1L

    private fun startHour(hour: Long, slot: Int) {
        slotHours[slot] = hour
        slotSamples[slot] = 0
        // Stops tracking servers that had no players for a week
        records.values.removeIf { hour - it.lastActive >= HOURS }
        records.values.forEach {
            it.totals[slot] = 0
            it.peaks[slot] = 0
        }
    }

    /**
     * Records one sample of every listed server's player count
     * @param servers The current server list
     */
    @Synchronized
    fun record(servers: List<ListedServer>, now: Long = System.currentTimeMillis()) {
        val hour = now / HOUR
        val slot = (hour % HOURS).toInt()
        if (slotHours[slot] != hour) startHour(hour, slot)
        if (firstSample < 0) firstSample = now
        slotSamples[slot]++

        for (server in servers) {
            val id = server.staticInfo?.id ?: continue
            val players = server.playerData?.playerCount ?: 0
            val record = records[id] ?: if (players > 0) Record().also { records[id] = it } else continue

            record.totals[slot] += players
            record.peaks[slot] = maxOf(record.peaks[slot].toInt(), players.coerceAtMost(Short.MAX_VALUE.toInt())).toShort()
            if (players > 0) record.lastActive = hour
        }
    }

    /**
     * Gets a server's player count stats over the last few hours, covering less time if the bot started more
     * recently. Untracked servers count as having no players.
     * @param id The id of the server
     * @param hours How many hours to look back, at most a week
     * @return The stats, or null if no server list has been recorded yet
     */
    @Synchronized
    fun stats(id: String, hours: Int, now: Long = System.currentTimeMillis()): Stats? {
        if (firstSample < 0) return null
        val hour = now / HOUR
        val window = minOf(hours, HOURS, (hour - firstSample / HOUR + 1).toInt())
        val record = records[id]
        var total = 0L
        var samples = 0L
        var peak = 0

        for (back in 0 until window) {
            val slot = ((hour - back) % HOURS).toInt()
            if (slotHours[slot] != hour - back) continue

            samples += slotSamples[slot]
            if (record != null) {
                total += record.totals[slot]
                peak = maxOf(peak, record.peaks[slot].toInt())
            }
        }

        if (samples == 0L) return null
        val minutes = minOf(hours * 60L, (now - firstSample) / MINUTE).toInt()
        return Stats((total.toDouble() / samples).roundToInt(), peak, minutes)
    }

    /**
     * Gets the hour of the day, in UTC, when a server has the most players on average over the last week
     * @param id The id of the server
     * @return The hour from 0 to 23, or null if there isn't a day of data or no hour stands out
     */
    @Synchronized
    fun busiestHour(id: String, now: Long = System.currentTimeMillis()): Int? {
        if (firstSample < 0 || now - firstSample < 24 * HOUR) return null
        val record = records[id] ?: return null
        val hour = now / HOUR
        val averages = DoubleArray(24)
        val counts = IntArray(24)

        for (back in 0 until HOURS) {
            val slot = ((hour - back) % HOURS).toInt()
            if (slotHours[slot] != hour - back || slotSamples[slot] == 0) continue

            val hourOfDay = ((hour - back) % 24).toInt()
            averages[hourOfDay] += record.totals[slot].toDouble() / slotSamples[slot]
            counts[hourOfDay]++
        }

        val hours = (0 until 24).filter { counts[it] > 0 }
        val busiest = hours.maxByOrNull { averages[it] / counts[it] } ?: return null

        // Only an hour at least 20% busier than the average counts as the busiest
        val usual = hours.sumOf { averages[it] } / hours.sumOf { counts[it] }
        return busiest.takeIf { averages[it] / counts[it] >= maxOf(usual * 1.2, 1.0) }
    }

}
