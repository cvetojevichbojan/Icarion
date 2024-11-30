package xyz.amplituhedron.icarion.kmp.migrations

import xyz.amplituhedron.icarion.AppUpdateMigration
import xyz.amplituhedron.icarion.SemanticVersion

class AppUpdateV110(override val targetVersion: SemanticVersion = SemanticVersion.fromVersion("1.1.0")) :
    AppUpdateMigration<SemanticVersion> {
    override suspend fun migrate() {
        println("Moving user created images to external app folders")
    }

    override suspend fun rollback() {
    }
}