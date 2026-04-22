import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
    id("com.vanniktech.maven.publish") version "0.30.0"
    signing
}

group = "io.github.top-tl"
version = "0.1.0"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.9.0")

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}

tasks.test {
    useJUnitPlatform()
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    signAllPublications()
    coordinates("io.github.top-tl", "toptl-kotlin", "0.1.0")

    pom {
        name.set("TOP.TL Kotlin SDK")
        description.set(
            "Official Kotlin SDK for the TOP.TL Telegram directory API — " +
                "post bot stats, check votes, manage vote webhooks.",
        )
        inceptionYear.set("2026")
        url.set("https://top.tl")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("toptl")
                name.set("TOP.TL")
                email.set("hello@top.tl")
                url.set("https://top.tl")
            }
        }
        scm {
            connection.set("scm:git:https://github.com/top-tl/kotlin.git")
            developerConnection.set("scm:git:git@github.com:top-tl/kotlin.git")
            url.set("https://github.com/top-tl/kotlin")
        }
    }
}

signing {
    // Use the gpg binary so the agent key (no passphrase) is picked up
    // from ~/.gnupg without requiring key material in gradle.properties.
    useGpgCmd()
}
