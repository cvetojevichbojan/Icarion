package xyz.amplituhedron.android_icarion.migrations

import android.app.Application.MODE_PRIVATE
import android.content.Context
import android.content.SharedPreferences
import xyz.amplituhedron.icarion.IntVersion

interface VersionRepository {
    fun getCurrentVersion(): IntVersion
    fun setCurrentVersion(version: IntVersion)
}

class SharedPreferencesVersionRepository private constructor(private val sharedPreferences: SharedPreferences) :
    VersionRepository {

    companion object {
        fun create(context: Context): SharedPreferencesVersionRepository =
            SharedPreferencesVersionRepository(
                context.getSharedPreferences("AppVersion", MODE_PRIVATE)
            )

        private const val VERSION_KEY = "current_version"
    }

    override fun getCurrentVersion(): IntVersion {
        val version = sharedPreferences.getInt(VERSION_KEY, 0) // Default to 0 if not set
        return IntVersion(version)
    }

    override fun setCurrentVersion(version: IntVersion) {
        sharedPreferences.edit().putInt(VERSION_KEY, version.value).apply()
    }
}