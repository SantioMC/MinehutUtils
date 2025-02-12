package me.santio.minehututils.skript

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import kotlinx.coroutines.launch
import me.santio.minehututils.scope
import me.santio.minehututils.skript.models.SkriptExample
import me.santio.minehututils.skript.models.SkriptSyntax
import me.santio.minehututils.utils.EnvUtils.env
import me.santio.minehututils.utils.TextHelper.titlecase
import org.slf4j.LoggerFactory

/**
 * A wrapper on the SkriptHub API
 * @author tarna
 */
@Suppress("DuplicatedCode")
object Skript {

    private val logger = LoggerFactory.getLogger(Skript::class.java)
    private val syntaxList = mutableListOf<SkriptSyntax>()
    private val exampleList = mutableListOf<SkriptExample>()

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            gson()
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 1000 * 60 * 10
            connectTimeoutMillis = 1000 * 60 * 10
            socketTimeoutMillis = 1000 * 60 * 10
        }

        defaultRequest {
            header("User-Agent", "MinehutUtils/1.0")
            header("Accept", "application/json")

            env("SKRIPTHUB_KEY")?.let {
                header("Authorization", "Token $it")
            }

            url("https://skripthub.net/api/v1/")
        }
    }

    /**
     * Refresh the syntax list, and cache it
     */
    fun refreshData() {
        if (env("SKRIPTHUB_KEY") == null) return

        scope.launch {
            // Fetch syntax list
            val list = httpClient.get("syntax")
                .takeIf { it.status.isSuccess() }
                ?.body<List<SkriptSyntax>>() ?: emptyList()

            if (list.isEmpty()) {
                logger.warn("The SkriptHub API returned an empty list, sticking with local cache")
                return@launch
            }

            syntaxList.clear()
            syntaxList.addAll(
                list
                    .sortedBy { it.title }
                    .onEach { it.title = it.title.titlecase() }
            )

            // Fetch example list
            val examples = httpClient.get("syntaxexample")
                .takeIf { it.status.isSuccess() }
                ?.body<List<SkriptExample>>() ?: emptyList()

            if (examples.isEmpty()) {
                logger.warn("The SkriptHub API returned an empty list, sticking with local cache")
                return@launch
            }

            exampleList.clear()
            exampleList.addAll(examples)
        }
    }

    /**
     * Search the syntax list for a query
     * @param query The query to search for
     * @return A list of syntaxes that match the query
     */
    fun search(query: String): List<SkriptSyntax> {
        return syntaxList
            .distinctBy { it.title }
            .filter { it.title.contains(query, ignoreCase = true) }
    }

    /**
     * Search the syntax list for a query by id
     * @param id The id of the syntax
     * @return The syntax, or null if it doesn't exist
     */
    fun search(id: Long): List<SkriptSyntax> {
        return syntaxList.filter { it.id.toString().startsWith(id.toString()) }
    }

    /**
     * Get a syntax by its title
     * @param title The title of the syntax
     * @return The syntax, or null if it doesn't exist
     */
    fun get(title: String): SkriptSyntax? {
        return syntaxList.find { it.title == title && it.addon == "Skript" }
            ?: syntaxList.find { it.title == title }
    }

    /**
     * Get a syntax by its id
     * @param id The id of the syntax
     * @return The syntax, or null if it doesn't exist
     */
    fun get(id: Long): SkriptSyntax? {
        return syntaxList.find { it.id == id }
    }

    /**
     * Get the best syntax example for a syntax element
     * @param id The id of the syntax element
     * @return The best example for the syntax element, or null if none exist
     */
    fun getExample(id: Long): SkriptExample? {
        return exampleList.filter { it.syntaxElement == id }
            .maxByOrNull { it.score }
    }

}
