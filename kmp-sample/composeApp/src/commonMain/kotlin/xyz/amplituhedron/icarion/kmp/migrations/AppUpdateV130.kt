package xyz.amplituhedron.icarion.kmp.migrations

import xyz.amplituhedron.icarion.AppUpdateMigration
import xyz.amplituhedron.icarion.SemanticVersion

class AppUpdateV130(override val targetVersion: SemanticVersion = SemanticVersion.fromVersion("1.3.0")) :
    AppUpdateMigration<SemanticVersion> {
    override suspend fun migrate() {
        println("Moving settings configuration to Firebase remote config")
    }

    override suspend fun rollback() {
    }
}