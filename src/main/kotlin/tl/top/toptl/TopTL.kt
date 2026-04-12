package tl.top.toptl

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.io.Closeable

/**
 * Kotlin client for the TOP.TL API.
 *
 * All methods are suspend functions designed for use with coroutines.
 *
 * ```kotlin
 * val client = TopTL("your-api-token")
 * val listing = client.getListing("mybot")
 * println("${listing.title} has ${listing.votes} votes")
 * client.close()
 * ```
 */
class TopTL(
    private val token: String,
    private val baseUrl: String = DEFAULT_BASE_URL
) : Closeable {

    companion object {
        const val DEFAULT_BASE_URL = "https://top.tl/api/v1"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(this@TopTL.json)
        }
        defaultRequest {
            header("Authorization", "Bearer $token")
            header("User-Agent", "toptl-kotlin/1.0.0")
            contentType(ContentType.Application.Json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
        }
    }

    /**
     * Gets listing information for the given username.
     */
    suspend fun getListing(username: String): Listing {
        return client.get("$baseUrl/listing/$username").body()
    }

    /**
     * Gets votes for the given listing.
     */
    suspend fun getVotes(username: String): VotesResponse {
        return client.get("$baseUrl/listing/$username/votes").body()
    }

    /**
     * Checks whether a user has voted for a listing.
     */
    suspend fun hasVoted(username: String, userId: String): VoteCheck {
        return client.get("$baseUrl/listing/$username/has-voted/$userId").body()
    }

    /**
     * Posts stats (member count, group count) for a listing.
     */
    suspend fun postStats(username: String, stats: StatsUpdate) {
        client.post("$baseUrl/listing/$username/stats") {
            setBody(stats)
        }
    }

    /**
     * Gets global TOP.TL statistics.
     */
    suspend fun getStats(): Stats {
        return client.get("$baseUrl/stats").body()
    }

    /**
     * Closes the underlying HTTP client.
     */
    override fun close() {
        client.close()
    }
}
