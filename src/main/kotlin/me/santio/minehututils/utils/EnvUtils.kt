package me.santio.minehututils.utils

import io.github.cdimascio.dotenv.Dotenv

@Suppress("unused")
object EnvUtils {
    private val dotenv = Dotenv.configure().ignoreIfMissing().load()

    fun env(key: String, def: String? = null): String {
        return env(key) ?: def ?: throw IllegalStateException("Missing environment variable $key")
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> env(key: String, def: T): T {
        return env(key) as? T ?: def
    }

    fun env(key: String): String? {
        return dotenv[key.uppercase()]
            ?: System.getenv(key.uppercase())
    }

}
