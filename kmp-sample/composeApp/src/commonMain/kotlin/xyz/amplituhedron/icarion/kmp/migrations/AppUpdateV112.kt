package xyz.amplituhedron.icarion.kmp.migrations

import xyz.amplituhedron.icarion.AppUpdateMigration
import xyz.amplituhedron.icarion.SemanticVersion

class AppUpdateV112(override val targetVersion: SemanticVersion = SemanticVersion.fromVersion("1.1.2")) :
    AppUpdateMigration<SemanticVersion> {
    override suspend fun migrate() {
        println("Importing images missing exif information")
    }

    override suspend fun rollback() {
    }
}