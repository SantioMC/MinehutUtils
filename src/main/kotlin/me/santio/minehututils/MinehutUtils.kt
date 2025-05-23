package me.santio.minehututils

import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.jdabuilder.default
import dev.minn.jda.ktx.jdabuilder.intents
import gg.ingot.iron.Iron
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import me.santio.minehututils.commands.CommandLoader
import me.santio.minehututils.commands.CommandManager
import me.santio.minehututils.database.DatabaseHandler
import me.santio.minehututils.marketplace.MarketplaceListener
import me.santio.minehututils.marketplace.MarketplaceManager
import me.santio.minehututils.minehut.Minehut
import me.santio.minehututils.skript.Skript
import me.santio.minehututils.tags.TagListener
import me.santio.minehututils.utils.EnvUtils.env
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.requests.GatewayIntent
import org.slf4j.LoggerFactory
import java.nio.file.Paths
import java.util.*
import kotlin.concurrent.schedule
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.notExists

lateinit var bot: JDA
lateinit var iron: Iron

val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

suspend fun main() {
    val logger = LoggerFactory.getLogger("MinehutUtils")
    val timer = Timer()

    // Create JDA instance
    bot = default(
        env("TOKEN") ?: throw IllegalStateException("No token provided"),
        true
    ) {
        intents += GatewayIntent.MESSAGE_CONTENT
    }.awaitReady()

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

    bot.addEventListener(MarketplaceListener, TagListener)
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

    // Attach shutdown hooks
    Runtime.getRuntime().addShutdownHook(Thread {
        Minehut.close()
        bot.shutdownNow()
    })
}
