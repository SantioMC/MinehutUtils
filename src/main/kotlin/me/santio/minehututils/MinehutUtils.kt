package me.santio.minehututils

import io.github.cdimascio.dotenv.Dotenv
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import me.santio.coffee.jda.CoffeeJDA
import me.santio.coffee.common.Coffee
import me.santio.minehututils.adapters.ServerAdapter
import me.santio.minehututils.minehut.Minehut
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder

lateinit var bot: JDA

suspend fun main() = coroutineScope {
    // Load .env
    val dotenv = Dotenv.load()

    // Create JDA instance
    bot = JDABuilder.createDefault(
        dotenv["TOKEN"] ?: throw IllegalStateException("No token provided")
    ).build().awaitReady()

    // Attach command handler
    Coffee.import(CoffeeJDA(bot))
    Coffee.adapter(ServerAdapter)
    Coffee.brew("me.santio.minehututils.commands")

    // Start cache refreshes
    Minehut.startTimer()

    // Log user
    println("Logged in as ${bot.selfUser.name}")

    runBlocking {
        Minehut.status()
    }

    // Attach shutdown hooks
    Runtime.getRuntime().addShutdownHook(Thread {
        Minehut.close()
        bot.shutdownNow()
    })
}