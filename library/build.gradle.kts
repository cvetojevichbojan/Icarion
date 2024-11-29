import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.vanniktech.maven.publish.SonatypeHost
plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinMultiplatform)

    id("com.vanniktech.maven.publish") version "0.29.0"

}

group = "xyz.amplituhedron"
val artifactId = "icarion"
version = "1.1.0"

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
    jvm()
    androidTarget {
        publishLibraryVariants("release")

        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17) // libs.versions.java.get()
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    linuxX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Icarion is self-sufficient for now
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.test)

                implementation(libs.kotlin.test)
            }
        }
    }
}

android {
    namespace = "xyz.amplituhedron.icarion"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    publishing {
        singleVariant("release") {

        }
    }
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    signAllPublications()

    coordinates(group.toString(), artifactId, version.toString())

    pom {
        name = "Icarion library"
        description = "Extensible app data migration"
        inceptionYear = "2024"
        url = "https://github.com/cvetojevichbojan/Icarion"
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                id = "cvetojevichbojan"
                name = "Bojan Cvetojevic"
                url = "https://github.com/cvetojevichbojan"
            }
        }
        scm {
            url = "https://github.com/cvetojevichbojan/Icarion"
            connection = "scm:git:git://github.com/cvetojevichbojan/Icarion.git"
            developerConnection = "scm:git:ssh://git@github.com/cvetojevichbojan/Icarion.git"
        }
    }
}
