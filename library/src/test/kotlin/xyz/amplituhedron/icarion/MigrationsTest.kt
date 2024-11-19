package xyz.amplituhedron.icarion

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.*
import xyz.amplituhedron.icarion.mock.FailingMigration
import xyz.amplituhedron.icarion.mock.MockMigration


@ExperimentalCoroutinesApi
class MigrationsTest {


    @Test
    fun `successful migration when no migrations are registered`() = runTest {
        val migrator = IcarionMigrator<IntVersion>()

        val result = migrator.executeMigrations(IntVersion(1), IntVersion(5))

        expectThat(result).isA<IcarionMigrationsResult.Success<IntVersion>>().and {
            get { completedMigrations }.isEmpty()
            get { skippedMigrations }.isEmpty()
        }
    }

    @Test
    fun `all migrations are executed between two versions`() = runTest {
        val migrator = IcarionMigrator<IntVersion>()

        // Register six migrations
        val migrations = listOf(
            MockMigration(IntVersion(1)),
            MockMigration(IntVersion(2)),
            MockMigration(IntVersion(3)),
            MockMigration(IntVersion(4)),
            MockMigration(IntVersion(5)),
            MockMigration(IntVersion(6))
        )

        migrations.forEach { migrator.registerMigration(it) }

        // Execute migrations from version 3 to version 6
        val result = migrator.executeMigrations(IntVersion(3), IntVersion(6))

        // Assert that only the migrations for versions 4, 5, and 6 were executed
        expectThat(result).isA<IcarionMigrationsResult.Success<IntVersion>>().and {
            get { completedMigrations }.containsExactly(IntVersion(4), IntVersion(5), IntVersion(6))
            get { skippedMigrations }.isEmpty()
        }

        // Verify execution status of the migrations
        expectThat(migrations[0].executed).isEqualTo(false) // v1
        expectThat(migrations[1].executed).isEqualTo(false) // v2
        expectThat(migrations[2].executed).isEqualTo(false) // v3
        expectThat(migrations[3].executed).isEqualTo(true)  // v4
        expectThat(migrations[4].executed).isEqualTo(true)  // v5
        expectThat(migrations[5].executed).isEqualTo(true)  // v6
    }

    @Test
    fun `failed migration is skipped when defaultFailureRecoveryHint is Skip`() = runTest {
        val migrator = IcarionMigrator<IntVersion>().apply {
            defaultFailureRecoveryHint = IcarionFailureRecoveryHint.Skip
        }

        // Register migrations with one failure
        val migrations = listOf(
            MockMigration(IntVersion(1)),
            FailingMigration(IntVersion(2)), // Will fail
            MockMigration(IntVersion(3)),
            MockMigration(IntVersion(4))
        )

        migrations.forEach { migrator.registerMigration(it) }

        // Execute migrations from version (1 to 4]
        val result = migrator.executeMigrations(IntVersion(1), IntVersion(4))

        // Assert that the failed migration was skipped and remaining were executed
        expectThat(result).isA<IcarionMigrationsResult.Success<IntVersion>>().and {
            get { completedMigrations }.containsExactly(IntVersion(3), IntVersion(4))
            get { skippedMigrations }.containsExactly(IntVersion(2))
        }

        // Execution failed and rollback was not triggered
        expectThat((migrations[1]).executed).isEqualTo(false)
        expectThat((migrations[1]).rollbacked).isEqualTo(false)

        expectThat((migrations[2]).executed).isEqualTo(true)
        expectThat((migrations[3]).executed).isEqualTo(true)
    }

