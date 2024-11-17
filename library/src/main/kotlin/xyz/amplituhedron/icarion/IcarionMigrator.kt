package xyz.amplituhedron.icarion

import xyz.amplituhedron.icarion.log.IcarionLogger
import xyz.amplituhedron.icarion.log.IcarionLoggerAdapter

/**
 * Observer with callback to monitor inidividual migration progress
 */
interface IcarionMigrationObserver<VERSION> {
    fun onMigrationStart(version: VERSION)
    fun onMigrationSuccess(version: VERSION)
    fun onMigrationFailure(version: VERSION, exception: Exception): IcarionFailureRecoveryHint
}

/**
 * Execute migrations detailed report in a result class
 */
sealed class IcarionMigrationsResult<VERSION> {
    data class Success<VERSION>(val completedMigrations: List<VERSION>, val skippedMigrations: List<VERSION>) :
        IcarionMigrationsResult<VERSION>()

    data class Failure<VERSION>(
        val completedNotRolledBackMigrations: List<VERSION>,
        val skippedMigrations: List<VERSION>,
        val rolledBackMigrations: List<VERSION>
    ) : IcarionMigrationsResult<VERSION>()

    data class AlreadyRunning<VERSION>(private val running: Boolean = true) : IcarionMigrationsResult<VERSION>()
}

/**
 * Indicates what to do in case of a failed migration
 */
sealed class IcarionFailureRecoveryHint {
    /**
     * Skip failed migration and continue
     */
    data object Skip : IcarionFailureRecoveryHint()

    /**
     * Rollback successful migrations
     */
    data object Rollback : IcarionFailureRecoveryHint()

    /**
     * Abort the migration process
     */
    data object Abort : IcarionFailureRecoveryHint()
}

/**
 * A class responsible for managing and executing migrations between different versions.
 * The `IcarionMigrator` ensures that migrations are run sequentially and allows developers
 * to register, execute, and handle migration failures with customizable recovery hints.
 *
 * This class is designed to support migrations for any version type that implements the
 * `Comparable` interface, enabling flexible versioning strategies (e.g., integer-based or semantic versioning).
 *
 * Example usage:
 *
 * 1. **IntVersion Example:**
 *
 * ```kotlin
 * val migrator = IcarionMigrator<Int>()
 *
 * val migrationV1toV2 = AppUpdateMigration(fromVersion = 1, targetVersion = 2) {
 *     // Migration logic for version 1 to version 2
 * }
 * migrator.registerMigration(migrationV1toV2)
 *
 * // Execute migrations from version 1 to version 3
 * val result = migrator.executeMigrations(fromVersion = 1, toVersion = 3)
 * println(result)  // Output will depend on the success or failure of migrations
 * ```
 *
 * 2. **SemanticVersion Example:**
 *
 * ```kotlin
 * data class SemanticVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<SemanticVersion> {
 *     override fun compareTo(other: SemanticVersion): Int {
 *         return compareValuesBy(this, other, SemanticVersion::major, SemanticVersion::minor, SemanticVersion::patch)
 *     }
 * }
 *
 * val migrator = IcarionMigrator<SemanticVersion>()
 *
 * val migrationV1_0_to_V1_1 = AppUpdateMigration(fromVersion = SemanticVersion(1, 0, 0), targetVersion = SemanticVersion(1, 1, 0)) {
 *     // Migration logic from version 1.0.0 to 1.1.0
 * }
 * migrator.registerMigration(migrationV1_0_to_V1_1)
 *
 * // Execute migrations from version 1.0.0 to 2.0.0
 * val result = migrator.executeMigrations(
 *     fromVersion = SemanticVersion(1, 0, 0),
 *     toVersion = SemanticVersion(2, 0, 0)
 * )
 * println(result)  // Output will depend on the success or failure of migrations
 * ```
 *
 * ### Migration Execution Flow:
 *
 * - The `executeMigrations` method ensures migrations are executed only if no migrations are currently running.
 * - Migrations are executed in ascending order based on the version.
 * - Migration failures are handled with customizable recovery hints, such as:
 *   - `IcarionFailureRecoveryHint.Skip` (skip the migration and continue)
 *   - `IcarionFailureRecoveryHint.Abort` (stop the migration process immediately)
 *   - `IcarionFailureRecoveryHint.Rollback` (roll back completed migrations)
 *
 * ### Migration Registration:
 *
 * Developers can register individual migrations using the `registerMigration` method. Each migration must have a unique `targetVersion` to avoid duplication.
 *
 * ### Example of handling migration failure:
 *
 * If a migration fails, the `onMigrationFailure` callback of the `IcarionMigrationObserver` will be invoked.
 * You can define custom recovery behavior within this callback:
 *
 * ```kotlin
 * class MyMigrationObserver : IcarionMigrationObserver<SemanticVersion> {
 *     override fun onMigrationStart(version: SemanticVersion) {
 *         println("Starting migration to version $version")
 *     }
 *
 *     override fun onMigrationSuccess(version: SemanticVersion) {
 *         println("Successfully migrated to version $version")
 *     }
 *
 *     override fun onMigrationFailure(version: SemanticVersion, exception: Exception): IcarionFailureRecoveryHint {
 *         println("Migration to version $version failed: ${exception.message}")
 *         // Custom logic to decide recovery
 *         return IcarionFailureRecoveryHint.Rollback
 *     }
 * }
 *
 * val migrator = IcarionMigrator<SemanticVersion>()
 * migrator.migrationObserver = MyMigrationObserver()
 * ```
 *
 * @param VERSION The type of versioning to use, which must implement the `Comparable` interface.
 */
