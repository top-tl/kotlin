package io.github.toptl.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A TOP.TL listing — a channel, group, or bot indexed by its Telegram
 * username. Extra fields added server-side are preserved through the
 * JSON deserializer (unknown keys ignored).
 */
@Serializable
data class Listing(
    val id: String = "",
    val username: String = "",
    val title: String = "",
    /** `CHANNEL`, `GROUP`, or `BOT`. */
    val type: String = "",
    val description: String? = null,
    val memberCount: Long = 0,
    val voteCount: Long = 0,
    val languages: List<String> = emptyList(),
    val verified: Boolean = false,
    val featured: Boolean = false,
    val photoUrl: String? = null,
)

/**
 * A single voter entry returned by `GET /v1/listing/{username}/votes`.
 *
 * The server historically returned `createdAt` and has since migrated to
 * `votedAt`; we accept both by exposing only one canonical field and
 * letting the wire format fill it.
 */
@Serializable
data class Voter(
    val userId: String = "",
    val firstName: String? = null,
    val username: String? = null,
    val votedAt: String? = null,
)

/** Response shape of `GET /v1/listing/{username}/has-voted/{userId}`. */
@Serializable
data class VoteCheck(
    val voted: Boolean = false,
    val votedAt: String? = null,
)

/**
 * Payload for `POST /v1/listing/{username}/stats`.
 *
 * All fields are nullable — only those set on the request body reach the
 * server, so callers can update a single counter without clobbering the
 * others. `botServes` is a list of usernames (channels/groups) the bot
 * operates in, used for the listing's trust signals.
 */
@Serializable
data class StatsPayload(
    val memberCount: Long? = null,
    val groupCount: Long? = null,
    val channelCount: Long? = null,
    val botServes: List<String>? = null,
)

/**
 * A single entry in the `POST /v1/stats/batch` request array.
 *
 * Identical to [StatsPayload] plus a required `username` so one request
 * can update many listings atomically.
 */
@Serializable
data class BatchStatsItem(
    val username: String,
    val memberCount: Long? = null,
    val groupCount: Long? = null,
    val channelCount: Long? = null,
    val botServes: List<String>? = null,
)

/** Generic ack returned by stats endpoints. */
@Serializable
data class StatsResult(
    val success: Boolean = true,
    val username: String? = null,
    val error: String? = null,
)

/** Response of `PUT /v1/listing/{username}/webhook`. */
@Serializable
data class WebhookConfig(
    val url: String? = null,
    val rewardTitle: String? = null,
)

/** Response of `POST /v1/listing/{username}/webhook/test`. */
@Serializable
data class WebhookTestResult(
    val success: Boolean = false,
    val statusCode: Int? = null,
    val message: String? = null,
)

/**
 * Site-wide totals from `GET /v1/stats`.
 *
 * Note: the API returns `total / channels / groups / bots`, NOT the
 * `totalListings / totalVotes / totalUsers` names that showed up in
 * earlier SDK drafts.
 */
@Serializable
data class GlobalStats(
    val total: Long = 0,
    val channels: Long = 0,
    val groups: Long = 0,
    val bots: Long = 0,
)

/**
 * Raw error envelope the API returns on 4xx/5xx responses. Used
 * internally to extract a human-readable message for exceptions.
 */
@Serializable
internal data class ApiErrorBody(
    val message: String? = null,
    val error: String? = null,
    @SerialName("statusCode") val statusCode: Int? = null,
)
