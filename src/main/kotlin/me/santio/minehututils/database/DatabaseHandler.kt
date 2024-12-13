package me.santio.minehututils.database

import me.santio.minehututils.database.models.Settings
import me.santio.minehututils.iron
import org.flywaydb.core.Flyway
import java.util.*

object DatabaseHandler {

    fun migrate() {
        Flyway.configure()
            .dataSource(iron.pool)
            .locations("classpath:db/migration")
            .sqlMigrationPrefix("")
            .sqlMigrationSeparator("_")
            .outOfOrder(true)
            .load()
            .migrate()
    }

    suspend fun callHooks() {
        val hooks = ServiceLoader.load(DatabaseHook::class.java, this.javaClass.classLoader)
        hooks.forEach { it.onHook() }
    }

    private suspend fun getSettingsNullable(guild: String): Settings? {
        return iron.prepare(
            "SELECT * FROM settings WHERE guild_id = ?",
            guild
        ).singleNullable()
    }

    suspend fun getSettings(guild: String): Settings {
        return getSettingsNullable(guild) ?: createSettings(guild)
    }

    suspend fun createIfNotExists(guild: String) {
        if (getSettingsNullable(guild) == null) {
            createSettings(guild)
        }
    }

    private suspend fun createSettings(guild: String): Settings {
        val settings = Settings(guildId = guild)

        iron.prepare(
            """
                INSERT INTO settings(guild_id, marketplace_channel, marketplace_cooldown, lockdown_role) 
                VALUES (?, ?, ?, ?)
            """.trimIndent(),
            settings.guildId,
            settings.marketplaceChannel,
            settings.marketplaceCooldown,
            settings.lockdownRole
        )

        return settings
    }

}
