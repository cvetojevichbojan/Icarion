package xyz.amplituhedron.icarion.kmp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

import icarionmultiplatform.composeapp.generated.resources.Res
import icarionmultiplatform.composeapp.generated.resources.compose_multiplatform
import kotlinx.coroutines.launch
import xyz.amplituhedron.icarion.IcarionFailureRecoveryHint
import xyz.amplituhedron.icarion.IcarionMigrationsResult
import xyz.amplituhedron.icarion.IcarionMigrator
import xyz.amplituhedron.icarion.SemanticVersion
import xyz.amplituhedron.icarion.kmp.migrations.AppUpdateV110
import xyz.amplituhedron.icarion.kmp.migrations.AppUpdateV112
import xyz.amplituhedron.icarion.kmp.migrations.AppUpdateV130
import xyz.amplituhedron.icarion.log.IcarionLogger
import xyz.amplituhedron.icarion.log.IcarionLoggerAdapter

@Composable
@Preview
fun App() {
    val coroutineScope = rememberCoroutineScope()

    MaterialTheme {
        var showContent by remember { mutableStateOf(false) }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = {
                coroutineScope.launch {
                    executeAppUpdateMigrations()
                    showContent = !showContent
                }
            }) {
                Text("Run Icarion App Update Migrations")
            }
            AnimatedVisibility(showContent) {
                val greeting = remember { Greeting().greet() }
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Look at app logs")
                }
            }
        }
    }
}

// Migration example
private const val APP_VERSION = "appVersion"
private val settings = Settings()


private suspend fun executeAppUpdateMigrations() {
    IcarionLoggerAdapter.init(createLoggerFacade())

    val icarion = IcarionMigrator<SemanticVersion>().apply {
        defaultFailureRecoveryHint = IcarionFailureRecoveryHint.Skip

        registerMigration(AppUpdateV110())
        registerMigration(AppUpdateV112())
        registerMigration(AppUpdateV130())
    }

    val previousVersion = SemanticVersion.fromVersion(settings[APP_VERSION, "1.0.0"])
    val targetVersion = SemanticVersion.fromVersion("1.3.0")

    val result = icarion.executeMigrations(previousVersion, targetVersion)
    when (result) {
        is IcarionMigrationsResult.AlreadyRunning<*> -> {
            println("Cant run multiple migrations at the same time.")
        }
        is IcarionMigrationsResult.Failure<SemanticVersion> -> {
            println("Handle failure")
        }
        is IcarionMigrationsResult.Success<SemanticVersion> -> {
            println("Saving version to settings")
            settings[APP_VERSION] = targetVersion.toString()
        }
    }
}


private fun createLoggerFacade() = object : IcarionLogger {

    override fun d(message: String) {
        println(message)
    }

    override fun i(message: String) {
        println(message)
    }

    override fun e(t: Throwable, message: String) {
        println("$message - cause -> ${t.toString()}")
    }

    override fun e(t: Throwable) {
        println(t)
    }
}

