package xyz.amplituhedron.android_icarion

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.amplituhedron.android_icarion.migrations.AppMigrationV2
import xyz.amplituhedron.android_icarion.migrations.AppMigrationV3
import xyz.amplituhedron.android_icarion.migrations.AppMigrationV7
import xyz.amplituhedron.android_icarion.migrations.SharedPreferencesVersionRepository
import xyz.amplituhedron.android_icarion.ui.theme.AndroidIcarionTheme
import xyz.amplituhedron.icarion.IcarionFailureRecoveryHint
import xyz.amplituhedron.icarion.IcarionMigrationObserver
import xyz.amplituhedron.icarion.IcarionMigrationsResult
import xyz.amplituhedron.icarion.IcarionMigrator
import xyz.amplituhedron.icarion.IntVersion

class MainActivity : ComponentActivity() {

    private val versionRepo by lazy {
        injectVersionRepo(this)
    }
    private val migrator = injectMigrator()

    init {
        migrator.migrationObserver = object : IcarionMigrationObserver<IntVersion> {
            override fun onMigrationStart(version: IntVersion) {
                // Notify user if you want how the migrations are going
            }

            override fun onMigrationFailure(
                version: IntVersion,
                exception: Exception
            ): IcarionFailureRecoveryHint {
                // Lets say you can skip all failed migrations
                return IcarionFailureRecoveryHint.Skip
            }

            override fun onMigrationSuccess(version: IntVersion) {
                // Notify user if you want how the migrations are going
            }

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidIcarionTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )

                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Button(onClick = {
                            lifecycleScope.launch {
                                withContext(Dispatchers.IO) {
                                    launchMigrations()
                                }
                            }
                        }) {
                            Text(text = "Launch migrations")
                        }
                    }
                }
            }
        }
    }

    private suspend fun launchMigrations() {
        val targetVersion = IntVersion(BuildConfig.VERSION_CODE)
        val result = migrator.executeMigrations(versionRepo.getCurrentVersion(), targetVersion)
        when (result) {
            is IcarionMigrationsResult.AlreadyRunning -> {
                // Can not launch multiple migrations
            }

            is IcarionMigrationsResult.Failure -> {
                // You can determine how to proceed based on the following data
                println(result.skippedMigrations)
                println(result.rolledBackMigrations)
                println(result.completedNotRolledBackMigrations)
            }

            is IcarionMigrationsResult.Success -> {
                versionRepo.setCurrentVersion(targetVersion)

                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(this, "Migration Success: $result", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AndroidIcarionTheme {
        Greeting("Android")
    }
}

// DI mocks
private fun injectMigrator() = IcarionMigrator<IntVersion>().apply {
    // default will apply if you do not set the migrationObserver
    defaultFailureRecoveryHint = IcarionFailureRecoveryHint.Skip

    registerMigration(AppMigrationV2())
    registerMigration(AppMigrationV3())
    registerMigration(AppMigrationV7())
}

private fun injectVersionRepo(context: Context) = SharedPreferencesVersionRepository.create(context)