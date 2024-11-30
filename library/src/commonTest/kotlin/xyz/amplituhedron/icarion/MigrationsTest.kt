package xyz.amplituhedron.icarion

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import xyz.amplituhedron.icarion.mock.MockMigration
import kotlin.test.*


@ExperimentalCoroutinesApi
class MigrationsTest {


    @Test
    fun `successful migration when no migrations are registered`() = runTest {
        val migrator = IcarionMigrator<IntVersion>()

        val result = migrator.executeMigrations(IntVersion(1), IntVersion(5))

        assertIs<IcarionMigrationsResult.Success<IntVersion>>(result)

        assertTrue { result.completedMigrations.isEmpty() }
        assertTrue { result.skippedMigrations.isEmpty() }
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

        assertIs<IcarionMigrationsResult.Success<IntVersion>>(result)

        // Assert that only the migrations for versions 4, 5, and 6 were executed
        assertContentEquals(result.completedMigrations, listOf(IntVersion(4), IntVersion(5), IntVersion(6)))
        assertTrue { result.skippedMigrations.isEmpty() }


        // Verify execution status of the migrations
        assertFalse { migrations[0].executed } // v1
        assertFalse { migrations[1].executed } // v2
        assertFalse { migrations[2].executed } // v3
        assertTrue { migrations[3].executed }  // v4
        assertTrue { migrations[4].executed }  // v5
        assertTrue { migrations[5].executed }  // v6
    }

}