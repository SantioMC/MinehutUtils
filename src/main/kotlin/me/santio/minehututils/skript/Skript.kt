package me.santio.minehututils.skript

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import me.santio.minehututils.httpClient
import me.santio.minehututils.scope
import me.santio.minehututils.utils.EnvUtils.env

object Skript {

    private const val BASE_URL = "https://skripthub.net/api/v1"

    val syntaxList = mutableListOf<SkriptSyntax>()

    fun refreshSyntaxList() {
        syntaxList.clear()
        scope.launch {
            val key = env("SKRIPTHUB_KEY") ?: error("No SkriptHub key provided")
            syntaxList.addAll(httpClient.get("$BASE_URL/syntax") {
                header(HttpHeaders.Authorization, "Token $key")
            }
                .takeIf { it.status.isSuccess() }
                ?.body<List<SkriptSyntax>>() ?: emptyList())
        }
    }

    fun search(query: String): List<SkriptSyntax> {
        return syntaxList.filter { it.title.contains(query, ignoreCase = true) }
    }

}
