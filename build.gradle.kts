plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    `maven-publish`
}

group = "tl.top"
artifactId = "toptl-kotlin"
version = "1.0.0"

repositories {
    mavenCentral()
}

val ktorVersion = "2.3.8"

dependencies {
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
}

kotlin {
    jvmToolchain(11)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "tl.top"
            artifactId = "toptl-kotlin"
            from(components["java"])

            pom {
                name.set("TOP.TL Kotlin SDK")
                description.set("Kotlin SDK for the TOP.TL Telegram Directory API")
                url.set("https://top.tl")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
            }
        }
    }
}

var artifactId: String
    get() = project.name
    set(value) { project.extra["artifactId"] = value }
