plugins {
    alias(libs.plugins.kotlinJvm)
}

group = "xyz.amplituhedron"
version = "1.0"

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

dependencies {

    testImplementation(platform(libs.junit.platform))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.strikt.core)
    testImplementation(kotlin("test"))
}