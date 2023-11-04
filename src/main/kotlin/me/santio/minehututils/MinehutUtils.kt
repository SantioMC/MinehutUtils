package me.santio.minehututils

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import me.santio.coffee.common.Coffee
import me.santio.coffee.jda.CoffeeJDA
import me.santio.minehututils.adapters.DurationAdapter
import me.santio.minehututils.adapters.ServerAdapter
import me.santio.minehututils.minehut.Minehut
import me.santio.minehututils.utils.EnvUtils.env
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import me.santio.minehututils.db.Minehut as Database

lateinit var bot: JDA
lateinit var database: Database

fun main() {
    // Setup database
    val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:data.db")
    Database.Schema.create(driver)
    database = Database(driver)

    // Create JDA instance
    bot = JDABuilder.createDefault(
        env("TOKEN") ?: throw IllegalStateException("No token provided")
    ).build().awaitReady()

    // Attach command handler
    bot.updateCommands().complete()
    Coffee.import(CoffeeJDA(bot))
    Coffee.adapter(ServerAdapter, DurationAdapter)
    Coffee.brew("me.santio.minehututils.commands")

    // Start cache refreshes
    Minehut.startTimer()

    // Log user
    println("Logged in as ${bot.selfUser.name}")

    // Attach shutdown hooks
    Runtime.getRuntime().addShutdownHook(Thread {
        Minehut.close()
        bot.shutdownNow()
    })
}