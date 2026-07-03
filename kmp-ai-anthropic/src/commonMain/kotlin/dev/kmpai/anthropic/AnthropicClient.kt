package dev.kmpai.anthropic

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
import dev.kmpai.anthropic.internal.dto.AnthropicContent
import dev.kmpai.anthropic.internal.dto.AnthropicDelta
import dev.kmpai.anthropic.internal.dto.AnthropicErrorResponse
import dev.kmpai.anthropic.internal.dto.AnthropicMessage
import dev.kmpai.anthropic.internal.dto.AnthropicMessageRequest
import dev.kmpai.anthropic.internal.dto.AnthropicMessageResponse
import dev.kmpai.anthropic.internal.dto.AnthropicStreamEvent
import dev.kmpai.anthropic.internal.dto.AnthropicTool
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonPrimitive

/**
 * Anthropic provider implementation of [AiClient].
 *
 * Create via the factory extension on [KmpAi]:
 * ```kotlin
 * val client = KmpAi.anthropic { apiKey = "sk-ant-..." }
 * ```
 */
class AnthropicClient internal constructor(
    private val config: AiClientConfig,
    httpClientEngine: HttpClientEngine? = null,
) : AiClient {

    companion object {
        private const val ANTHROPIC_VERSION = "2023-06-01"
        private const val ANTHROPIC_BETA = "tools-2024-04-04"
    }

    override val provider = AiProvider(
        id = "anthropic",
        name = "Anthropic",
        baseUrl = config.baseUrl ?: "https://api.anthropic.com/v1",
    )

    private val httpClient = buildHttpClient(config, httpClientEngine).config {
        io.ktor.client.plugins.defaultRequest {
            header("x-api-key", config.apiKey)
            header("anthropic-version", ANTHROPIC_VERSION)
        }
    }

    override suspend fun chat(request: ChatRequest): ChatResponse {
        val dto = request.toAnthropicRequest()
        return try {
            httpClient.post("${provider.baseUrl}/messages") {
                setBody(dto)
            }.body<AnthropicMessageResponse>().toCoreResponse(provider)
        } catch (e: ClientRequestException) {
            throw e.toAiException(provider.name)
        } catch (e: ServerResponseException) {
            throw AiException.ProviderException(e.message ?: "Server error", provider.name, e.response.status.value)
        } catch (e: Exception) {
            throw AiException.NetworkException(e.message ?: "Network error", e)
        }
    }

    override fun chatStream(request: ChatRequest): Flow<StreamChunk> = flow {
        val dto = request.toAnthropicRequest().copy(stream = true)
        try {
            httpClient.preparePost("${provider.baseUrl}/messages") {
                setBody(dto)
            }.execute { response ->
                val channel = response.bodyAsChannel()
                var streamId = ""
                while (!channel.isClosedForRead) {
                    val line = channel.readUTF8Line() ?: break
                    if (line.startsWith("data: ")) {
                        val data = line.removePrefix("data: ").trim()
                        if (data.isNotEmpty()) {
                            try {
                                val event = sharedJson.decodeFromString<AnthropicStreamEvent>(data)
                                when (event.type) {
                                    "message_start" -> streamId = event.message?.id ?: ""
                                    "content_block_delta" -> {
                                        val text = event.delta?.text
                                        if (!text.isNullOrEmpty()) {
                                            emit(StreamChunk(
                                                id = streamId,
                                                delta = MessageDelta(content = text),
                                                finishReason = null,
                                            ))
                                        }
                                    }
                                    "message_delta" -> {
                                        val stopReason = event.delta?.stopReason
                                        val usage = event.usage?.let {
                                            Usage(it.inputTokens, it.outputTokens, it.inputTokens + it.outputTokens)
                                        }
                                        if (stopReason != null) {
                                            emit(StreamChunk(
                                                id = streamId,
                                                delta = MessageDelta(),
                                                finishReason = stopReason.toFinishReason(),
                                                usage = usage,
                                            ))
                                        }
                                    }
                                }
                            } catch (_: Exception) { /* skip */ }
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
        throw AiException.UnsupportedOperationException(
            "Anthropic does not support embeddings. Use OpenAI or Gemini instead.",
            provider.name,
        )
    }

    override suspend fun listModels(): List<AiModel> = AnthropicModels.run {
        listOf(
            CLAUDE_3_5_SONNET, CLAUDE_3_5_HAIKU,
            CLAUDE_3_OPUS, CLAUDE_3_SONNET, CLAUDE_3_HAIKU,
        ).map { id ->
            AiModel(
                id = id,
                provider = provider,
                supportsStreaming = true,
                supportsTools = true,
                supportsVision = true,
            )
        }
    }

    override fun close() = httpClient.close()

    private fun ChatRequest.toAnthropicRequest(): AnthropicMessageRequest {
        val systemMessage = messages.firstOrNull { it.role == Role.system }?.content?.asText()
        val conversation = messages.filter { it.role != Role.system }
        return AnthropicMessageRequest(
            model = model,
            system = systemMessage,
            messages = conversation.map { msg ->
                AnthropicMessage(
                    role = msg.role.name,
                    content = JsonPrimitive(msg.content.asText() ?: ""),
                )
            },
            maxTokens = maxTokens ?: 1024,
            temperature = temperature,
            topP = topP,
            stop = stop,
            tools = tools?.map { t ->
                AnthropicTool(
                    name = t.function.name,
                    description = t.function.description,
                    inputSchema = t.function.parameters,
                )
            },
        )
    }
}

private fun AnthropicMessageResponse.toCoreResponse(provider: AiProvider): ChatResponse {
    val text = content.filterIsInstance<AnthropicContent>()
        .firstOrNull { it.type == "text" }?.text ?: ""
    return ChatResponse(
        id = id,
        model = model,
        provider = provider,
        choices = listOf(
            Choice(
                index = 0,
                message = Message(Role.assistant, MessageContent.Text(text)),
                finishReason = stopReason?.toFinishReason(),
            )
        ),
        usage = usage?.let {
            Usage(it.inputTokens, it.outputTokens, it.inputTokens + it.outputTokens)
        },
    )
}

private fun String.toFinishReason(): FinishReason = when (this) {
    "end_turn" -> FinishReason.stop
    "max_tokens" -> FinishReason.length
    "tool_use" -> FinishReason.tool_calls
    else -> FinishReason.stop
}

private suspend fun ClientRequestException.toAiException(providerName: String): AiException {
    val body = try { response.bodyAsText() } catch (_: Exception) { message ?: "" }
    val errorMsg = try {
        sharedJson.decodeFromString<AnthropicErrorResponse>(body).error.message
    } catch (_: Exception) { body }
    return when (response.status) {
        HttpStatusCode.Unauthorized -> AiException.AuthenticationException(errorMsg, providerName)
        HttpStatusCode.TooManyRequests -> AiException.RateLimitException(errorMsg, providerName)
        else -> AiException.InvalidRequestException(errorMsg, providerName, response.status.value)
    }
}

/** DSL config builder for Anthropic. */
class AnthropicClientConfigBuilder {
    var apiKey: String = ""
    var baseUrl: String? = null
    var timeoutMs: Long = 60_000L
    var maxRetries: Int = 3
    var httpLoggingEnabled: Boolean = false

    fun build() = AiClientConfig(
        apiKey = apiKey,
        baseUrl = baseUrl,
        timeoutMs = timeoutMs,
        maxRetries = maxRetries,
        httpLoggingEnabled = httpLoggingEnabled,
    )
}

/** Factory extension: create an [AnthropicClient] from [KmpAi]. */
fun KmpAi.anthropic(block: AnthropicClientConfigBuilder.() -> Unit): AnthropicClient =
    AnthropicClient(AnthropicClientConfigBuilder().apply(block).build())
