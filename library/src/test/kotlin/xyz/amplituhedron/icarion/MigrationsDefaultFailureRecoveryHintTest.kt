package xyz.amplituhedron.icarion

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.*
import xyz.amplituhedron.icarion.mock.FailingMigration
import xyz.amplituhedron.icarion.mock.MockMigration


class MigrationsDefaultFailureRecoveryHintTest {

    @Test
    fun `all migrations abort when defaultFailureRecoveryHint is Abort`() = runTest {
        val migrator = IcarionMigrator<IntVersion>().apply {
            defaultFailureRecoveryHint = IcarionFailureRecoveryHint.Abort
        }

        // Register migrations
        val migrations = listOf(
            MockMigration(IntVersion(1)),
            FailingMigration(IntVersion(2)),
            MockMigration(IntVersion(3)),
            MockMigration(IntVersion(4))
        )

        migrations.forEach { migrator.registerMigration(it) }

        // Execute migrations
        val result = migrator.executeMigrations(IntVersion(0), IntVersion(4))

        // Assert that migration aborted after the first failure
        expectThat(result).isA<IcarionMigrationsResult.Failure<IntVersion>>().and {
            get { completedNotRolledBackMigrations }.containsExactly(IntVersion(1))
            get { skippedMigrations }.isEmpty()
            get { rolledBackMigrations }.isEmpty()
        }

        // Verify execution status of the migrations
        expectThat(migrations[0].executed).isEqualTo(true) // v1 succeeded
        expectThat(migrations[1].executed).isEqualTo(false) // v2 failed
        expectThat(migrations[2].executed).isEqualTo(false) // v3 not executed
        expectThat(migrations[3].executed).isEqualTo(false) // v4 not executed
    }

    @Test
    fun `failed migrations are skipped when defaultFailureRecoveryHint is Skip`() = runTest {
        val migrator = IcarionMigrator<IntVersion>().apply {
            defaultFailureRecoveryHint = IcarionFailureRecoveryHint.Skip
        }

        // Register migrations
        val migrations = listOf(
            MockMigration(IntVersion(1)),
            FailingMigration(IntVersion(2)),
            MockMigration(IntVersion(3)),
            MockMigration(IntVersion(4))
        )

        migrations.forEach { migrator.registerMigration(it) }

        // Execute migrations
        val result = migrator.executeMigrations(IntVersion(0), IntVersion(4))

        // Assert that failed migration was skipped, and others were executed
        expectThat(result).isA<IcarionMigrationsResult.Success<IntVersion>>().and {
            get { completedMigrations }.containsExactly(IntVersion(1), IntVersion(3), IntVersion(4))
            get { skippedMigrations }.containsExactly(IntVersion(2))
        }

        // Verify execution status of the migrations
        expectThat(migrations[0].executed).isEqualTo(true) // v1 succeeded
        expectThat(migrations[1].executed).isEqualTo(false) // v2 failed
        expectThat(migrations[2].executed).isEqualTo(true) // v3 succeeded
        expectThat(migrations[3].executed).isEqualTo(true) // v4 succeeded
    }

}