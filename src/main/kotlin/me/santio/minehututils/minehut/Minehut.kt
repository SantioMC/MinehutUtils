package me.santio.minehututils.minehut

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.gson.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.santio.minehututils.coroutines.exceptionHandler
import me.santio.minehututils.minehut.mcsrvstat.PingModel
import me.santio.minehututils.minehut.mcstatus.StatusModel
import me.santio.minehututils.scope
import me.santio.sdk.minehut.apis.Minehut
import me.santio.sdk.minehut.models.ListedServer
import me.santio.sdk.minehut.models.PlayerStats
import me.santio.sdk.minehut.models.Server
import me.santio.sdk.minehut.models.SimpleStats
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A wrapper on unirest for accessing the Minehut API
 */
@Suppress("MemberVisibilityCanBePrivate")
object Minehut {

    private const val BASE_URL = "https://api.minehut.com"

    private val logger = LoggerFactory.getLogger(Minehut::class.java)

    @Volatile
    private var serverCache: List<ListedServer>? = null
    private val refreshing = AtomicBoolean(false)
    private var failedRefreshes = 0
    private val client = Minehut(BASE_URL)

    val dailyTimeLimit: Duration = Duration.ofHours(4)

    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            gson()
        }
        install(DefaultRequest) {
            header("User-Agent", "MinehutUtils/1.0")
            header("Accept", "application/json")
        }
    }

    /**
     * Refresh the server list cache. Failures keep the previous cache, and only the first failure of an
     * outage is logged as a warning, so an API outage doesn't produce a stack trace every 30 seconds.
     */
    fun refreshList() {
        if (!refreshing.compareAndSet(false, true)) return // previous refresh is still running

        scope.launch(exceptionHandler) {
            try {
                fetchServers()?.let {
                    serverCache = it
                    if (failedRefreshes > 0) logger.info("Server list refresh recovered after {} failed attempts", failedRefreshes)
                    failedRefreshes = 0
                } ?: refreshFailed("the API returned an unsuccessful response")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                refreshFailed(e.toString())
            } finally {
                refreshing.set(false)
            }
        }
    }

    private fun refreshFailed(reason: String) {
        failedRefreshes++
        if (failedRefreshes == 1) {
            logger.warn("Failed to refresh the server list, keeping the cached list until it recovers: {}", reason)
        } else {
            logger.debug("Failed to refresh the server list ({} attempts): {}", failedRefreshes, reason)
        }
    }

    fun close() {
        httpClient.close()
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
    suspend fun network(): SimpleStats? {
        return client.getNetworkStatistics().takeIf { it.success }?.body()
    }

    /**
     * Get the player statistics and distribution
     * @return The player stats model, or null if the request failed
     */
    suspend fun players(): PlayerStats? {
        return client.getPlayerDistribution().takeIf { it.success }?.body()
    }

    /**
     * Get a single server's information
     * @param name The name of the server
     * @return The server model, or null if the server does not exist
     */
    suspend fun server(name: String): Server? {
        return client.getServer(name, true).takeIf { it.success }?.body()?.server
    }

    /**
     * Get a list of all servers
     * @param bypassCache Whether to bypass the server list cache
     * @return A servers model containing a list of servers along with extra information, or null if the request failed
     */
    suspend fun servers(bypassCache: Boolean = false): List<ListedServer> {
        serverCache?.takeIf { !bypassCache }?.let { return it }

        // An unsuccessful response keeps the previous cache rather than replacing it with nothing
        val servers = fetchServers() ?: return serverCache ?: emptyList()
        serverCache = servers
        return servers
    }

    fun cachedServers(): List<ListedServer> = serverCache ?: emptyList()

    private suspend fun fetchServers(): List<ListedServer>? {
        return client.getServers(
            q = null,
            category = null,
            limit = null
        ).takeIf { it.success }
            ?.body()
            ?.servers
    }

    /**
     * Ping a service
     * @param service The service to ping
     * @return Whether the service is online, or null if the service failed to ping
     */
    suspend fun ping(service: Service): Boolean? = coroutineScope {
        val sources = mapOf(
            "mcsrvstat.us" to suspend { pingMcsrvstatUs(service) },
            "mcstatus.io" to suspend { pingMcstatusIo(service) },
        )

        val results = Channel<Boolean?>(sources.size)
        for ((name, source) in sources) {
            launch {
                results.send(runCatching { source() }.onFailure {
                    if (it is CancellationException) throw it
                    logger.warn("Failed to ping {} through {}: {}", service, name, it.toString())
                }.getOrNull())
            }
        }

        var offline = 0
        repeat(sources.size) {
            when (results.receive()) {
                true -> {
                    coroutineContext.cancelChildren()
                    return@coroutineScope true
                }
                false -> offline++
                null -> {}
            }
        }

        if (offline == sources.size) false else null
    }

    suspend fun pingMcsrvstatUs(service: Service): Boolean? {
        return withContext(Dispatchers.IO) {
            val url = when (service) {
                Service.JAVA, Service.PROXY -> "https://api.mcsrvstat.us/3/minehut.com"
                Service.BEDROCK -> "https://api.mcsrvstat.us/bedrock/3/bedrock.minehut.com"
                else -> return@withContext null
            }

            return@withContext httpClient.get(url)
                .takeIf { it.status.value == 200 }
                ?.body<PingModel>()
                ?.let { it.online || (it.players?.online ?: 0) > 0 }
        }
    }

    suspend fun pingMcstatusIo(service: Service): Boolean? {
        return withContext(Dispatchers.IO) {
            val url = when (service) {
                Service.JAVA, Service.PROXY -> "https://api.mcstatus.io/v2/status/java/minehut.com"
                Service.BEDROCK -> "https://api.mcstatus.io/v2/status/bedrock/bedrock.minehut.com"
                else -> return@withContext null
            }

            return@withContext httpClient.get(url)
                .takeIf { it.status.value == 200 }
                ?.body<StatusModel>()
                ?.let { it.online || (it.players?.online ?: 0) > 0 }
        }
    }

    /**
     * Get the status of core Minehut services
     * @return A map of services to their status
     */
    suspend fun status(): Map<Service, State> = coroutineScope {
        val status = mutableMapOf(
            Service.JAVA to State.ONLINE,
            Service.BEDROCK to State.ONLINE,
            Service.API to State.ONLINE,
            Service.PROXY to State.ONLINE,
        )
        val pings = listOf(Service.PROXY, Service.BEDROCK).associateWith { async { runCatching { ping(it) } } }

        runCatching { players() }.onFailure {
            if (it is CancellationException) throw it
            logger.warn("Failed to fetch the player distribution for the status check: {}", it.toString())
        }.getOrNull().apply {
            if (this == null) {
                status[Service.API] = State.OFFLINE
                return@apply
            }

            if (this.bedrockTotal != null && this.bedrockTotal < 50) status[Service.BEDROCK] = State.DEGRADED
            if (this.bedrockTotal != null && this.bedrockTotal == 0) status[Service.BEDROCK] = State.OFFLINE

            if (this.javaTotal != null && this.javaTotal < 1000) status[Service.JAVA] = State.DEGRADED
            if (this.javaTotal != null && this.javaTotal == 0) status[Service.JAVA] = State.OFFLINE
        }

        for ((service, ping) in pings) {
            ping.await().onFailure {
                if (it is CancellationException) throw it
                logger.warn("Failed to ping {} for the status check: {}", service, it.toString())
            }.getOrNull().apply {
                when (this) {
                    null -> status[service] = State.FAILED
                    false -> status[service] = State.OFFLINE
                    true -> {}
                }
            }
        }

        // TODO: Implement version checking

        status
    }

}
