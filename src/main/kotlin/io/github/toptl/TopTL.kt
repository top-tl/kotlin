package io.github.toptl

import io.github.toptl.exception.ApiException
import io.github.toptl.exception.AuthenticationException
import io.github.toptl.exception.NotFoundException
import io.github.toptl.exception.RateLimitException
import io.github.toptl.exception.TopTLException
import io.github.toptl.exception.ValidationException
import io.github.toptl.model.ApiErrorBody
import io.github.toptl.model.BatchStatsItem
import io.github.toptl.model.GlobalStats
import io.github.toptl.model.Listing
import io.github.toptl.model.StatsPayload
import io.github.toptl.model.StatsResult
import io.github.toptl.model.VoteCheck
import io.github.toptl.model.Voter
import io.github.toptl.model.WebhookConfig
import io.github.toptl.model.WebhookTestResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.put
import kotlinx.serialization.serializer
import java.io.Closeable
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

/**
 * Coroutine-native client for the TOP.TL public API.
 *
 * Backed by the JDK 11+ [HttpClient] — no transitive transport
 * dependency. All calls are suspend functions; errors map to the sealed
 * [TopTLException] hierarchy.
 *
 * ```kotlin
 * TopTL("your-api-key").use { toptl ->
 *     val listing = toptl.getListing("mybot")
 *     println("${listing.title} — ${listing.voteCount} votes")
 * }
 * ```
 *
 * @param apiKey API key issued at https://top.tl/profile → API Keys.
 * @param baseUrl Override for self-hosted / staging. Defaults to the
 *                public API. Trailing slashes are trimmed.
 * @param timeout Per-request timeout. Default 15s.
 * @param userAgent Optional suffix appended to the SDK's UA header.
 */
