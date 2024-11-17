package xyz.amplituhedron.migrations

import io.ktor.server.application.*
import org.slf4j.LoggerFactory
import xyz.amplituhedron.icarion.AppUpdateMigration
import xyz.amplituhedron.icarion.SemanticVersion

class SampleMigrationTo_v1_2_3(override val targetVersion: SemanticVersion = SemanticVersion(1, 2, 3)) :
    AppUpdateMigration<SemanticVersion> {

    private val logger = LoggerFactory.getLogger(Application::class.java)

    override suspend fun migrate() {
        logger.info("Migrating Sample Migration to $targetVersion")

        logger.info("Moving temp files redis cache")
    }

    override suspend fun rollback() {
        // Ignore
    }

}

class SampleMigrationTo_v2_0_0(override val targetVersion: SemanticVersion = SemanticVersion(2, 0, 0)) :
    AppUpdateMigration<SemanticVersion> {

    private val logger = LoggerFactory.getLogger(Application::class.java)

    override suspend fun migrate() {
        logger.info("Migrating Sample Migration to $targetVersion")

        logger.info("Copying old session tokens to tmp db table..")

        logger.info("Invalidating old session tokens from db..")
    }

    override suspend fun rollback() {
        logger.info("Rolling back old session tokens from tmp db table..")
    }

}

class SampleMigrationTo_v2_1_0(override val targetVersion: SemanticVersion = SemanticVersion(2, 1, 0)) :
    AppUpdateMigration<SemanticVersion> {

    private val logger = LoggerFactory.getLogger(Application::class.java)

    override suspend fun migrate() {
        logger.info("Migrating Sample Migration to $targetVersion")

        logger.info("Generating redis cache for user icons")
    }

    override suspend fun rollback() {
        logger.info("No rollback needed for this migration.")
    }

}