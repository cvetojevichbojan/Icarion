@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    base
    alias(libs.plugins.kotlinJvm)
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