    @Test
    fun `failed migration aborts the migration in place when defaultFailureRecoveryHint is Abort`() = runTest {
        val migrator = IcarionMigrator<IntVersion>().apply {
            defaultFailureRecoveryHint = IcarionFailureRecoveryHint.Abort
        }

        // Register migrations with one failure
        val migrations = listOf(
            MockMigration(IntVersion(1)),
            FailingMigration(IntVersion(2)), // Will fail
            MockMigration(IntVersion(3))
        )

        migrations.forEach { migrator.registerMigration(it) }

        // Execute migrations from version 0 to 3
        val result = migrator.executeMigrations(IntVersion(0), IntVersion(3))

        // Assert that migration stopped on failure
        expectThat(result).isA<IcarionMigrationsResult.Failure<IntVersion>>().and {
            get { completedNotRolledBackMigrations }.containsExactly(IntVersion(1))
            get { skippedMigrations }.isEmpty() // Nothing was skipped for failure
            get { rolledBackMigrations }.isEmpty() // Assume no rollback for Abort
            get { eligibleMigrations }.containsExactly(migrations.map { it.targetVersion })
            get { failedMigration }.isEqualTo(IntVersion(2))
        }

        // Verify execution status
        expectThat((migrations[0]).executed).isEqualTo(true)  // v1
        expectThat((migrations[0]).rollbacked).isEqualTo(false)  // (not rollbacked)

        expectThat((migrations[1] as FailingMigration).executed).isEqualTo(false) // v2 (failed, abort)
        expectThat((migrations[1] as FailingMigration).rollbacked).isEqualTo(false) // v2 (not rollbacked)

        expectThat((migrations[2]).executed).isEqualTo(false)  // v3 (ignored due to abort)
        expectThat((migrations[2]).rollbacked).isEqualTo(false)  // v3 (ignored due to abort)
    }

    @Test
    fun `rollback occurs on failure when defaultFailureRecoveryHint is Rollback`() = runTest {
        val migrator = IcarionMigrator<IntVersion>().apply {
            defaultFailureRecoveryHint = IcarionFailureRecoveryHint.Rollback
        }

        // Register migrations with one failure
        val migrations = listOf(
            MockMigration(IntVersion(1)),
            MockMigration(IntVersion(2)),
            FailingMigration(IntVersion(3)), // Will fail
            MockMigration(IntVersion(4)),
            MockMigration(IntVersion(5))
        )

        migrations.forEach { migrator.registerMigration(it) }

        // Execute migrations from to version (0 to 5]
        val result = migrator.executeMigrations(IntVersion(0), IntVersion(5))

        // Assert that migration failed and rollbacks were performed
        expectThat(result).isA<IcarionMigrationsResult.Failure<IntVersion>>().and {
            get { completedNotRolledBackMigrations }.isEmpty()
            get { rolledBackMigrations }.containsExactly(IntVersion(2), IntVersion(1))
            get { skippedMigrations }.isEmpty()
            get { eligibleMigrations }.containsExactly(migrations.map { it.targetVersion })
            get { failedMigration }.isEqualTo(IntVersion(3))
        }

        // Verify execution and rollback status
        expectThat((migrations[0]).executed).isEqualTo(true)     // v1
        expectThat((migrations[0]).rollbacked).isEqualTo(true)  // v1 rolledback

        expectThat((migrations[1]).executed).isEqualTo(true)     // v2
        expectThat((migrations[1]).rollbacked).isEqualTo(true)  // v2 rolledback

        expectThat((migrations[2] as FailingMigration).executed).isEqualTo(false) // v3 (failed)
        expectThat((migrations[2] as FailingMigration).rollbacked).isEqualTo(false) // v3 (no rollback since it failed)

        expectThat((migrations[3]).executed).isEqualTo(false)    // v4 (never reached)
        expectThat((migrations[3]).executed).isEqualTo(false)    // v4 (no rollback since it never reached)

        expectThat((migrations[4]).executed).isEqualTo(false)    // v5 (no rollback since it never reached)
        expectThat((migrations[4]).executed).isEqualTo(false)    // v5 (no rollback since it never reached)
    }

