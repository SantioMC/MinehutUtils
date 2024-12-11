package me.santio.minehututils

import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.jdabuilder.default
import dev.minn.jda.ktx.jdabuilder.intents
import gg.ingot.iron.Iron
import me.santio.minehututils.commands.CommandLoader
import me.santio.minehututils.commands.CommandManager
import me.santio.minehututils.database.Migrator
import me.santio.minehututils.minehut.Minehut
import me.santio.minehututils.tags.TagListener
import me.santio.minehututils.tags.TagManager
import me.santio.minehututils.utils.EnvUtils.env
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.requests.GatewayIntent
import org.slf4j.LoggerFactory
import java.nio.file.Paths
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.notExists

lateinit var bot: JDA
lateinit var iron: Iron

suspend fun main() {
    val logger = LoggerFactory.getLogger("MinehutUtils")

    // Create JDA instance
    bot = default(
        env("TOKEN") ?: throw IllegalStateException("No token provided"),
        true
    ) {
        intents += GatewayIntent.MESSAGE_CONTENT
    }.awaitReady()

    // Prepare database
    val databaseUri = env("DATABASE_URI", "jdbc:sqlite:data/minehut.db")
    val path = Paths.get(databaseUri)

    if (path.parent.notExists()) path.parent.createDirectories()
    if (path.notExists()) path.createFile()

    iron = Iron(env("DATABASE_URI", "jdbc:sqlite:data/minehut.db")).connect()
    Migrator.migrate()

    // Preload resources
    TagManager.preload()

    // Attach command handler
    CommandLoader.load()
    bot.updateCommands().addCommands(CommandManager.collect()).queue()

    // Start cache refreshes
    Minehut.startTimer()

    bot.addEventListener(TagListener)
    bot.listener<SlashCommandInteractionEvent> {
        CommandManager.execute(it)
    }

    bot.listener<CommandAutoCompleteInteractionEvent> {
        CommandManager.autoComplete(it)
    }

    // Log user
    logger.info("Logged in as ${bot.selfUser.name}")

    // Attach shutdown hooks
    Runtime.getRuntime().addShutdownHook(Thread {
        Minehut.close()
        bot.shutdownNow()
    })
}
