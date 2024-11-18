import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.kotlinJvm)
    id("com.vanniktech.maven.publish") version "0.29.0"
}

group = "xyz.amplituhedron"
val artifactId = "icarion"
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

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.2")

    testImplementation(kotlin("test"))
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
        scm {
            url = "https://github.com/cvetojevichbojan/Icarion"
            connection = "scm:git:git://github.com/cvetojevichbojan/Icarion.git"
            developerConnection = "scm:git:ssh://git@github.com/cvetojevichbojan/Icarion.git"
        }
    }
}
