package dev.kmpai.openai

import dev.kmpai.core.AiClient
import dev.kmpai.core.AiClientConfig
import dev.kmpai.core.KmpAi
import dev.kmpai.core.error.AiException
import dev.kmpai.core.internal.buildHttpClient
import dev.kmpai.core.internal.sharedJson
import dev.kmpai.core.models.AiModel
import dev.kmpai.core.models.AiProvider
import dev.kmpai.core.models.ChatRequest
import dev.kmpai.core.models.ChatResponse
import dev.kmpai.core.models.EmbeddingRequest
import dev.kmpai.core.models.EmbeddingResponse
import dev.kmpai.core.models.StreamChunk
import dev.kmpai.openai.internal.dto.OpenAiChatResponse
import dev.kmpai.openai.internal.dto.OpenAiEmbeddingResponse
import dev.kmpai.openai.internal.dto.OpenAiErrorResponse
import dev.kmpai.openai.internal.dto.OpenAiModelsResponse
import dev.kmpai.openai.internal.dto.OpenAiStreamResponse
import dev.kmpai.openai.internal.mappers.toCoreResponse
import dev.kmpai.openai.internal.mappers.toOpenAiRequest
import dev.kmpai.openai.internal.mappers.toStreamChunk
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * OpenAI provider implementation of [AiClient].
 *
 * Create via the factory extension on [KmpAi]:
 * ```kotlin
 * val client = KmpAi.openAi { apiKey = "sk-..." }
 * ```
 */
class OpenAiClient internal constructor(
    private val config: AiClientConfig,
    httpClientEngine: HttpClientEngine? = null,
) : AiClient {

    override val provider = AiProvider(
        id = "openai",
        name = "OpenAI",
        baseUrl = config.baseUrl ?: "https://api.openai.com/v1",
    )

    private val httpClient = buildHttpClient(
        config = config,
        engine = httpClientEngine,
        providerHeaders = mapOf("Authorization" to "Bearer ${config.apiKey}"),
    )

    override suspend fun chat(request: ChatRequest): ChatResponse {
        return try {
            httpClient.post("${provider.baseUrl}/chat/completions") {
                setBody(request.toOpenAiRequest())
            }.body<OpenAiChatResponse>().toCoreResponse(provider)
        } catch (e: ClientRequestException) {
            throw e.toAiException(provider.name)
        } catch (e: ServerResponseException) {
            throw AiException.ProviderException(
                e.message ?: "Server error", provider.name, e.response.status.value,
            )
        } catch (e: Exception) {
            throw AiException.NetworkException(e.message ?: "Network error", e)
        }
    }

    override fun chatStream(request: ChatRequest): Flow<StreamChunk> = flow {
        try {
            httpClient.preparePost("${provider.baseUrl}/chat/completions") {
                setBody(request.copy(stream = true).toOpenAiRequest())
            }.execute { response ->
                val channel = response.bodyAsChannel()
                while (!channel.isClosedForRead) {
                    val line = channel.readUTF8Line() ?: break
                    if (line.startsWith("data: ")) {
                        val data = line.removePrefix("data: ").trim()
                        if (data == "[DONE]") break
                        if (data.isNotEmpty()) {
                            try {
                                val chunk = sharedJson.decodeFromString<OpenAiStreamResponse>(data)
                                emit(chunk.toStreamChunk())
                            } catch (_: Exception) {
                                // Skip malformed chunks
                            }
                        }
                    }
                }
            }
        } catch (e: ClientRequestException) {
            throw e.toAiException(provider.name)
        } catch (e: Exception) {
            throw AiException.NetworkException(e.message ?: "Stream error", e)
        }
    }

    override suspend fun embed(request: EmbeddingRequest): EmbeddingResponse {
        return try {
            httpClient.post("${provider.baseUrl}/embeddings") {
                setBody(request.toOpenAiRequest())
            }.body<OpenAiEmbeddingResponse>().toCoreResponse()
        } catch (e: ClientRequestException) {
            throw e.toAiException(provider.name)
        } catch (e: Exception) {
            throw AiException.NetworkException(e.message ?: "Network error", e)
        }
    }

    override suspend fun listModels(): List<AiModel> {
        return try {
            httpClient.get("${provider.baseUrl}/models")
                .body<OpenAiModelsResponse>()
                .data.map { it.toAiModel(provider) }
        } catch (e: Exception) {
            throw AiException.NetworkException(e.message ?: "Network error", e)
        }
    }

    override fun close() = httpClient.close()
}

private suspend fun ClientRequestException.toAiException(providerName: String): AiException {
    val body     = try { response.bodyAsText() } catch (_: Exception) { message ?: "" }
    val errorMsg = try {
        sharedJson.decodeFromString<OpenAiErrorResponse>(body).error.message
    } catch (_: Exception) { body }

    return when (response.status) {
        HttpStatusCode.Unauthorized   -> AiException.AuthenticationException(errorMsg, providerName)
        HttpStatusCode.TooManyRequests -> {
            val retryAfter = response.headers["Retry-After"]?.toIntOrNull()
            AiException.RateLimitException(errorMsg, providerName, retryAfter)
        }
        else -> AiException.InvalidRequestException(errorMsg, providerName, response.status.value)
    }
}

/** Factory extension: create an [OpenAiClient] from [KmpAi]. */
fun KmpAi.openAi(block: OpenAiClientConfigBuilder.() -> Unit): OpenAiClient =
    OpenAiClient(OpenAiClientConfigBuilder().apply(block).build())
