package dev.kmpai.openai

import dev.kmpai.core.AiClientConfig

/** DSL builder for OpenAI client configuration. */
class OpenAiClientConfigBuilder {
    var apiKey: String = ""
    var organization: String? = null
    var project: String? = null
    var baseUrl: String? = null
    var timeoutMs: Long = 30_000L
    var maxRetries: Int = 3
    var httpLoggingEnabled: Boolean = false

    fun build(): AiClientConfig = AiClientConfig(
        apiKey = apiKey,
        baseUrl = baseUrl,
        organization = organization,
        timeoutMs = timeoutMs,
        maxRetries = maxRetries,
        httpLoggingEnabled = httpLoggingEnabled,
        defaultHeaders = buildMap {
            if (organization != null) put("OpenAI-Organization", organization!!)
            if (project != null) put("OpenAI-Project", project!!)
        },
    )
}