class TopTL @JvmOverloads constructor(
    private val apiKey: String,
    baseUrl: String = DEFAULT_BASE_URL,
    private val timeout: Duration = Duration.ofSeconds(15),
    userAgent: String? = null,
) : Closeable {

    init {
        require(apiKey.isNotBlank()) { "apiKey is required" }
    }

    private val baseUrl: String = baseUrl.trimEnd('/')

    private val ua: String = buildString {
        append("toptl-kotlin/").append(SDK_VERSION)
        if (!userAgent.isNullOrBlank()) append(' ').append(userAgent)
    }

    private val http: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        explicitNulls = false
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /** `GET /v1/listing/{username}` — fetch a listing by Telegram username. */
    suspend fun getListing(username: String): Listing =
        request("GET", "/v1/listing/${enc(username)}", serializer<Listing>())

    /**
     * `GET /v1/listing/{username}/votes?limit={limit}` — recent voters.
     *
     * Server may return a bare array or `{"items": [...]}`; both are
     * accepted transparently.
     */
    suspend fun getVotes(username: String, limit: Int = 20): List<Voter> {
        val raw = requestJson(
            method = "GET",
            path = "/v1/listing/${enc(username)}/votes?limit=$limit",
            body = null,
        )
        val array: JsonArray = when (raw) {
            is JsonArray -> raw
            is JsonObject -> raw["items"]?.jsonArray ?: buildJsonArray { }
            else -> buildJsonArray { }
        }
        return array.map { json.decodeFromJsonElement(Voter.serializer(), it) }
    }

    /** `GET /v1/listing/{username}/has-voted/{userId}` */
    suspend fun hasVoted(username: String, userId: String): VoteCheck =
        request(
            "GET",
            "/v1/listing/${enc(username)}/has-voted/${enc(userId)}",
            serializer<VoteCheck>(),
        )

    /** Convenience overload — accepts numeric Telegram user IDs. */
    suspend fun hasVoted(username: String, userId: Long): VoteCheck =
        hasVoted(username, userId.toString())

    /**
     * `POST /v1/listing/{username}/stats` — update counters.
     *
     * At least one of [memberCount], [groupCount], [channelCount], or
     * [botServes] must be non-null.
     */
    suspend fun postStats(
        username: String,
        memberCount: Long? = null,
        groupCount: Long? = null,
        channelCount: Long? = null,
        botServes: List<String>? = null,
    ): StatsResult =
        postStats(
            username,
            StatsPayload(memberCount, groupCount, channelCount, botServes),
        )

    /** Same as above, using a pre-built [StatsPayload]. */
    suspend fun postStats(username: String, payload: StatsPayload): StatsResult {
        require(
            payload.memberCount != null ||
                payload.groupCount != null ||
                payload.channelCount != null ||
                payload.botServes != null,
        ) { "postStats needs at least one counter set on the payload" }
        val body = json.encodeToString(StatsPayload.serializer(), payload)
        return request(
            "POST",
            "/v1/listing/${enc(username)}/stats",
            serializer<StatsResult>(),
            body,
        )
    }

    /**
     * `POST /v1/stats/batch` — update up to 25 listings in one request.
     *
     * Failed items come back with `success=false` and an error message
     * in the corresponding [StatsResult] — the request itself still
     * returns 200.
     */
    suspend fun batchPostStats(items: List<BatchStatsItem>): List<StatsResult> {
        if (items.isEmpty()) return emptyList()
        val body = json.encodeToString(ListSerializer(BatchStatsItem.serializer()), items)
        return request(
            "POST",
            "/v1/stats/batch",
            ListSerializer(StatsResult.serializer()),
            body,
        )
    }

    /**
     * `PUT /v1/listing/{username}/webhook` — register a vote webhook.
     *
     * @param rewardTitle optional badge title shown in the voter's
     *                    confirmation notification.
     */
    suspend fun setWebhook(
        username: String,
        url: String,
        rewardTitle: String? = null,
    ): WebhookConfig {
        val body = buildJsonObject {
            put("url", url)
            if (rewardTitle != null) put("rewardTitle", rewardTitle)
        }.toString()
        return request(
            "PUT",
            "/v1/listing/${enc(username)}/webhook",
            serializer<WebhookConfig>(),
            body,
        )
    }

    /** `POST /v1/listing/{username}/webhook/test` — send a synthetic vote. */
    suspend fun testWebhook(username: String): WebhookTestResult =
        request(
            "POST",
            "/v1/listing/${enc(username)}/webhook/test",
            serializer<WebhookTestResult>(),
            body = "",
        )

    /** `GET /v1/stats` — site-wide totals. */
    suspend fun getGlobalStats(): GlobalStats =
        request("GET", "/v1/stats", serializer<GlobalStats>())

    override fun close() {
        // JDK HttpClient has no explicit close on JDK 17 (added in 21);
        // dropping the reference is enough for the selector thread to
        // shut down once idle.
    }

    // ------------------------------------------------------------------
    // Internal transport
    // ------------------------------------------------------------------

    private suspend fun <T> request(
        method: String,
        path: String,
        deserializer: KSerializer<T>,
        body: String? = null,
    ): T {
        val raw = requestRaw(method, path, body)
        return try {
            json.decodeFromString(deserializer, raw)
        } catch (e: Exception) {
            throw ApiException("Failed to decode response: ${e.message}", cause = e)
        }
    }

    private suspend fun requestJson(
        method: String,
        path: String,
        body: String?,
    ): JsonElement {
        val raw = requestRaw(method, path, body)
        return try {
            json.parseToJsonElement(raw)
        } catch (e: Exception) {
            throw ApiException("Failed to parse JSON: ${e.message}", cause = e)
        }
    }

    private suspend fun requestRaw(method: String, path: String, body: String?): String {
        val url = "$baseUrl$path"
        val builder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(timeout)
            .header("Authorization", "Bearer $apiKey")
            .header("User-Agent", ua)
            .header("Accept", "application/json")

        if (body != null) {
            builder.header("Content-Type", "application/json")
            builder.method(method, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody())
        }

        val response: HttpResponse<String> = try {
            withContext(Dispatchers.IO) {
                http.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                    .await()
            }
        } catch (e: TopTLException) {
            throw e
        } catch (e: Exception) {
            throw ApiException("Transport failure contacting $url: ${e.message}", cause = e)
        }

        val status = response.statusCode()
        val text = response.body() ?: ""
        if (status in 200..299) return text
        throw mapError(status, text, url)
    }

    private fun mapError(status: Int, body: String, url: String): TopTLException {
        val parsed = runCatching {
            json.decodeFromString(ApiErrorBody.serializer(), body)
        }.getOrNull()
        val message = (parsed?.message ?: parsed?.error ?: "HTTP $status").let { "$it ($url)" }
        return when (status) {
            401, 403 -> AuthenticationException(message, status, body)
            404 -> NotFoundException(message, status, body)
            429 -> RateLimitException(message, status, body)
            in 400..499 -> ValidationException(message, status, body)
            else -> ApiException(message, status, body)
        }
    }

    private fun enc(s: String): String =
        URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20")

    companion object {
        const val DEFAULT_BASE_URL: String = "https://top.tl/api"
        const val SDK_VERSION: String = "0.1.0"
    }
}
