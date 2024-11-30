package xyz.amplituhedron.icarion

import kotlinx.coroutines.test.runTest
import xyz.amplituhedron.icarion.mock.FailingMigration
import xyz.amplituhedron.icarion.mock.MockMigration
import kotlin.test.*


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
        assertIs<IcarionMigrationsResult.Failure<IntVersion>>(result)

        assertContentEquals(result.completedNotRolledBackMigrations, listOf(IntVersion(1)))
        assertTrue { result.skippedMigrations.isEmpty() }
        assertTrue { result.rolledBackMigrations.isEmpty()  }

        // Verify execution status of the migrations
        assertTrue { migrations[0].executed } // v1 succeeded
        assertFalse { migrations[1].executed } // v2 failed
        assertFalse { migrations[2].executed } // v3 not executed
        assertFalse { migrations[3].executed } // v4 not executed
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
        assertIs<IcarionMigrationsResult.Success<IntVersion>>(result)
        assertContentEquals(result.completedMigrations, listOf(IntVersion(1), IntVersion(3), IntVersion(4)))
        assertContentEquals(result.skippedMigrations, listOf(IntVersion(2)))

        // Verify execution status of the migrations
        assertTrue { migrations[0].executed } // v1 succeeded
        assertFalse { migrations[1].executed } // v2 failed
        assertTrue { migrations[2].executed } // v3 succeeded
        assertTrue { migrations[3].executed } // v4 succeeded
    }

}