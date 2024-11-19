package xyz.amplituhedron.android_icarion.migrations

import android.util.Log
import xyz.amplituhedron.icarion.AppUpdateMigration
import xyz.amplituhedron.icarion.IntVersion

class AppMigrationV7(override val targetVersion: IntVersion = IntVersion(7)) : AppUpdateMigration<IntVersion> {
    override suspend fun migrate() {
        Log.i("AppMigrationV7", "Downloading audio expansion pack for drum sets")
    }

    override suspend fun rollback() {
    }
}