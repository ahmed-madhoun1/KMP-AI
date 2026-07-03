package dev.kmpai.core.internal

import dev.kmpai.core.AiClientConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Shared JSON configuration used across all provider DTOs. */
val sharedJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
    isLenient = true
    coerceInputValues = true
}

/**
 * Build a pre-configured Ktor [HttpClient] from [AiClientConfig].
 *
 * @param config Client configuration.
 * @param engine Optional custom engine (useful for testing with [MockEngine]).
 * @param providerHeaders Provider-specific headers to include on every request
 *   (e.g. Authorization, x-api-key). Merged with any headers in [config].
 */
internal fun buildHttpClient(
    config: AiClientConfig,
    engine: HttpClientEngine? = null,
    providerHeaders: Map<String, String> = emptyMap(),
): HttpClient {
    val client: HttpClient = if (engine != null) HttpClient(engine) else HttpClient()

    return client.config {
        install(ContentNegotiation) {
            json(sharedJson)
        }

        install(HttpTimeout) {
            requestTimeoutMillis = config.timeoutMs
            connectTimeoutMillis = 10_000L
            socketTimeoutMillis  = config.timeoutMs
        }

        install(HttpRequestRetry) {
            maxRetries = config.maxRetries
            retryOnServerErrors(maxRetries = config.maxRetries)
            retryOnException(maxRetries = config.maxRetries, retryOnTimeout = true)
            exponentialDelay(base = config.retryDelayMs.toDouble() / 1000)
        }

        install(DefaultRequest) {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            // Provider-specific auth headers (Authorization, x-api-key, etc.)
            providerHeaders.forEach { (key, value) -> header(key, value) }
            // Any custom headers the caller injected via config
            config.defaultHeaders.forEach { (key, value) -> header(key, value) }
        }

        if (config.httpLoggingEnabled) {
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        co.touchlab.kermit.Logger.d("KmpAi/HTTP") { message }
                    }
                }
                level = LogLevel.ALL
            }
        }
    }
}
