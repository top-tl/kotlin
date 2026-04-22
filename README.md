# TOP.TL Kotlin SDK

[![Maven Central](https://img.shields.io/maven-central/v/io.github.top-tl/toptl-kotlin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.top-tl/toptl-kotlin)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blueviolet.svg?logo=kotlin)](https://kotlinlang.org/)
[![JVM](https://img.shields.io/badge/JVM-17%2B-orange.svg)](https://adoptium.net/)
[![TOP.TL](https://img.shields.io/badge/TOP.TL-directory-0088cc.svg)](https://top.tl)

Official Kotlin SDK for the [TOP.TL](https://top.tl) Telegram directory API. Coroutine-native, kotlinx.serialization-backed, zero transport dependencies (uses the JDK 17 `HttpClient`).

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("io.github.top-tl:toptl-kotlin:0.1.0")
}
```

### Gradle (Groovy)

```groovy
implementation 'io.github.top-tl:toptl-kotlin:0.1.0'
```

### Maven

```xml
<dependency>
    <groupId>io.github.top-tl</groupId>
    <artifactId>toptl-kotlin</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Quick start

```kotlin
import io.github.toptl.TopTL
import io.github.toptl.model.StatsPayload
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    TopTL("your-api-key").use { toptl ->
        val listing = toptl.getListing("mybot")
        println("${listing.title} — ${listing.voteCount} votes")

        val recent = toptl.getVotes("mybot", limit = 10)
        recent.forEach { println("  ${it.firstName} @ ${it.votedAt}") }

        val check = toptl.hasVoted("mybot", 123456789L)
        if (check.voted) println("already voted at ${check.votedAt}")

        toptl.postStats(
            username = "mybot",
            memberCount = 50_000,
            groupCount = 120,
            channelCount = 3,
        )

        val global = toptl.getGlobalStats()
        println("TOP.TL indexes ${global.total} listings")
    }
}
```

## Endpoints

| Method | Path | SDK |
|---|---|---|
| GET  | `/v1/listing/{username}` | `getListing(username)` |
| GET  | `/v1/listing/{username}/votes?limit=N` | `getVotes(username, limit)` |
| GET  | `/v1/listing/{username}/has-voted/{userId}` | `hasVoted(username, userId)` |
| POST | `/v1/listing/{username}/stats` | `postStats(username, ...)` |
| POST | `/v1/stats/batch` | `batchPostStats(items)` |
| PUT  | `/v1/listing/{username}/webhook` | `setWebhook(username, url, rewardTitle)` |
| POST | `/v1/listing/{username}/webhook/test` | `testWebhook(username)` |
| GET  | `/v1/stats` | `getGlobalStats()` |

## Autoposter

Post stats on a timer from any coroutine context:

```kotlin
import io.github.toptl.Autoposter
import io.github.toptl.TopTL
import io.github.toptl.model.StatsPayload
import kotlin.time.Duration.Companion.minutes

val toptl = TopTL("your-api-key")

val autoposter = Autoposter(toptl, "mybot") {
    StatsPayload(memberCount = bot.memberCount())
}
autoposter
    .onPost { stats -> logger.info("posted $stats") }
    .onError { e -> logger.warn("autopost failed", e) }
    .start(interval = 30.minutes)
```

## Error handling

Every failure surfaces as a subclass of `io.github.toptl.exception.TopTLException`:

| HTTP | Exception |
|---|---|
| 401 / 403 | `AuthenticationException` |
| 404 | `NotFoundException` |
| 429 | `RateLimitException` |
| other 4xx | `ValidationException` |
| 5xx / transport / decode | `ApiException` |

The class is `sealed` — pattern-match exhaustively in `when`.

## Requirements

- Kotlin 2.0+
- JVM 17+

## License

[MIT](LICENSE) — TOP.TL
