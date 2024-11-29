package xyz.amplituhedron.icarion

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import xyz.amplituhedron.icarion.mock.MockMigration
import kotlin.test.Test


@ExperimentalCoroutinesApi
class MigrationsTest {

//
//    @Test
//    fun `successful migration when no migrations are registered`() = runTest {
//        val migrator = IcarionMigrator<IntVersion>()
//
//        val result = migrator.executeMigrations(IntVersion(1), IntVersion(5))
//
//        expectThat(result).isA<IcarionMigrationsResult.Success<IntVersion>>().and {
//            get { completedMigrations }.isEmpty()
//            get { skippedMigrations }.isEmpty()
//        }
//    }
//
//    @Test
//    fun `all migrations are executed between two versions`() = runTest {
//        val migrator = IcarionMigrator<IntVersion>()
//
//        // Register six migrations
//        val migrations = listOf(
//            MockMigration(IntVersion(1)),
//            MockMigration(IntVersion(2)),
//            MockMigration(IntVersion(3)),
//            MockMigration(IntVersion(4)),
//            MockMigration(IntVersion(5)),
//            MockMigration(IntVersion(6))
//        )
//
//        migrations.forEach { migrator.registerMigration(it) }
//
//        // Execute migrations from version 3 to version 6
//        val result = migrator.executeMigrations(IntVersion(3), IntVersion(6))
//
//        // Assert that only the migrations for versions 4, 5, and 6 were executed
//        expectThat(result).isA<IcarionMigrationsResult.Success<IntVersion>>().and {
//            get { completedMigrations }.containsExactly(IntVersion(4), IntVersion(5), IntVersion(6))
//            get { skippedMigrations }.isEmpty()
//        }
//
//        // Verify execution status of the migrations
//        expectThat(migrations[0].executed).isEqualTo(false) // v1
//        expectThat(migrations[1].executed).isEqualTo(false) // v2
//        expectThat(migrations[2].executed).isEqualTo(false) // v3
//        expectThat(migrations[3].executed).isEqualTo(true)  // v4
//        expectThat(migrations[4].executed).isEqualTo(true)  // v5
//        expectThat(migrations[5].executed).isEqualTo(true)  // v6
//    }

}