package me.santio.minehututils.database

import me.santio.minehututils.iron
import org.flywaydb.core.Flyway

object Migrator {

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

}
