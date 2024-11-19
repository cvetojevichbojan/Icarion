package xyz.amplituhedron

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.logging.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import xyz.amplituhedron.icarion.*
import xyz.amplituhedron.icarion.log.IcarionLogger
import xyz.amplituhedron.icarion.log.IcarionLoggerAdapter
import xyz.amplituhedron.migrations.SampleMigrationTo_v1_2_3
import xyz.amplituhedron.migrations.SampleMigrationTo_v2_0_0
import xyz.amplituhedron.migrations.SampleMigrationTo_v2_1_0
import xyz.amplituhedron.migrations.SampleMigrationTo_v2_1_3

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module).start(wait = true)
}

val logger = LoggerFactory.getLogger(Application::class.java)

fun Application.module() {
    configureRouting()

    // Add logger adapter
    IcarionLoggerAdapter.init(createLoggerFacade())

    // Create Icarion Migrator
    val icarion = IcarionMigrator<SemanticVersion>().apply {
        migrationObserver = appMigrationObserver

        // Default will be applied if you omit the migration observer
        defaultFailureRecoveryHint = IcarionFailureRecoveryHint.Skip

        registerMigration(SampleMigrationTo_v1_2_3())
        registerMigration(SampleMigrationTo_v2_0_0())
        registerMigration(SampleMigrationTo_v2_1_0())
        registerMigration(SampleMigrationTo_v2_1_3())
    }

    environment.monitor.subscribe(ApplicationStarted) {
        logger.debug("Application started")
        launch {
            withContext(Dispatchers.IO) {
                val result = icarion.executeMigrations(
                    fromVersion = fetchCurrentActiveVersion(),
                    toVersionInclusive = SemanticVersion(2, 1, 3) // For ex fetch target app version from application.conf
                )

                when (result) {
                    is IcarionMigrationsResult.AlreadyRunning -> {
                        logger.error("Can not run multiple migrations at the same time!")
                    }

                    is IcarionMigrationsResult.Failure -> {
                        logger.error(result.toString())
                    }

                    is IcarionMigrationsResult.Success -> {
                        logger.info(result.toString())
                        // Store current active version in persistence
                    }
                }
            }
        }
    }
}

/**
 * IcarionMigrationObserver example
 */
val appMigrationObserver = object : IcarionMigrationObserver<SemanticVersion> {
    override fun onMigrationStart(version: SemanticVersion) {
        logger.info("onMigrationStart ${version.toString()}")
    }

    override fun onMigrationSuccess(version: SemanticVersion) {
        logger.info("onMigrationSuccess ${version.toString()}")
    }

    override fun onMigrationFailure(
        version: SemanticVersion, exception: Exception
    ): IcarionFailureRecoveryHint {
        logger.error("onMigrationFailure ${version.toString()}")

        // Skip if its non breaking for ex.
        return IcarionFailureRecoveryHint.Skip
    }
}

// Mock read the current active version from persistence
private fun fetchCurrentActiveVersion() = SemanticVersion(2, 0, 0)

/**
 * Sl4fj logger facade
 */
private fun createLoggerFacade() = object : IcarionLogger {
    private val logger = LoggerFactory.getLogger("IcarionLogger")

    override fun d(message: String) {
        logger.debug(message)
    }

    override fun i(message: String) {
        logger.info(message)
    }

    override fun e(t: Throwable, message: String) {
       logger.error(message, t)
    }

    override fun e(t: Throwable) {
        logger.error(t)
    }
}
