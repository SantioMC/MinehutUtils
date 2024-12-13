package me.santio.minehututils.database

/**
 * An auto-service interface for database hooks
 * @author santio
 */
interface DatabaseHook {

    /**
     * Called once the database is ready to be used
     */
    suspend fun onHook()

}
