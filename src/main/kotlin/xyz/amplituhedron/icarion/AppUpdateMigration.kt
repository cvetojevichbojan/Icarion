package xyz.amplituhedron.icarion

interface AppUpdateMigration<VERSION : Comparable<VERSION>> {
    val targetVersion: VERSION

    /**
     * Migration to [targetVersion]
     */
    suspend fun migrate()

    /**
     * Migration rollback if required
     */
    suspend fun rollback()
}