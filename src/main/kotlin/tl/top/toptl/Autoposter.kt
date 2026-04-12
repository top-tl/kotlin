package tl.top.toptl

import kotlinx.coroutines.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Automatically posts stats to TOP.TL at a fixed interval using coroutines.
 *
 * ```kotlin
 * val client = TopTL("your-api-token")
 * val autoposter = Autoposter(client, "mybot") {
 *     StatsUpdate(memberCount = bot.getMemberCount())
 * }
 * autoposter.start(interval = 30.minutes)
 *
 * // Later, when shutting down:
 * autoposter.stop()
 * client.close()
 * ```
 */
class Autoposter(
    private val client: TopTL,
    private val username: String,
    private val statsProvider: suspend () -> StatsUpdate
) {
    private var job: Job? = null
    private var scope: CoroutineScope? = null
    private var onError: (suspend (Exception) -> Unit)? = null
    private var onPost: (suspend (StatsUpdate) -> Unit)? = null

    /**
     * Sets an error handler for failed post attempts.
     */
    fun onError(handler: suspend (Exception) -> Unit): Autoposter {
        this.onError = handler
        return this
    }

    /**
     * Sets a callback invoked after each successful post.
     */
    fun onPost(handler: suspend (StatsUpdate) -> Unit): Autoposter {
        this.onPost = handler
        return this
    }

    /**
     * Starts the autoposter with the given interval.
     *
     * @param interval duration between posts (default: 30 minutes)
     * @param dispatcher the coroutine dispatcher to use
     */
    fun start(
        interval: Duration = 30.minutes,
        dispatcher: CoroutineDispatcher = Dispatchers.Default
    ) {
        if (job != null) {
            throw IllegalStateException("Autoposter is already running")
        }

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

    /**
     * Stops the autoposter and cancels the coroutine scope.
     */
    fun stop() {
        job?.cancel()
        job = null
        scope?.cancel()
        scope = null
    }
}
