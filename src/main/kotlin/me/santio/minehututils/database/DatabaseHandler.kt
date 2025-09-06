package me.santio.minehututils.database

import me.santio.minehututils.database.models.GuildData
import me.santio.minehututils.database.models.Settings
import me.santio.minehututils.iron
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import java.util.*

object DatabaseHandler {

    private val logger = LoggerFactory.getLogger(DatabaseHandler::class.java)

    fun migrate() = runCatching {
        Flyway.configure()
            .dataSource(iron.pool)
            .locations("classpath:db/migration")
            .sqlMigrationPrefix("")
            .sqlMigrationSeparator("_")
            .outOfOrder(true)
            .load()
            .migrate()
    }.getOrElse { err ->
        logger.error("Failed to run flyway migrations!", err)
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
        if (getDataNullable(guild) == null) {
            createData(guild)
        }
    }

    private suspend fun createSettings(guild: String): Settings {
        val settings = Settings(guildId = guild)

        iron.prepare(
            """
                INSERT INTO settings(guild_id, marketplace_channel, marketplace_cooldown, lockdown_role, booster_pass_role, max_booster_passes) 
                VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            settings.guildId,
            settings.marketplaceChannel,
            settings.marketplaceCooldown,
            settings.lockdownRole,
            settings.boosterPassRole,
            settings.maxBoosterPasses
        )

        return settings
    }

    suspend fun getDataNullable(guild: String): GuildData? {
        return iron.prepare(
            "SELECT * FROM guild_data WHERE guild_id = ?",
            guild
        ).singleNullable()
    }

    suspend fun getData(guild: String): GuildData {
        return getDataNullable(guild) ?: createData(guild)
    }

    private suspend fun createData(guild: String): GuildData {
        val data = GuildData(guildId = guild)

        iron.prepare(
            """
                INSERT INTO guild_data(guild_id, sticky_message) 
                VALUES (?, ?)
            """.trimIndent(),
            data.guildId,
            data.stickyMessage
        )

        return data
    }

}
