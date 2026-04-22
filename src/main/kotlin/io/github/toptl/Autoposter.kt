package io.github.toptl

import io.github.toptl.model.StatsPayload
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Fire-and-forget stats poster.
 *
 * Pulls a fresh [StatsPayload] from [statsProvider] every [interval] and
 * POSTs it to TOP.TL. Backed by a [SupervisorJob] so a single failure
 * never tears down the coroutine.
 *
 * ```kotlin
 * val toptl = TopTL(apiKey)
 * val autoposter = Autoposter(toptl, "mybot") {
 *     StatsPayload(memberCount = bot.memberCount())
 * }
 * autoposter
 *     .onPost { stats -> logger.info("posted $stats") }
 *     .onError { e     -> logger.warn("autopost failed", e) }
 *     .start()
 * ```
 */
class Autoposter(
    private val client: TopTL,
    private val username: String,
    private val statsProvider: suspend () -> StatsPayload,
) {
    private var job: Job? = null
    private var scope: CoroutineScope? = null
    private var onError: (suspend (Exception) -> Unit)? = null
    private var onPost: (suspend (StatsPayload) -> Unit)? = null

    /** Set a handler for failed post attempts. Fluent. */
    fun onError(handler: suspend (Exception) -> Unit): Autoposter {
        this.onError = handler
        return this
    }

    /** Set a callback fired after each successful post. Fluent. */
    fun onPost(handler: suspend (StatsPayload) -> Unit): Autoposter {
        this.onPost = handler
        return this
    }

    /**
     * Start posting. Idempotent within a single process — a second call
     * before [stop] throws.
     */
    fun start(
        interval: Duration = 30.minutes,
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
    ) {
        check(job == null) { "Autoposter is already running" }
        val newScope = CoroutineScope(dispatcher + SupervisorJob())
        scope = newScope
        job = newScope.launch {
            while (isActive) {
                try {
                    val stats = statsProvider()
                    client.postStats(username, stats)
                    onPost?.invoke(stats)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    onError?.invoke(e)
                }
                delay(interval)
            }
        }
    }

    /** Stop posting and tear down the scope. Safe to call repeatedly. */
    fun stop() {
        job?.cancel()
        job = null
        scope?.cancel()
        scope = null
    }
}
