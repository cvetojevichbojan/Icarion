package xyz.amplituhedron.icarion.mock

import xyz.amplituhedron.icarion.AppUpdateMigration
import xyz.amplituhedron.icarion.IntVersion

open class MockMigration(override val targetVersion: IntVersion) : AppUpdateMigration<IntVersion> {

    var executed = false
    var rollbacked = false

    override suspend fun migrate() {
        executed = true
    }

    override suspend fun rollback() {
        rollbacked = true
    }
}

class FailingMigration(targetVersion: IntVersion) : MockMigration(targetVersion) {
    override suspend fun migrate() {
        throw Exception("Failing migration")
    }
}