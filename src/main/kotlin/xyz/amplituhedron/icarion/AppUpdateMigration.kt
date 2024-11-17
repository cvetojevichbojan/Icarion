package xyz.amplituhedron.icarion

interface AppUpdateMigration<VERSION : Comparable<VERSION>> {
    val targetVersion: VERSION

    /**
     * Migration to [targetVersion]
     */
    fun migrate()

    /**
     * Migration rollback if required
     */
    fun rollback()
}