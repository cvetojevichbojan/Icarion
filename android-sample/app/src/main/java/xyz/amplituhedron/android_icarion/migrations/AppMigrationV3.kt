package xyz.amplituhedron.android_icarion.migrations

import android.util.Log
import xyz.amplituhedron.icarion.AppUpdateMigration
import xyz.amplituhedron.icarion.IntVersion

class AppMigrationV3(override val targetVersion: IntVersion = IntVersion(3)) : AppUpdateMigration<IntVersion> {
    override suspend fun migrate() {
        Log.i("AppMigrationV2", "Introducing user settings via Firebase Config.")
        //Mock failure
        throw RuntimeException("Maybe Firebase config is unavailable in this users country")
    }

    override suspend fun rollback() {
    }
}