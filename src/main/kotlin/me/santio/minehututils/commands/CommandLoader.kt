package me.santio.minehututils.commands

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Handles discovery and registration of commands.
 * @author santio
 */
object CommandLoader {

    val logger: Logger = LoggerFactory.getLogger(javaClass)
    val loaded = mutableSetOf<SlashCommand>()

    fun load() {
        val loader = ServiceLoader.load(
            SlashCommand::class.java,
            this.javaClass.classLoader
        )

        loader.forEach {
            CommandManager.register(it)
            loaded.add(it)
        }

        logger.info("Discovered and registered {} commands", loaded.size)
    }

    inline fun <reified C : SlashCommand> get(): C {
        return loaded.first { it is C } as C
    }

}
