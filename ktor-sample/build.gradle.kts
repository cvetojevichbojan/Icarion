plugins {
    alias(libs.plugins.kotlinJvm)

    id("application")
    id("io.ktor.plugin") version "2.3.3"
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
}

dependencies {
    implementation(project(":library"))

    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("ch.qos.logback:logback-classic:1.4.5")

}
