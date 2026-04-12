package tl.top.toptl

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Listing(
    @SerialName("username") val username: String = "",
    @SerialName("title") val title: String = "",
    @SerialName("description") val description: String = "",
    @SerialName("category") val category: String = "",
    @SerialName("memberCount") val memberCount: Long? = null,
    @SerialName("votes") val votes: Long = 0,
    @SerialName("featured") val featured: Boolean = false,
    @SerialName("verified") val verified: Boolean = false,
    @SerialName("createdAt") val createdAt: String = ""
)

@Serializable
data class VotesResponse(
    @SerialName("votes") val votes: List<Vote> = emptyList(),
    @SerialName("total") val total: Long = 0
)

@Serializable
data class Vote(
    @SerialName("userId") val userId: String = "",
    @SerialName("timestamp") val timestamp: String = ""
)

@Serializable
data class VoteCheck(
    @SerialName("hasVoted") val hasVoted: Boolean = false,
    @SerialName("timestamp") val timestamp: String? = null
)

@Serializable
data class Stats(
    @SerialName("totalListings") val totalListings: Long = 0,
    @SerialName("totalVotes") val totalVotes: Long = 0,
    @SerialName("totalUsers") val totalUsers: Long = 0
)

@Serializable
data class StatsUpdate(
    @SerialName("memberCount") val memberCount: Long? = null,
    @SerialName("groupCount") val groupCount: Long? = null
)
