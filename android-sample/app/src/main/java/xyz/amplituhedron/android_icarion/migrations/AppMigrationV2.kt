package xyz.amplituhedron.android_icarion.migrations

import android.util.Log
import xyz.amplituhedron.icarion.AppUpdateMigration
import xyz.amplituhedron.icarion.IntVersion

class AppMigrationV2(override val targetVersion: IntVersion = IntVersion(2)) : AppUpdateMigration<IntVersion> {
    override suspend fun migrate() {
        Log.i("AppMigrationV2", "Compressing user audio files")
        Log.i("AppMigrationV2", "Setting default shared preferences.")
    }

    override suspend fun rollback() {
    }
}