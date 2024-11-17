package xyz.amplituhedron

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import xyz.amplituhedron.icarion.*
import xyz.amplituhedron.migrations.SampleMigrationTo_v1_2_3
import xyz.amplituhedron.migrations.SampleMigrationTo_v2_0_0
import xyz.amplituhedron.migrations.SampleMigrationTo_v2_1_0

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module).start(wait = true)
}

val logger = LoggerFactory.getLogger(Application::class.java)
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

        return IcarionFailureRecoveryHint.Skip
    }
}

fun Application.module() {
    configureRouting()

    val icarion = IcarionMigrator<SemanticVersion>().apply {
        migrationObserver = appMigrationObserver
        defaultFailureRecoveryHint = IcarionFailureRecoveryHint.Skip

        registerMigration(SampleMigrationTo_v1_2_3())
        registerMigration(SampleMigrationTo_v2_0_0())
        registerMigration(SampleMigrationTo_v2_1_0())
    }

    environment.monitor.subscribe(ApplicationStarted) {
        logger.debug("Application started")
        launch {
            withContext(Dispatchers.IO) {
                val result = icarion.executeMigrations(
                    fromVersion = fetchCurrentActiveVersion(),
                    toVersion = SemanticVersion(2, 1, 0) // For ex fetch target app version from application.conf
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

// Read the current active version from persistence
private fun fetchCurrentActiveVersion() = SemanticVersion(2, 0, 0)