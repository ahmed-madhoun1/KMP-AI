package dev.kmpai.ollama

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
import dev.kmpai.core.models.FunctionCall
import dev.kmpai.core.models.Message
import dev.kmpai.core.models.MessageContent
import dev.kmpai.core.models.MessageDelta
import dev.kmpai.core.models.Role
import dev.kmpai.core.models.StreamChunk
import dev.kmpai.core.models.ToolCall
import dev.kmpai.core.models.Usage
import dev.kmpai.core.models.asText
import dev.kmpai.ollama.internal.dto.OllamaChatRequest
import dev.kmpai.ollama.internal.dto.OllamaChatResponse
import dev.kmpai.ollama.internal.dto.OllamaEmbedRequest
import dev.kmpai.ollama.internal.dto.OllamaEmbedResponse
import dev.kmpai.ollama.internal.dto.OllamaFunctionDef
import dev.kmpai.ollama.internal.dto.OllamaMessage
import dev.kmpai.ollama.internal.dto.OllamaModelInfo
import dev.kmpai.ollama.internal.dto.OllamaOptions
import dev.kmpai.ollama.internal.dto.OllamaTool
import dev.kmpai.ollama.internal.dto.OllamaTagsResponse
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Ollama local model provider implementation of [AiClient].
 *
 * Connects to a locally running Ollama instance. No API key required.
 *
 * Create via the factory extension on [KmpAi]:
 * ```kotlin
 * val client = KmpAi.ollama {
 *     baseUrl = "http://localhost:11434"
 * }
 * ```
 */
class OllamaClient internal constructor(
    private val config: AiClientConfig,
    httpClientEngine: HttpClientEngine? = null,
) : AiClient {

    override val provider = AiProvider(
        id = "ollama",
        name = "Ollama",
        baseUrl = config.baseUrl ?: "http://localhost:11434",
    )

    private val httpClient = buildHttpClient(config, httpClientEngine)

    override suspend fun chat(request: ChatRequest): ChatResponse {
        val dto = request.toOllamaRequest(stream = false)
        return try {
            httpClient.post("${provider.baseUrl}/api/chat") {
                setBody(dto)
            }.body<OllamaChatResponse>().toCoreResponse(provider)
        } catch (e: Exception) {
            throw AiException.NetworkException(e.message ?: "Ollama error", e)
        }
    }

    override fun chatStream(request: ChatRequest): Flow<StreamChunk> = flow {
        val dto = request.toOllamaRequest(stream = true)
        try {
            httpClient.preparePost("${provider.baseUrl}/api/chat") {
                setBody(dto)
            }.execute { response ->
                val channel = response.bodyAsChannel()
                var chunkId = 0
                while (!channel.isClosedForRead) {
                    val line = channel.readUTF8Line() ?: break
                    if (line.isNotBlank()) {
                        try {
                            val chunk = sharedJson.decodeFromString<OllamaChatResponse>(line)
                            val text = chunk.message?.content ?: ""
                            emit(StreamChunk(
                                id = "ollama-${chunkId++}",
                                delta = MessageDelta(content = text.ifEmpty { null }),
                                finishReason = if (chunk.done) FinishReason.stop else null,
                            ))
                            if (chunk.done) return@execute
                        } catch (_: Exception) { /* skip */ }
                    }
                }
            }
        } catch (e: Exception) {
            throw AiException.NetworkException(e.message ?: "Stream error", e)
        }
    }

    override suspend fun embed(request: EmbeddingRequest): EmbeddingResponse {
        return try {
            val response = httpClient.post("${provider.baseUrl}/api/embed") {
                setBody(OllamaEmbedRequest(model = request.model, input = request.input))
            }.body<OllamaEmbedResponse>()
            EmbeddingResponse(
                model = response.model,
                embeddings = response.embeddings,
                usage = Usage(0, 0, 0),
            )
        } catch (e: Exception) {
            throw AiException.NetworkException(e.message ?: "Embed error", e)
        }
    }

    override suspend fun listModels(): List<AiModel> {
        return try {
            httpClient.get("${provider.baseUrl}/api/tags")
                .body<OllamaTagsResponse>()
                .models.map { it.toAiModel(provider) }
        } catch (e: Exception) {
            throw AiException.NetworkException(e.message ?: "Network error", e)
        }
    }

    override fun close() = httpClient.close()

    private fun ChatRequest.toOllamaRequest(stream: Boolean): OllamaChatRequest {
        return OllamaChatRequest(
            model = model,
            messages = messages.map { msg ->
                OllamaMessage(
                    role = msg.role.name,
                    content = msg.content.asText() ?: "",
                )
            },
            stream = stream,
            options = OllamaOptions(
                temperature = temperature,
                numPredict = maxTokens,
                topP = topP,
                stop = stop,
            ),
            tools = tools?.map { t ->
                OllamaTool(
                    function = OllamaFunctionDef(
                        name = t.function.name,
                        description = t.function.description,
                        parameters = t.function.parameters,
                    )
                )
            },
        )
    }
}

private fun OllamaChatResponse.toCoreResponse(provider: AiProvider): ChatResponse = ChatResponse(
    id = "ollama-response",
    model = model,
    provider = provider,
    choices = listOf(
        Choice(
            index = 0,
            message = Message(
                role = Role.assistant,
                content = MessageContent.Text(message?.content ?: ""),
            ),
            finishReason = if (done) FinishReason.stop else null,
        )
    ),
    usage = if (promptEvalCount != null && evalCount != null) {
        Usage(promptEvalCount, evalCount, promptEvalCount + evalCount)
    } else null,
)

private fun OllamaModelInfo.toAiModel(provider: AiProvider): AiModel = AiModel(
    id = name,
    provider = provider,
    supportsStreaming = true,
    supportsTools = true,
    supportsEmbeddings = true,
)

/** DSL config builder for Ollama. */
class OllamaClientConfigBuilder {
    var baseUrl: String = "http://localhost:11434"
    var timeoutMs: Long = 120_000L // local models can be slow
    var httpLoggingEnabled: Boolean = false

    fun build() = AiClientConfig(
        apiKey = "",
        baseUrl = baseUrl,
        timeoutMs = timeoutMs,
        httpLoggingEnabled = httpLoggingEnabled,
    )
}

/** Factory extension: create an [OllamaClient] from [KmpAi]. */
fun KmpAi.ollama(block: OllamaClientConfigBuilder.() -> Unit = {}): OllamaClient =
    OllamaClient(OllamaClientConfigBuilder().apply(block).build())