class IcarionMigrator<VERSION : Comparable<VERSION>> {

    /**
     * Observer to monitor migration events
     */
    @Volatile
    var migrationObserver: IcarionMigrationObserver<VERSION>? = null

    /**
     * Default recovery hint if not observer is specified
     */
    @Volatile
    var defaultFailureRecoveryHint: IcarionFailureRecoveryHint = IcarionFailureRecoveryHint.Abort

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

    private fun getEligibleMigrations(fromVersion: VERSION, toVersion: VERSION): List<AppUpdateMigration<VERSION>> {
        return migrations
            .filter { it.targetVersion > fromVersion && it.targetVersion <= toVersion }
            .sortedBy { it.targetVersion }
    }

    suspend fun executeMigrations(fromVersion: VERSION, toVersion: VERSION): IcarionMigrationsResult<VERSION> {
        IcarionLoggerAdapter.i("Requesting migration from $fromVersion to $toVersion")

        if (migrationsRunning) {
            IcarionLoggerAdapter.i("Migrations unavailable because IcarionMigrationsResult.AlreadyRunning")

            return IcarionMigrationsResult.AlreadyRunning()
        }

        migrationsRunning = true

        val completedMigrations = mutableSetOf<AppUpdateMigration<VERSION>>()
        val skippedMigrations = mutableSetOf<AppUpdateMigration<VERSION>>()

        val eligibleMigrations = getEligibleMigrations(fromVersion, toVersion)
        IcarionLoggerAdapter.i("Found ${eligibleMigrations.size} eligibleMigrations")

        eligibleMigrations.forEach { migration ->
            IcarionLoggerAdapter.d("Running migration ${migration.targetVersion}")

            migrationObserver?.onMigrationStart(migration.targetVersion)
            try {
                migration.migrate()
                completedMigrations.add(migration)

                IcarionLoggerAdapter.d("Completed migration ${migration.targetVersion}")
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

                    is IcarionFailureRecoveryHint.Abort ->
                        return abortMigration(completed = completedMigrations, skipped = skippedMigrations)

                    is IcarionFailureRecoveryHint.Rollback ->
                        return rollbackMigration(completed = completedMigrations, skipped = skippedMigrations)
                }
            }
        }

        migrationsRunning = false


        val result = IcarionMigrationsResult.Success(
            completedMigrations.map { it.targetVersion }.toList(),
            skippedMigrations.map { it.targetVersion }.toList()
        )

        IcarionLoggerAdapter.i("Migration process completed successfully: $result")

        return result
    }

    private fun abortMigration(
        completed: Set<AppUpdateMigration<VERSION>>,
        skipped: Set<AppUpdateMigration<VERSION>>
    ): IcarionMigrationsResult.Failure<VERSION> {
        IcarionLoggerAdapter.i("Aborting migration")

        return IcarionMigrationsResult.Failure(
            completedNotRolledBackMigrations = completed.map { it.targetVersion }.toList(),
            skippedMigrations = skipped.map { it.targetVersion }.toList(),
            rolledBackMigrations = emptyList()
        )
    }

    private suspend fun rollbackMigration(
        completed: Set<AppUpdateMigration<VERSION>>,
        skipped: Set<AppUpdateMigration<VERSION>>
    ): IcarionMigrationsResult.Failure<VERSION> {
        val rolledBackMigrations = executeRollback(completed.toList())
        return IcarionMigrationsResult.Failure(
            completedNotRolledBackMigrations = (completed - rolledBackMigrations).map { it.targetVersion }.toList(),
            skippedMigrations = skipped.map { it.targetVersion }.toList(),
            rolledBackMigrations = rolledBackMigrations.map { it.targetVersion }.toList()
        )
    }

    private suspend fun executeRollback(completedMigrations: List<AppUpdateMigration<VERSION>>): Set<AppUpdateMigration<VERSION>> {
        IcarionLoggerAdapter.i("Rolling back completed migrations (execution will be in reversed order): ${completedMigrations.joinToString { it.targetVersion.toString() }}")

        val rolledBackMigrations = mutableSetOf<AppUpdateMigration<VERSION>>()
        completedMigrations.reversed().forEach {
            try {
                it.rollback()
                rolledBackMigrations.add(it)
            } catch (e: Exception) {
                IcarionLoggerAdapter.e(
                    e,
                    "Unable to rollback migration ${it.targetVersion}, stopping rollback mechanism."
                )

                return rolledBackMigrations
            }
        }

        IcarionLoggerAdapter.i("Rolled back all completed migrations: $rolledBackMigrations")
        return rolledBackMigrations
    }

}