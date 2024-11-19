package xyz.amplituhedron.icarion

interface AppUpdateMigration<VERSION : Comparable<VERSION>> {
    val targetVersion: VERSION

    /**
     * Migration to [targetVersion]
     *
     * Intentionally failed migrations should throw an exception here, for ex. throw RuntimeException("Can not migrate all data to external storage..")
     */
    suspend fun migrate()

    /**
     * Migration rollback if required
     */
    suspend fun rollback()
}