    @Test
    fun `migration recovery logic can be set to Skip and Aborted via MigrationObserver`() = runTest {
        val migrator = IcarionMigrator<IntVersion>()

        val migrations = listOf(
            MockMigration(IntVersion(1)),
            MockMigration(IntVersion(2)),
            FailingMigration(IntVersion(3)), // Will Skip
            MockMigration(IntVersion(4)),
            MockMigration(IntVersion(5)),
            FailingMigration(IntVersion(6)), // Will Abort
            MockMigration(IntVersion(7)),
        )
        val selectedMigrations = migrations.drop(1)

        migrations.forEach { migrator.registerMigration(it) }

        // Define a MigrationObserver to control recovery behavior dynamically
        migrator.migrationObserver = object : IcarionMigrationObserver<IntVersion> {

            override fun onMigrationStart(version: IntVersion) {
                println("onMigrationStart $version")
            }

            override fun onMigrationSuccess(version: IntVersion) {
                println("onMigrationSuccess $version")
            }

            override fun onMigrationFailure(version: IntVersion, exception: Exception): IcarionFailureRecoveryHint {
                return if (version == IntVersion(3)) {
                    IcarionFailureRecoveryHint.Skip // Skip the failing migration
                } else {
                    IcarionFailureRecoveryHint.Abort // Abort for any other failure
                }
            }
        }

        // Execute migrations from version 1 to 7
        val result = migrator.executeMigrations(IntVersion(1), IntVersion(7))

        // Assert that migrations continued after skipping the failed migration but Aborted on Version 6
        expectThat(result).isA<IcarionMigrationsResult.Failure<IntVersion>>().and {
            get { completedNotRolledBackMigrations }.containsExactly(IntVersion(2), IntVersion(4), IntVersion(5))
            get { skippedMigrations }.containsExactly(IntVersion(3))
            get { eligibleMigrations }.contains(selectedMigrations.map { it.targetVersion })
//            get { rolledBackMigrations }.contains(IntVersion(5), IntVersion(4), IntVersion(2))
            get { rolledBackMigrations }.isEmpty()
            get { failedMigration }.isEqualTo(IntVersion(6))
        }

        // Verify execution status of migrations
        expectThat((selectedMigrations[0]).executed).isEqualTo(true)  // v1
        expectThat((selectedMigrations[1] as FailingMigration).executed).isEqualTo(false) // v3
        expectThat((selectedMigrations[2]).executed).isEqualTo(true)  // v4
        expectThat((selectedMigrations[3]).executed).isEqualTo(true)  // v5
        expectThat((selectedMigrations[4]).executed).isEqualTo(false)  // v6 aborted
        expectThat((selectedMigrations[5]).executed).isEqualTo(false)  // v7 never reached
    }

    @Test
    fun `migration recovery logic can be set to Rollback via MigrationObserver`() = runTest {
        val migrator = IcarionMigrator<IntVersion>()

        val migrations = listOf(
            MockMigration(IntVersion(1)),
            MockMigration(IntVersion(2)),
            FailingMigration(IntVersion(3)), // Will Skip
            MockMigration(IntVersion(4)),
            MockMigration(IntVersion(5)),
            FailingMigration(IntVersion(6)), // Will trigger Rollback
            MockMigration(IntVersion(7)),
        )
        val selectedMigrations = migrations.drop(1)

        migrations.forEach { migrator.registerMigration(it) }

        // Define a MigrationObserver to control recovery behavior dynamically
        migrator.migrationObserver = object : IcarionMigrationObserver<IntVersion> {

            override fun onMigrationStart(version: IntVersion) {
                println("onMigrationStart $version")
            }

            override fun onMigrationSuccess(version: IntVersion) {
                println("onMigrationSuccess $version")
            }

            override fun onMigrationFailure(version: IntVersion, exception: Exception): IcarionFailureRecoveryHint {
                return if (version == IntVersion(3)) {
                    IcarionFailureRecoveryHint.Skip // Skip the failing migration
                } else {
                    IcarionFailureRecoveryHint.Rollback // Rollback for any other failure
                }
            }
        }

        // Execute migrations from version 1 to 7
        val result = migrator.executeMigrations(IntVersion(1), IntVersion(7))

        // Assert that migrations continued after skipping the failed migration but Rolledback from Version 6 failure
        expectThat(result).isA<IcarionMigrationsResult.Failure<IntVersion>>().and {
            get { completedNotRolledBackMigrations }.isEmpty()
            get { skippedMigrations }.containsExactly(IntVersion(3))
            get { eligibleMigrations }.contains(selectedMigrations.map { it.targetVersion })
            get { rolledBackMigrations }.contains(IntVersion(5), IntVersion(4), IntVersion(2))
            get { failedMigration }.isEqualTo(IntVersion(6))
        }

        // Verify execution status of migrations
        expectThat((selectedMigrations[0]).executed).isEqualTo(true)  // v1
        expectThat((selectedMigrations[1] as FailingMigration).executed).isEqualTo(false) // v3
        expectThat((selectedMigrations[2]).executed).isEqualTo(true)  // v4
        expectThat((selectedMigrations[3]).executed).isEqualTo(true)  // v5
        expectThat((selectedMigrations[4]).executed).isEqualTo(false)  // v6 aborted
        expectThat((selectedMigrations[5]).executed).isEqualTo(false)  // v7 never reached
    }

}