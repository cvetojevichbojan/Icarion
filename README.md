<p align="center">
  <img src="/release/icarion_logo_128.jpg" />
</p>


# Icarion - Migration Library

Icarion is a lightweight, extensible migration library designed to handle version-based migrations for your application. It supports both rollback and recovery mechanisms for fine-grained control over migrations, making it ideal for settings and configuration changes, file migrations, even database updates and more.

Written 100% in Kotlin you can run it on any JVM based system: Android, Ktor, Spring, Desktop, you name it...Android based projects were the main culprit behind Icarion idea as many times devs would just perform SharedPreferences or FirebaseConfig data updates in the Application onCreate() based on current BuildConfig version value without any long term organization of migrations.    

This library is here to help alleviate some of the pain of rolling out your own system of migrations, no matter where you run it.

Inspired by <b>Icarus</b> myth, which is  often interpreted as a cautionary tale about ego, self-sabotage, and the consequences of ignoring wise counsel.

    Icarus, in Greek mythology, son of the inventor Daedalus who perished by flying too near the Sun with waxen wings.


## Features

- **Version-Based Migrations**: Define migrations targeting specific versions.
- **Flexible Rollbacks**: Support for rollback strategies in case of migration failures.
- **Observer Support**: Monitor migration progress with callbacks.
- **Concurrent Execution Prevention**: Ensures no overlapping migrations are executed.
- **Custom Recovery Strategies**: Handle failures by skipping, aborting, or rolling back migrations.


## Use Cases

* Configuration or settings updates
* File system changes
* Data transformations during app updates
* Any stateful application upgrade processes
* Database schema migrations

---

