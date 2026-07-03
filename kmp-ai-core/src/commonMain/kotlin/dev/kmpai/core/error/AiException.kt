package dev.kmpai.core.error

/**
 * Base class for all exceptions thrown by KMP AI clients.
 *
 * Use a `when` expression to handle specific error types:
 *
 * ```kotlin
 * try {
 *     client.chat(request)
 * } catch (e: AiException) {
 *     when (e) {
 *         is AiException.AuthenticationException -> // invalid API key
 *         is AiException.RateLimitException -> // back off and retry
 *         is AiException.InvalidRequestException -> // fix the request
 *         is AiException.ProviderException -> // provider outage
 *         is AiException.NetworkException -> // connectivity issue
 *         else -> // unexpected
 *     }
 * }
 * ```
 */
sealed class AiException(message: String, cause: Throwable? = null) :
    Exception(message, cause) {

    /**
     * HTTP 401 — the API key is missing, invalid, or revoked.
     */
    class AuthenticationException(
        message: String,
        val provider: String,
    ) : AiException("[$provider] Authentication failed: $message")

    /**
     * HTTP 429 — the rate limit has been exceeded.
     *
     * @param retryAfterSeconds Suggested wait time before retrying, if provided by the API.
     */
    class RateLimitException(
        message: String,
        val provider: String,
        val retryAfterSeconds: Int? = null,
    ) : AiException("[$provider] Rate limit exceeded: $message")

    /**
     * HTTP 4xx (not 401 or 429) — the request was malformed or invalid.
     *
     * @param statusCode The HTTP status code returned.
     * @param providerErrorCode Provider-specific error code string, if available.
     */
    class InvalidRequestException(
        message: String,
        val provider: String,
        val statusCode: Int,
        val providerErrorCode: String? = null,
    ) : AiException("[$provider] Invalid request ($statusCode): $message")

    /**
     * HTTP 5xx — the provider's server returned an error.
     */
    class ProviderException(
        message: String,
        val provider: String,
        val statusCode: Int,
    ) : AiException("[$provider] Provider error ($statusCode): $message")

    /**
     * A network or timeout error prevented the request from completing.
     */
    class NetworkException(
        message: String,
        cause: Throwable,
    ) : AiException("Network error: $message", cause)

    /**
     * The provider's response could not be parsed.
     */
    class SerializationException(
        message: String,
        cause: Throwable? = null,
    ) : AiException("Failed to parse response: $message", cause)

    /**
     * The requested operation is not supported by this provider.
     */
    class UnsupportedOperationException(
        message: String,
        val provider: String,
    ) : AiException("[$provider] Unsupported: $message")
}
