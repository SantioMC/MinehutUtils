package me.santio.minehututils.utils


import io.github.cdimascio.dotenv.Dotenv

@Suppress("unused")
object EnvUtils {
    private val dotenv = Dotenv.load()

    fun env(key: String, def: String? = null): String {
        return dotenv[key.uppercase()] ?: def ?: throw IllegalStateException("No environment variable for '$key'")
    }

    @Suppress("UNCHECKED_CAST")
    fun <T: Any> env(key: String, def: T): T {
        return dotenv[key.uppercase()] as? T ?: def
    }

    fun env(key: String): String? {
        return dotenv[key.uppercase()]
    }

}