![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)
![License](https://img.shields.io/badge/license-Apache%202.0-blue?style=flat-square)
[![Maven Central Version](https://img.shields.io/maven-central/v/xyz.amplituhedron/icarion)](https://central.sonatype.com/artifact/xyz.amplituhedron/icarion)

## Installation

Add Icarion to your project as a dependency. If you use Gradle, add the following:


```kotlin
repositories {
    mavenCentral()
}


dependencies {
    implementation("com.example:icarion:1.0.0")
}
```
---
## Integratation

### Define Your Versioning Scheme
You can use any versioning system that implements `Comparable`. Icarion comes with two versioning schemes out of the box for easier integration: 

```data class IntVersion(val value: Int)``` and ```data class SemanticVersion(val major: Int, val minor: Int, val patch: Int)```

```kotlin
val v1 = IntVersion(1)
val v3 = IntVersion(3)

val v1_1_1 = SemanticVersion(1, 1, 1)
```

Enums can be used as they implement comparable by default via their natural ordering:

```kotlin
enum class YourAppNamedVersion {
    ACACIA, // Lowest
    BIRCH,
    CEDAR,
    DOUGLAS_FIR,
    OAK,
    PINE,
    SEQUOIA; // Highest
}


val v1 = YourAppNamedVersion.ACACIA
val v3 = YourAppNamedVersion.CEDAR
```

---

### Implement Migrations

To define a migration, you need to implement the `AppUpdateMigration` interface. This interface requires you to define:

1. **`targetVersion`**: The version this migration updates to.
2. **`migrate`**: The logic to apply the migration.
3. **`rollback`**: The logic to revert the migration in case of failure.

#### Example: Migration to IntVersion(2)

```kotlin
class SampleMigration : AppUpdateMigration<IntVersion> {
    override val targetVersion = IntVersion(2)

    override suspend fun migrate() {
        println("Migrating to version $targetVersion")
        // Add your migration logic here
    }

    override suspend fun rollback() {
        println("Rolling back version $targetVersion")
        // Add your rollback logic here
    }
}
```

#### Example: Migration to SemanticVersion(1, 1, 0)
```kotlin
class FeatureUpgradeMigrationV110 : AppUpdateMigration<SemanticVersion> {
    override val targetVersion = SemanticVersion(1, 1, 0)

    override suspend fun migrate() {
        println("Upgrading feature to version $targetVersion")
        // Add feature-specific migration logic here
    }

    override suspend fun rollback() {
        println("Reverting feature upgrade for version $targetVersion")
        // Add feature-specific rollback logic here
    }
}
```

Define as many migrations as needed for your application. Each migration should handle only the changes required for its specific version.

---

### Register Migrations

Once you've implemented your migrations, register them with an instance of `IcarionMigrator`. This ensures the migrator knows which migrations are available for execution.

#### Example: Registering Migrations

```kotlin
// Register multiple migrations at once
val migrator = IcarionMigrator<IntVersion>().apply {
    registerMigration(FeatureUpgradeMigrationV1())
    registerMigration(FeatureUpgradeMigrationV2())
    registerMigration(FeatureUpgradeMigrationV3())
    registerMigration(FeatureUpgradeMigrationV4())
    registerMigration(FeatureUpgradeMigrationV5())
}

// Register individual migrations
migrator.registerMigration(SampleMigration())
```

#### Constraints on Migration Registration

	•	You cannot register migrations while a migration process is running. An IllegalStateException will be thrown if you attempt to do so.
	•	Each migration must target a unique version. If you register two migrations with the same targetVersion, an IllegalArgumentException will be thrown.

Registering migrations correctly ensures that the migrator can execute the necessary upgrades in the right order.

---

### Executing Migrations

To execute migrations, invoke the migrateTo method, specifying current and the target version. The migrator will ensure that all migrations between the current and target versions are executed sequentially.

#### Example: Basic Migration Run

```kotlin
val currentVersion = IntVersion("1")
val targetVersion = IntVersion("5")

val result = migrator.migrateTo(
    from = currentVersion,
    to = targetVersion
)
```

In this example he migrator runs all migrations from version 1 up to and including 5.


### Migration Result

Migration Result

The IcarionMigrationsResult class encapsulates the outcome of executed migrations, providing a detailed report of the migration process.

Result Types

* <b>Success</b> - Indicates that all migrations have been successfully completed or skipped.
  - Fields:
    - completedMigrations: A list of successfully completed migrations.
    - skippedMigrations: A list of migrations that were skipped (they failed but you returned <b>Skip</b> from migration observer).
* <b>Failure</b> - Represents a migration failure and provides information about rollback operations.
  - Fields:
    -	completedNotRolledBackMigrations: A list of migrations that completed but were not rolled back due to failure.
    -	skippedMigrations: A list of migrations that were skipped.
    -	rolledBackMigrations: A list of migrations that were attempted but rolled back due to an error.
* <b>AlreadyRunning</b> - Indicates that another migration process is already in progress.


### Migration Observer

To provide insights into the migration process, IcarionMigrator supports an observer mechanism. By implementing the IcarionMigrationObserver interface, you can monitor the progress of each migration, handle failures, and decide the recovery strategy.

#### Defining a Migration Observer

The IcarionMigrationObserver interface includes the following methods:
   * ```onMigrationStart(version: VERSION)```: Invoked when a migration targeting the specified version begins. 
   * ```onMigrationSuccess(version: VERSION)```: Invoked when a migration targeting the specified version completes successfully. 
   * ```onMigrationFailure(version: VERSION, exception: Exception)```: Invoked when a migration targeting the specified version fails. You can return an appropriate ```IcarionFailureRecoveryHint``` to determine the recovery strategy: ```Skip```, ```Rollback```, or ```Abort```.

Observer can be set via ```migrator.migrationObserver```

---

### Failure Recovery and Rollback

The migrator supports three strategies to handle migration failures, configurable via `IcarionFailureRecoveryHint`:

1. **Skip**: Continues execution by skipping the failed migration.
2. **Rollback**: Tries to revert previously successful migrations in reverse order.
3. **Abort**: Stops the migration process immediately without rolling back or continuing.

These strategies can be set as a default via ```migrator.defaultFailureRecoveryHint``` or they can be determined on individual migration level.

The default value is ```IcarionFailureRecoveryHint.Abort```

If no Migration Observer is set, the  ```defaultFailureRecoveryHint``` is used. 
With the Migration Observer, each migration must return how its failure should be addressed.

#### Rollback Logic

When using the `Rollback` strategy, the migrator will:
1. Halt further migrations upon encountering a failure.
2. Invoke the `rollback` function for all successfully executed migrations, in reverse order.


Note: <b>If rollback fails for any migration in the chain, the process is stopped and ```IcarionMigrationsResult.Failure``` is returned with info on which migrations have been completed and which have been rolled backed.</b>

With this Result information you can then decide how to handle the failed rollback process.

---

### Logging Migration Progress

The migrator provides detailed logging at every step of the process. You can integrate your preferred logging framework 
(e.g., SLF4J, Android Logcat, etc...) to monitor progress, failures, and skipped migrations.

Logging is done via a small and simple Logger Facade to not force any dependencies via Icarion. 

#### Example: Slf4j logger facade

```kotlin
IcarionLoggerAdapter.init(createLoggerFacade())

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

```

## Samples

Take a look at working samples in the following folders ktor-sample, android-sample, desktop-sample (TODO).

## Summary

The IcarionMigrator simplifies version migrations by handling:

* Version ordering: Ensures migrations are executed in the correct sequence.
* Failure recovery: Allows flexible behavior when migrations fail.
* Logging: Provides visibility into migration progress and issues.

You’re now ready to run migrations in your app!

---

## Contributing

Contributions are welcome! Please fork this repository and submit a pull request.