# TOP.TL Kotlin SDK

[![Maven Central](https://img.shields.io/maven-central/v/tl.top/toptl-kotlin)](https://central.sonatype.com/artifact/tl.top/toptl-kotlin)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin 1.9+](https://img.shields.io/badge/Kotlin-1.9%2B-purple)](https://kotlinlang.org/)

Official Kotlin SDK for the [TOP.TL](https://top.tl) Telegram Directory API. Built with coroutines, Ktor, and kotlinx.serialization.

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("tl.top:toptl-kotlin:1.0.0")
}
```

### Gradle (Groovy)

```groovy
implementation 'tl.top:toptl-kotlin:1.0.0'
```

## Quick Start

```kotlin
import tl.top.toptl.*
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val client = TopTL("your-api-token")

    // Get listing info
    val listing = client.getListing("mybot")
    println("${listing.title} has ${listing.votes} votes")

    // Get votes
    val votes = client.getVotes("mybot")
    println("Total votes: ${votes.total}")

    // Check if a user voted
    val check = client.hasVoted("mybot", "123456789")
    if (check.hasVoted) {
        println("User has voted!")
    }

    // Post stats
    client.postStats("mybot", StatsUpdate(memberCount = 50000))

    // Get global stats
    val stats = client.getStats()
    println("Total listings on TOP.TL: ${stats.totalListings}")

    client.close()
}
```

## Autoposter

Automatically post stats at a regular interval using coroutines:

```kotlin
import tl.top.toptl.*
import kotlin.time.Duration.Companion.minutes

val client = TopTL("your-api-token")

val autoposter = Autoposter(client, "mybot") {
    StatsUpdate(memberCount = bot.getMemberCount())
}

autoposter
    .onPost { stats -> println("Posted stats: $stats") }
    .onError { e -> println("Failed to post stats: ${e.message}") }
    .start(interval = 30.minutes)

// When shutting down:
autoposter.stop()
client.close()
```

## Requirements

- Kotlin 1.9+
- Java 11+

## License

[MIT](LICENSE) - TOP.TL
