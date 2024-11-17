package xyz.amplituhedron.icarion

import xyz.amplituhedron.icarion.log.IcarionLoggerAdapter

/**
 * Observer with callback to monitor inidividual migration progress
 */
interface IcarionMigrationObserver<VERSION> {
    fun onMigrationStart(version: VERSION)
    fun onMigrationSuccess(version: VERSION)
    fun onMigrationFailure(version: VERSION, exception: Exception): IcarionFailureRecoveryHint
}

sealed class IcarionMigrationsResult {
    data class Success<VERSION>(val completedMigrations: List<VERSION>, val skippedMigrations: List<VERSION>) :
        IcarionMigrationsResult()

    data class Failure<VERSION>(
        val completedNotRolledBackMigrations: List<VERSION>,
        val skippedMigrations: List<VERSION>,
        val rolledBackMigrations: List<VERSION>
    ) :
        IcarionMigrationsResult()

    data object AlreadyRunning : IcarionMigrationsResult()
}

/**
 * Indicates what to do in case of a failed migration
 */
sealed class IcarionFailureRecoveryHint {
    data object Skip : IcarionFailureRecoveryHint()
    data object Rollback : IcarionFailureRecoveryHint()
    data object Abort : IcarionFailureRecoveryHint()
}

class IcarionMigrator<VERSION : Comparable<VERSION>> {

    @Volatile
    var migrationObserver: IcarionMigrationObserver<VERSION>? = null
    @Volatile
    var defaultFailureRecoveryHint = IcarionFailureRecoveryHint.Abort

    @Volatile
    private var migrationsRunning = false

    private val migrations = mutableSetOf<AppUpdateMigration<VERSION>>()

    @Throws(IllegalStateException::class, IllegalArgumentException::class)
    fun registerMigration(migration: AppUpdateMigration<VERSION>) {
        if (migrationsRunning) {
            throw IllegalStateException("Cannot register migrations while migrations are running.")
        }
        if (migrations.any { it.targetVersion == migration.targetVersion }) {
            throw IllegalArgumentException("A migration targeting version ${migration.targetVersion} is already registered.")
        }

        migrations.add(migration)
    }

    fun executeMigrations(fromVersion: VERSION, toVersion: VERSION): IcarionMigrationsResult {
        if (migrationsRunning) {
            return IcarionMigrationsResult.AlreadyRunning
        }
        migrationsRunning = true
        val completedMigrations = mutableSetOf<AppUpdateMigration<VERSION>>()
        val skippedMigrations = mutableSetOf<AppUpdateMigration<VERSION>>()

        migrations
            .filter { it.targetVersion > fromVersion && it.targetVersion <= toVersion }
            .sortedBy { it.targetVersion }
            .forEach { migration ->
                migrationObserver?.onMigrationStart(migration.targetVersion)
                try {
                    migration.migrate()
                    completedMigrations.add(migration)
                    migrationObserver?.onMigrationSuccess(migration.targetVersion)
                } catch (e: Exception) {
                    IcarionLoggerAdapter.e(e, "Failed migration ${migration.targetVersion}")
                    val recoveryHint =
                        migrationObserver?.onMigrationFailure(migration.targetVersion, e) ?: defaultFailureRecoveryHint

                    IcarionLoggerAdapter.i("Recovery hint for ${migration.targetVersion} is $recoveryHint")

                    when (recoveryHint) {
                        is IcarionFailureRecoveryHint.Skip -> {
                            IcarionLoggerAdapter.i("Skipping migration: ${migration.targetVersion}")

                            skippedMigrations.add(migration)
                        }

                        is IcarionFailureRecoveryHint.Abort -> {
                            IcarionLoggerAdapter.i("Aborting on migration: ${migration.targetVersion}")

                            return IcarionMigrationsResult.Failure(
                                completedNotRolledBackMigrations = completedMigrations.toList(),
                                skippedMigrations = skippedMigrations.toList(),
                                rolledBackMigrations = listOf()
                            )
                        }

                        is IcarionFailureRecoveryHint.Rollback -> {
                            IcarionLoggerAdapter.i("Rollback requested from failed migration: ${migration.targetVersion}")

                            val rolledBackMigrations = executeRollback(completedMigrations.toList())

                            return IcarionMigrationsResult.Failure(
                                completedNotRolledBackMigrations = (completedMigrations - rolledBackMigrations).toList(),
                                skippedMigrations = skippedMigrations.toList(),
                                rolledBackMigrations = rolledBackMigrations.toList()
                            )
                        }
                    }
                }
            }

        migrationsRunning = false

        return IcarionMigrationsResult.Success(completedMigrations.toList(), skippedMigrations.toList())
    }

    private fun executeRollback(completedMigrations: List<AppUpdateMigration<VERSION>>): Set<AppUpdateMigration<VERSION>> {
        val rolledBackMigrations = mutableSetOf<AppUpdateMigration<VERSION>>()
        completedMigrations.reversed().forEach {
            try {
                it.rollback()
                rolledBackMigrations.add(it)
            } catch (e: Exception) {
                IcarionLoggerAdapter.e(e, "Unable to rollback migration ${it.targetVersion}")
                IcarionLoggerAdapter.i("Stopping rollback mechanism.")
                return rolledBackMigrations
            }
        }

        IcarionLoggerAdapter.i("Rolled back all completed migrations: $rolledBackMigrations")
        return rolledBackMigrations
    }

}