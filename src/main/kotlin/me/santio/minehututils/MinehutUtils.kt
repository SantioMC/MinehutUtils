package me.santio.minehututils

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.github.cdimascio.dotenv.Dotenv
import me.santio.coffee.common.Coffee
import me.santio.coffee.jda.CoffeeJDA
import me.santio.minehututils.adapters.DurationAdapter
import me.santio.minehututils.adapters.ServerAdapter
import me.santio.minehututils.minehut.Minehut
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import me.santio.minehututils.db.Minehut as Database

lateinit var bot: JDA
lateinit var database: Database

suspend fun main() {
    // Load .env
    val dotenv = Dotenv.load()

    // Setup database
    val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:data.db")
    Database.Schema.create(driver)
    database = Database(driver)

    // Create JDA instance
    bot = JDABuilder.createDefault(
        dotenv["TOKEN"] ?: throw IllegalStateException("No token provided")
    ).build().awaitReady()

    // Attach command handler
    Coffee.import(CoffeeJDA(bot))
    Coffee.adapter(ServerAdapter, DurationAdapter)
    Coffee.brew("me.santio.minehututils.commands")

    // Start cache refreshes
    Minehut.startTimer()

    // Log user
    println("Logged in as ${bot.selfUser.name}")

    Minehut.status()

    // Attach shutdown hooks
    Runtime.getRuntime().addShutdownHook(Thread {
        Minehut.close()
        bot.shutdownNow()
    })
}