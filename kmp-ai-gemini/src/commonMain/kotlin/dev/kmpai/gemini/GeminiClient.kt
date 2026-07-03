package dev.kmpai.gemini

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
import dev.kmpai.core.models.Choice
import dev.kmpai.core.models.EmbeddingRequest
import dev.kmpai.core.models.EmbeddingResponse
import dev.kmpai.core.models.FinishReason
import dev.kmpai.core.models.Message
import dev.kmpai.core.models.MessageContent
import dev.kmpai.core.models.MessageDelta
import dev.kmpai.core.models.Role
import dev.kmpai.core.models.StreamChunk
import dev.kmpai.core.models.Usage
import dev.kmpai.core.models.asText
import dev.kmpai.gemini.internal.dto.GeminiBatchEmbedRequest
import dev.kmpai.gemini.internal.dto.GeminiBatchEmbedResponse
import dev.kmpai.gemini.internal.dto.GeminiContent
import dev.kmpai.gemini.internal.dto.GeminiEmbedRequest
import dev.kmpai.gemini.internal.dto.GeminiGenerateRequest
import dev.kmpai.gemini.internal.dto.GeminiGenerateResponse
import dev.kmpai.gemini.internal.dto.GeminiGenerationConfig
import dev.kmpai.gemini.internal.dto.GeminiModelData
import dev.kmpai.gemini.internal.dto.GeminiModelsResponse
import dev.kmpai.gemini.internal.dto.GeminiPart
import dev.kmpai.gemini.internal.dto.GeminiTool
import dev.kmpai.gemini.internal.dto.GeminiFunctionDeclaration
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Google Gemini provider implementation of [AiClient].
 *
 * Create via the factory extension on [KmpAi]:
 * ```kotlin
 * val client = KmpAi.gemini { apiKey = "AIza..." }
 * ```
 */
