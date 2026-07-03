package dev.kmpai.core

/**
 * Configuration for an [AiClient] instance.
 *
 * @param apiKey The provider API key. Required for all cloud providers.
 * @param baseUrl Override the provider's default base URL (e.g. for proxies or self-hosted models).
 * @param organization Provider-specific organization or project ID (e.g. OpenAI org ID).
 * @param timeoutMs HTTP request timeout in milliseconds. Default: 30 seconds.
 * @param maxRetries Number of automatic retries on retryable errors (429, 5xx). Default: 3.
 * @param retryDelayMs Initial delay between retries in milliseconds. Doubles on each retry.
 * @param httpLoggingEnabled Whether to log raw HTTP requests and responses. Disable in production.
 * @param defaultHeaders Additional headers sent with every request.
 */
data class AiClientConfig(
    val apiKey: String,
    val baseUrl: String? = null,
    val organization: String? = null,
    val timeoutMs: Long = 30_000L,
    val maxRetries: Int = 3,
    val retryDelayMs: Long = 1_000L,
    val httpLoggingEnabled: Boolean = false,
    val defaultHeaders: Map<String, String> = emptyMap(),
)
