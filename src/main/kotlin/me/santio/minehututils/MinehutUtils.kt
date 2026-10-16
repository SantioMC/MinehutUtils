package me.santio.minehututils

import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.jdabuilder.default
import gg.ingot.iron.Iron
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.sentry.Sentry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.santio.minehututils.commands.CommandLoader
import me.santio.minehututils.commands.CommandManager
import me.santio.minehututils.cooldown.CooldownManager
import me.santio.minehututils.coroutines.exceptionHandler
import me.santio.minehututils.database.DatabaseHandler
import me.santio.minehututils.marketplace.MarketplaceListener
import me.santio.minehututils.marketplace.MarketplaceManager
import me.santio.minehututils.minehut.Minehut
import me.santio.minehututils.resolvers.DurationResolver
import me.santio.minehututils.skript.Skript
import me.santio.minehututils.sticky.StickyListener
import me.santio.minehututils.sticky.StickyManager
import me.santio.minehututils.utils.EnvUtils.env
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.slf4j.LoggerFactory
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.schedule
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.notExists

lateinit var bot: JDA
lateinit var iron: Iron

private val logger = LoggerFactory.getLogger("MinehutUtils")
private val timer = Timer()
val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

suspend fun main() {
    Sentry.init { it.dsn = env("SENTRY_DSN", "") }

    // Create JDA instance
    bot = default(
        env("TOKEN") ?: throw IllegalStateException("No token provided"),
        true
    ).awaitReady()

    // Start heartbeating
    startHeartbeat()

    // Prepare database
    val databaseUri = env("DATABASE_URI", "jdbc:sqlite:data/minehut.db")
    val path = Paths.get(databaseUri.substringAfterLast(":"))

    if (path.parent.notExists()) path.parent.createDirectories()
    if (path.notExists()) path.createFile()

    iron = Iron(env("DATABASE_URI", "jdbc:sqlite:data/minehut.db")).connect()
    DatabaseHandler.migrate()
    DatabaseHandler.callHooks()

    // Attach command handler
    CommandLoader.load(bot)
    bot.updateCommands().addCommands(CommandManager.collect()).queue()

    bot.addEventListener(MarketplaceListener, StickyListener)
    bot.listener<SlashCommandInteractionEvent> {
        CommandManager.execute(it)
    }

    bot.listener<CommandAutoCompleteInteractionEvent> {
        CommandManager.autoComplete(it)
    }

    // Log user
    logger.info("Logged in as ${bot.selfUser.name}")

    // Start cache refreshes
    timer.schedule(0, 1000 * 30) { // 30 seconds
        Minehut.refreshList()
    }

    timer.schedule(0, 1000 * 60 * 60 * 24) { // 24 hours
        Skript.refreshData()
    }

    timer.schedule(0, 1000 * 60 * 60 * 24) { // 24 hours
        MarketplaceManager.clearOldMessages()
    }

    timer.schedule(0, 1000 * 5) { // 5 seconds
        StickyManager.refreshSticky()
    }

    CooldownManager.start()

    // Attach shutdown hooks
    Runtime.getRuntime().addShutdownHook(Thread {
        Minehut.close()
        bot.shutdownNow()
    })
}

private fun startHeartbeat() {
    val httpMethod = HttpMethod.parse(env("HEARTBEAT_METHOD", "GET").uppercase())

    val url = env("HEARTBEAT_URL")
        ?: return logger.info("MinehutUtils is running without heartbeats, no status will be reported")

    val interval = DurationResolver.from(env("HEARTBEAT_INTERVAL", "30s"))
        ?: error("Unknown heartbeat resolver")

    val client = HttpClient(CIO) {}
    val sending = AtomicBoolean(false)
    var failures = 0

    timer.schedule(0, interval.toMillis()) {
        if (!sending.compareAndSet(false, true)) return@schedule // previous heartbeat is still running

        scope.launch(exceptionHandler) {
            try {
                val response = client.request(url) { method = httpMethod }
                if (!response.status.isSuccess()) error("Heartbeat returned ${response.status}")

                if (failures > 0) logger.info("Heartbeat recovered after {} failed attempts", failures)
                failures = 0
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Only warn once per outage, the heartbeat runs every few seconds
                if (++failures == 1) logger.warn("Failed to send a heartbeat, retrying until it recovers: {}", e.toString())
                else logger.debug("Failed to send a heartbeat ({} attempts): {}", failures, e.toString())
            } finally {
                sending.set(false)
            }
        }
    }
}
