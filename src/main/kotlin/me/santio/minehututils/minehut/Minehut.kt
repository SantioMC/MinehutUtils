package me.santio.minehututils.minehut

import kong.unirest.core.Unirest
import me.santio.minehututils.cooldown.CooldownRegistry
import me.santio.minehututils.minehut.api.*
import java.time.Duration
import java.util.*
import java.util.concurrent.Executors
import kotlin.concurrent.schedule

/**
 * A wrapper on unirest for accessing the Minehut API
 */
@Suppress("MemberVisibilityCanBePrivate")
object Minehut {

    val dailyTimeLimit: Duration = Duration.ofHours(4)
    private var serverCache: ServersModel? = null

    private val client = Unirest.spawnInstance().apply {
        config().addDefaultHeader("Content-Type", "application/json")
        config().addDefaultHeader("Accept", "application/json")
        config().addDefaultHeader("User-Agent", "MinehutUtils/2.0")
        config().connectTimeout(5000)
    }


    /**
     * Refresh the server list cache
     */
    private fun refreshList() {
        Executors.newSingleThreadExecutor().submit {
            serverCache = servers(true)
        }
    }

    /**
     * Start the server list cache timer
     */
    fun startTimer() {
        Timer().schedule(30000) {
            // Update in-memory server cache
            refreshList()

            // Cleanup in-memory cooldown cache
            CooldownRegistry.cleanup()
        }.also { refreshList() }
    }

    /**
     * Close the unirest client
     */
    fun close() {
        client.close()
    }

    /**
     * Gets the epoch time of the next daily time reset
     * @return The epoch time of the next daily time reset
     */
    fun getDailyTimeReset(): Long {
        val reset = Calendar.getInstance(TimeZone.getTimeZone("GMT-8")).apply {
            set(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        if (reset.before(Calendar.getInstance(TimeZone.getTimeZone("GMT-8")))) {
            reset.add(Calendar.DATE, 1)
        }

        return reset.timeInMillis / 1000
    }

    /**
     * Get the network statistics
     * @return The network stats model, or null if the request failed
     */
    fun network(): SimpleStatsModel? {
        val response = client.get("https://api.minehut.com/network/simple_stats").asObject(SimpleStatsModel::class.java)
        return if (response.isSuccess) { response.body } else null
    }

    /**
     * Get the player statistics and distribution
     * @return The player stats model, or null if the request failed
     */
    fun players(): PlayerStatsModel? {
        val response = client.get("https://api.minehut.com/network/players/distribution").asObject(PlayerStatsModel::class.java)
        return if (response.isSuccess) { response.body } else null
    }

    /**
     * Get a single server's information
     * @param name The name of the server
     * @return The server model, or null if the server does not exist
     */
    fun server(name: String): ServerModel? {
        val response = client.get("https://api.minehut.com/server/${name}?byName=true")
            .asObject(ServerResponseModel::class.java)

        return if (response.isSuccess && response.body.ok != false) { response.body.server } else null
    }

    /**
     * Get a list of all servers
     * @param bypassCache Whether to bypass the server list cache
     * @return A servers model containing a list of servers along with extra information, or null if the request failed
     */
    fun servers(bypassCache: Boolean = false): ServersModel? {
        if (!bypassCache) return serverCache
        val response = client.get("https://api.minehut.com/servers").asObject(ServersModel::class.java)
        return if (response.isSuccess) { response.body } else null
    }

    /**
     * Ping a service
     * @param service The service to ping
     * @return The ping model, or null if the service failed to ping
     */
    fun ping(service: Service): PingModel? {
        val url = when (service) {
            Service.JAVA, Service.PROXY -> "https://api.mcsrvstat.us/3/minehut.com"
            Service.BEDROCK -> "https://api.mcsrvstat.us/bedrock/3/bedrock.minehut.com"
            else -> return null
        }

        val response = client.get(url).asObject(PingModel::class.java)
        return if (response.isSuccess) { response.body } else null
    }

    /**
     * Get the status of core Minehut services
     * @return A map of services to their status
     */
    fun status(): Map<Service, State> {
        val status = mutableMapOf(
            Service.JAVA to State.ONLINE,
            Service.BEDROCK to State.ONLINE,
            Service.API to State.ONLINE,
            Service.PROXY to State.ONLINE,
        )

        players().apply {
            if (this == null) {
                status[Service.API] = State.OFFLINE
                return@apply
            }

            if (this.bedrockTotal < 50) status[Service.BEDROCK] = State.DEGRADED
            if (this.bedrockTotal == 0) status[Service.BEDROCK] = State.OFFLINE

            if (this.javaTotal < 1000) status[Service.JAVA] = State.DEGRADED
            if (this.javaTotal == 0) status[Service.JAVA] = State.OFFLINE
        }

        for (service in listOf(Service.PROXY, Service.BEDROCK)) {
            ping(service).apply {
                when {
                    this == null -> status[service] = State.FAILED
                    !online && (players == null || players.online == 0) -> status[service] = State.OFFLINE
                }
            }
        }

        // TODO: Implement version checking

        return status
    }

}