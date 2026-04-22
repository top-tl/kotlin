package io.github.toptl

import io.github.toptl.model.GlobalStats
import io.github.toptl.model.Listing
import io.github.toptl.model.StatsPayload
import io.github.toptl.model.VoteCheck
import io.github.toptl.model.Voter
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ModelsTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    @Test
    fun `decodes a Listing with unknown fields`() {
        val body = """
            {"id":"1","username":"mybot","title":"My Bot","type":"BOT",
             "memberCount":100,"voteCount":5,"verified":true,
             "extraServerField":"ignored"}
        """.trimIndent()
        val listing = json.decodeFromString(Listing.serializer(), body)
        assertEquals("mybot", listing.username)
        assertEquals(100L, listing.memberCount)
        assertTrue(listing.verified)
    }

    @Test
    fun `VoteCheck uses voted not hasVoted`() {
        val check = json.decodeFromString(
            VoteCheck.serializer(),
            """{"voted":true,"votedAt":"2026-04-20T00:00:00Z"}""",
        )
        assertTrue(check.voted)
        assertEquals("2026-04-20T00:00:00Z", check.votedAt)
    }

    @Test
    fun `GlobalStats uses total channels groups bots`() {
        val stats = json.decodeFromString(
            GlobalStats.serializer(),
            """{"total":89000,"channels":50000,"groups":30000,"bots":9000}""",
        )
        assertEquals(89000L, stats.total)
        assertEquals(9000L, stats.bots)
    }

    @Test
    fun `StatsPayload only serializes set fields`() {
        val encoded = json.encodeToString(
            StatsPayload.serializer(),
            StatsPayload(memberCount = 100, channelCount = 3),
        )
        assertTrue(encoded.contains("\"memberCount\":100"))
        assertTrue(encoded.contains("\"channelCount\":3"))
        assertFalse(encoded.contains("groupCount"))
        assertFalse(encoded.contains("botServes"))
    }

    @Test
    fun `Voter nullable fields tolerate missing data`() {
        val v = json.decodeFromString(
            Voter.serializer(),
            """{"userId":"123"}""",
        )
        assertEquals("123", v.userId)
        assertNull(v.firstName)
        assertNull(v.votedAt)
    }
}
