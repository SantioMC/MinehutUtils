package me.santio.minehututils.minehut

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.gson.*
import kotlinx.coroutines.*
import me.santio.minehututils.coroutines.exceptionHandler
import me.santio.minehututils.minehut.mcsrvstat.PingModel
import me.santio.sdk.minehut.apis.Minehut
import me.santio.sdk.minehut.models.ListedServer
import me.santio.sdk.minehut.models.PlayerStats
import me.santio.sdk.minehut.models.Server
import me.santio.sdk.minehut.models.SimpleStats
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*
import kotlin.concurrent.schedule

/**
 * A wrapper on unirest for accessing the Minehut API
 */
@Suppress("MemberVisibilityCanBePrivate")
object Minehut {

    private val logger = LoggerFactory.getLogger(Minehut::class.java)
    private val timer = Timer()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serverCache: List<ListedServer>? = null
    private val client = Minehut("https://api.minehut.com")

    val dailyTimeLimit: Duration = Duration.ofHours(4)
    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            gson()
        }
    }

    /**
     * Refresh the server list cache
     */
    // todo: dont do this
    private fun refreshList() {
        scope.launch(exceptionHandler) {
            serverCache = servers(true)
        }
    }

    /**
     * Start the server list cache timer
     */
    fun startTimer() {
        timer.schedule(0, 30000) {
            refreshList()
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
        if (!bypassCache && serverCache != null) return serverCache!!

        val servers = client.getServers(
            q = null,
            category = null,
            limit = null
        ).takeIf { it.success }
            ?.body()
            ?.servers
            ?: emptyList()

        serverCache = servers
        return servers
    }

    /**
     * Ping a service
     * @param service The service to ping
     * @return The ping model, or null if the service failed to ping
     */
    suspend fun ping(service: Service): PingModel? {
        return withContext(Dispatchers.IO) {
            val url = when (service) {
                Service.JAVA, Service.PROXY -> "https://api.mcsrvstat.us/3/minehut.com"
                Service.BEDROCK -> "https://api.mcsrvstat.us/bedrock/3/bedrock.minehut.com"
                else -> return@withContext null
            }

            return@withContext httpClient.get(url)
                .takeIf { it.status.value == 200 }
                ?.body<PingModel>()
        }
    }

    /**
     * Get the status of core Minehut services
     * @return A map of services to their status
     */
    suspend fun status(): Map<Service, State> {
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

            if (this.bedrockTotal != null && this.bedrockTotal < 50) status[Service.BEDROCK] = State.DEGRADED
            if (this.bedrockTotal != null && this.bedrockTotal == 0) status[Service.BEDROCK] = State.OFFLINE

            if (this.javaTotal != null && this.javaTotal < 1000) status[Service.JAVA] = State.DEGRADED
            if (this.javaTotal != null && this.javaTotal == 0) status[Service.JAVA] = State.OFFLINE
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
