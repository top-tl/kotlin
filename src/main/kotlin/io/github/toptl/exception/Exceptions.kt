package io.github.toptl.exception

/**
 * Base class for every exception the TOP.TL SDK raises.
 *
 * Sealed so callers can exhaustively `when`-switch on subtype:
 *
 * ```kotlin
 * try { client.getListing(name) }
 * catch (e: TopTLException) {
 *     when (e) {
 *         is AuthenticationException -> refreshToken()
 *         is NotFoundException       -> createListing(name)
 *         is RateLimitException      -> backoffAndRetry()
 *         is ValidationException     -> log("bad input", e)
 *     }
 * }
 * ```
 */
sealed class TopTLException(
    message: String,
    val statusCode: Int? = null,
    val responseBody: String? = null,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/** 401 / 403 — invalid, missing, or under-scoped API key. */
class AuthenticationException(
    message: String,
    statusCode: Int? = null,
    responseBody: String? = null,
    cause: Throwable? = null,
) : TopTLException(message, statusCode, responseBody, cause)

/** 404 — listing or resource does not exist. */
class NotFoundException(
    message: String,
    statusCode: Int? = null,
    responseBody: String? = null,
    cause: Throwable? = null,
) : TopTLException(message, statusCode, responseBody, cause)

/** 429 — API rate limit hit. Back off and retry. */
class RateLimitException(
    message: String,
    statusCode: Int? = null,
    responseBody: String? = null,
    cause: Throwable? = null,
) : TopTLException(message, statusCode, responseBody, cause)

/** Other 4xx — request payload rejected by the server. */
class ValidationException(
    message: String,
    statusCode: Int? = null,
    responseBody: String? = null,
    cause: Throwable? = null,
) : TopTLException(message, statusCode, responseBody, cause)

/** 5xx / transport / deserialization failures. */
class ApiException(
    message: String,
    statusCode: Int? = null,
    responseBody: String? = null,
    cause: Throwable? = null,
) : TopTLException(message, statusCode, responseBody, cause)
