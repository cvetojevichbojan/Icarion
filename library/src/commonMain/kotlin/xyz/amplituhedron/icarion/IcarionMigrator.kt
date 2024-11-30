package xyz.amplituhedron.icarion

import xyz.amplituhedron.icarion.log.IcarionLogger
import xyz.amplituhedron.icarion.log.IcarionLoggerAdapter
import kotlin.concurrent.Volatile

/**
 * Observer with callback to monitor individual migration progress
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
        /**
         *  Migrations that completed but were not rolled back due to fallback hint or rollback failure.
         */
        val completedNotRolledBackMigrations: List<VERSION>,
        /**
         * Migrations which failed but were "recovered" via [IcarionFailureRecoveryHint.Skip]
         */
        val skippedMigrations: List<VERSION>,
        /**
         * Migrations which failed but were rolled back due to [IcarionFailureRecoveryHint.Rollback]
         */
        val rolledBackMigrations: List<VERSION>,
        /**
         * All migrations which were selected for migration between (currentVersion, targetVersion]
         */
        val eligibleMigrations: List<VERSION>,
        /**
         * Migration [VERSION] which caused the Failure
         */
        val failedMigration: VERSION
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
 * Two types are provided for convenience: [IntVersion] and [SemanticVersion], while all enums work out of the box since they implement the [Comparable] interface.
 *
 * Register migrations via [registerMigration]
 *
 * Execute migrations via [executeMigrations] and check the result [IcarionMigrationsResult]
 *
 * Specify default recovery strategy via [defaultFailureRecoveryHint] and [IcarionFailureRecoveryHint]
 *
 * Monitor migrations and control individual recovery strategey via [migrationObserver]
 *
 * For logging implement the [IcarionLogger] and set it via [IcarionLoggerAdapter.init]
 * ```kotlin
 * IcarionLoggerAdapter.init(YourLoggerImpl())
 * ```
 *
 * For detailed documentation and usage samples please visit the github page [https://github.com/cvetojevichbojan/Icarion]
 *
 * @param VERSION your Version type (Comparable<VERSION>)
 */
class IcarionMigrator<VERSION : Comparable<VERSION>> {

    /**
     * Observer to monitor migration events and react to failures via individual [IcarionFailureRecoveryHint]'s
     */
    @Volatile
    var migrationObserver: IcarionMigrationObserver<VERSION>? = null

    /**
     * Default recovery hint if no observer is specified.
     *
     * Defaults to [IcarionFailureRecoveryHint.Abort]
     */
    @Volatile
    var defaultFailureRecoveryHint: IcarionFailureRecoveryHint = IcarionFailureRecoveryHint.Abort

    @Volatile
    private var migrationsRunning = false

    private val migrations = mutableSetOf<AppUpdateMigration<VERSION>>()

    /**
     * Register migration for later execution
     * @param migration
     */
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

    /**
     * Executes registered migrations in sequence between ([fromVersion], [toVersionInclusive]] and returns [IcarionMigrationsResult]
     *
     * For realtime migration progress and recovery handling, checkout [migrationObserver] and [IcarionMigrationObserver]
     *
     * @param fromVersion from which version are you upgrading
     * @param toVersionInclusive to which version you are upgrading
     */
    suspend fun executeMigrations(fromVersion: VERSION, toVersionInclusive: VERSION): IcarionMigrationsResult<VERSION> {
        IcarionLoggerAdapter.i("Requesting migration from $fromVersion to $toVersionInclusive")

        if (migrationsRunning) {
            IcarionLoggerAdapter.i("Migrations unavailable because IcarionMigrationsResult.AlreadyRunning")

            return IcarionMigrationsResult.AlreadyRunning()
        }

        migrationsRunning = true

        val completedMigrations = mutableSetOf<AppUpdateMigration<VERSION>>()
        val skippedMigrations = mutableSetOf<AppUpdateMigration<VERSION>>()

        val eligibleMigrations = getEligibleMigrations(fromVersion, toVersionInclusive)
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
                        return abortMigration(allMigrations = eligibleMigrations, completed = completedMigrations, skipped = skippedMigrations, failedMigration = migration)

                    is IcarionFailureRecoveryHint.Rollback ->
                        return rollbackMigration(allMigrations = eligibleMigrations, completed = completedMigrations, skipped = skippedMigrations, failedMigration = migration)
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

    private fun getEligibleMigrations(fromVersion: VERSION, toVersion: VERSION): List<AppUpdateMigration<VERSION>> {
        return migrations
            .filter { it.targetVersion > fromVersion && it.targetVersion <= toVersion }
            .sortedBy { it.targetVersion }
    }

    private fun abortMigration(
        allMigrations: Collection<AppUpdateMigration<VERSION>>,
        completed: Set<AppUpdateMigration<VERSION>>,
        skipped: Set<AppUpdateMigration<VERSION>>,
        failedMigration: AppUpdateMigration<VERSION>
    ): IcarionMigrationsResult.Failure<VERSION> {
        IcarionLoggerAdapter.i("Aborting migration")

        return IcarionMigrationsResult.Failure(
            completedNotRolledBackMigrations = completed.map { it.targetVersion }.toList(),
            skippedMigrations = skipped.map { it.targetVersion }.toList(),
            rolledBackMigrations = emptyList(),
            eligibleMigrations = allMigrations.map { it.targetVersion },
            failedMigration = failedMigration.targetVersion
        )
    }

    private suspend fun rollbackMigration(
        allMigrations: Collection<AppUpdateMigration<VERSION>>,
        completed: Set<AppUpdateMigration<VERSION>>,
        skipped: Set<AppUpdateMigration<VERSION>>,
        failedMigration: AppUpdateMigration<VERSION>
    ): IcarionMigrationsResult.Failure<VERSION> {
        val rolledBackMigrations = executeRollback(completed.toList())
        return IcarionMigrationsResult.Failure(
            completedNotRolledBackMigrations = (completed.map { it.targetVersion } - rolledBackMigrations.map { it.targetVersion }).toList(),
            skippedMigrations = skipped.map { it.targetVersion }.toList(),
            rolledBackMigrations = rolledBackMigrations.map { it.targetVersion }.toList(),
            eligibleMigrations = allMigrations.map { it.targetVersion },
            failedMigration = failedMigration.targetVersion
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