class GeminiClient internal constructor(
    private val config: AiClientConfig,
    httpClientEngine: HttpClientEngine? = null,
) : AiClient {

    override val provider = AiProvider(
        id      = "gemini",
        name    = "Google Gemini",
        baseUrl = config.baseUrl ?: "https://generativelanguage.googleapis.com/v1beta",
    )

    private val httpClient = buildHttpClient(config, httpClientEngine)

    // Gemini uses an API key query param, not a header — no providerHeaders needed.

    override suspend fun chat(request: ChatRequest): ChatResponse {
        val dto       = request.toGeminiRequest()
        val modelPath = "models/${request.model}:generateContent"
        return try {
            httpClient.post("${provider.baseUrl}/$modelPath") {
                parameter("key", config.apiKey)
                setBody(dto)
            }.body<GeminiGenerateResponse>().toCoreResponse(provider, request.model)
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
        val dto       = request.toGeminiRequest()
        val modelPath = "models/${request.model}:streamGenerateContent"
        try {
            httpClient.preparePost("${provider.baseUrl}/$modelPath") {
                parameter("key", config.apiKey)
                parameter("alt", "sse")
                setBody(dto)
            }.execute { response ->
                val channel  = response.bodyAsChannel()
                var chunkId  = 0
                while (!channel.isClosedForRead) {
                    val line = channel.readUTF8Line() ?: break
                    if (line.startsWith("data: ")) {
                        val data = line.removePrefix("data: ").trim()
                        if (data.isNotEmpty() && data != "[DONE]") {
                            try {
                                val chunk       = sharedJson.decodeFromString<GeminiGenerateResponse>(data)
                                val candidate   = chunk.candidates?.firstOrNull() ?: continue
                                val text        = candidate.content?.parts?.firstOrNull()?.text ?: continue
                                val finishReason = candidate.finishReason
                                emit(StreamChunk(
                                    id           = "gemini-stream-${chunkId++}",
                                    delta        = MessageDelta(content = text),
                                    finishReason = finishReason?.toFinishReason(),
                                ))
                            } catch (_: Exception) { /* skip malformed chunks */ }
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
        val batchRequest = GeminiBatchEmbedRequest(
            requests = request.input.map { text ->
                GeminiEmbedRequest(
                    model   = "models/${request.model}",
                    content = GeminiContent(parts = listOf(GeminiPart(text = text))),
                )
            }
        )
        return try {
            val response = httpClient
                .post("${provider.baseUrl}/models/${request.model}:batchEmbedContents") {
                    parameter("key", config.apiKey)
                    setBody(batchRequest)
                }.body<GeminiBatchEmbedResponse>()
            EmbeddingResponse(
                model      = request.model,
                embeddings = response.embeddings.map { it.values },
                usage      = Usage(0, 0, 0),
            )
        } catch (e: Exception) {
            throw AiException.NetworkException(e.message ?: "Embedding error", e)
        }
    }

    override suspend fun listModels(): List<AiModel> {
        return try {
            httpClient.get("${provider.baseUrl}/models") {
                parameter("key", config.apiKey)
            }.body<GeminiModelsResponse>().models.map { it.toAiModel(provider) }
        } catch (e: Exception) {
            throw AiException.NetworkException(e.message ?: "Network error", e)
        }
    }

    override fun close() = httpClient.close()

    private fun ChatRequest.toGeminiRequest(): GeminiGenerateRequest {
        val systemMsg    = messages.firstOrNull { it.role == Role.system }
        val conversation = messages.filter { it.role != Role.system }
        return GeminiGenerateRequest(
            contents = conversation.map { msg ->
                GeminiContent(
                    role  = if (msg.role == Role.assistant) "model" else "user",
                    parts = listOf(GeminiPart(text = msg.content.asText() ?: "")),
                )
            },
            systemInstruction = systemMsg?.let { sm ->
                GeminiContent(parts = listOf(GeminiPart(text = sm.content.asText() ?: "")))
            },
            generationConfig = GeminiGenerationConfig(
                temperature     = temperature,
                maxOutputTokens = maxTokens,
                topP            = topP,
                stopSequences   = stop,
            ),
            tools = tools?.let { toolList ->
                listOf(GeminiTool(
                    functionDeclarations = toolList.map { t ->
                        GeminiFunctionDeclaration(
                            name        = t.function.name,
                            description = t.function.description,
                            parameters  = t.function.parameters,
                        )
                    }
                ))
            },
        )
    }
}

private fun GeminiGenerateResponse.toCoreResponse(provider: AiProvider, model: String): ChatResponse {
    val candidate = candidates?.firstOrNull()
    val text      = candidate?.content?.parts?.firstOrNull()?.text ?: ""
    return ChatResponse(
        id       = "gemini-response",   // Gemini API doesn't return a request ID
        model    = model,
        provider = provider,
        choices  = listOf(
            Choice(
                index        = 0,
                message      = Message(Role.assistant, MessageContent.Text(text)),
                finishReason = candidate?.finishReason?.toFinishReason(),
            )
        ),
        usage = usageMetadata?.let {
            Usage(it.promptTokenCount, it.candidatesTokenCount, it.totalTokenCount)
        },
    )
}

private fun String.toFinishReason(): FinishReason = when (this) {
    "STOP"       -> FinishReason.stop
    "MAX_TOKENS" -> FinishReason.length
    "SAFETY"     -> FinishReason.content_filter
    else         -> FinishReason.stop
}

private suspend fun ClientRequestException.toAiException(providerName: String): AiException =
    when (response.status) {
        HttpStatusCode.Unauthorized    -> AiException.AuthenticationException(message ?: "Unauthorized", providerName)
        HttpStatusCode.TooManyRequests -> AiException.RateLimitException(message ?: "Rate limited", providerName)
        else -> AiException.InvalidRequestException(message ?: "Request error", providerName, response.status.value)
    }

private fun GeminiModelData.toAiModel(provider: AiProvider): AiModel = AiModel(
    id                 = name.removePrefix("models/"),
    provider           = provider,
    contextWindow      = inputTokenLimit,
    maxOutputTokens    = outputTokenLimit,
    supportsStreaming   = "generateContent" in supportedGenerationMethods,
    supportsTools      = true,
    supportsVision     = true,
    supportsEmbeddings = "embedContent" in supportedGenerationMethods,
)

/** DSL config builder for Gemini. */
class GeminiClientConfigBuilder {
    var apiKey             : String  = ""
    var baseUrl            : String? = null
    var timeoutMs          : Long    = 30_000L
    var maxRetries         : Int     = 3
    var httpLoggingEnabled : Boolean = false

    fun build() = AiClientConfig(
        apiKey             = apiKey,
        baseUrl            = baseUrl,
        timeoutMs          = timeoutMs,
        maxRetries         = maxRetries,
        httpLoggingEnabled = httpLoggingEnabled,
    )
}

/** Factory extension: create a [GeminiClient] from [KmpAi]. */
fun KmpAi.gemini(block: GeminiClientConfigBuilder.() -> Unit): GeminiClient =
    GeminiClient(GeminiClientConfigBuilder().apply(block).build())
