package dev.kmpai.core

import dev.kmpai.core.models.AiModel
import dev.kmpai.core.models.AiProvider
import dev.kmpai.core.models.ChatRequest
import dev.kmpai.core.models.ChatResponse
import dev.kmpai.core.models.EmbeddingRequest
import dev.kmpai.core.models.EmbeddingResponse
import dev.kmpai.core.models.StreamChunk
import kotlinx.coroutines.flow.Flow

/**
 * The unified AI client interface implemented by all providers.
 *
 * Example usage:
 * ```kotlin
 * val client = KmpAi.openAi { apiKey = "sk-..." }
 *
 * val response = client.chat(
 *     ChatRequest(
 *         model = "gpt-4o",
 *         messages = listOf(Message(Role.user, MessageContent.Text("Hello!")))
 *     )
 * )
 * println(response.choices.first().message.content)
 * client.close()
 * ```
 */
interface AiClient {

    /** The provider this client connects to. */
    val provider: AiProvider

    /**
     * Send a chat completion request and receive a single, complete response.
     *
     * @param request The chat request containing model, messages, and options.
     * @return A [ChatResponse] with the model's reply.
     * @throws dev.kmpai.core.error.AiException on API or network errors.
     */
    suspend fun chat(request: ChatRequest): ChatResponse

    /**
     * Send a chat completion request and receive a streaming response.
     *
     * Emits [StreamChunk] objects as tokens arrive from the provider.
     * Collect with [dev.kmpai.streaming.textDeltas] for a simple text flow.
     *
     * @param request The chat request. The `stream` field is set to `true` automatically.
     * @return A cold [Flow] of [StreamChunk] objects.
     */
    fun chatStream(request: ChatRequest): Flow<StreamChunk>

    /**
     * Generate an embedding vector for the given input text(s).
     *
     * @param request The embedding request.
     * @return An [EmbeddingResponse] containing the vector(s).
     * @throws dev.kmpai.core.error.AiException.UnsupportedOperationException if provider
     *   does not support embeddings.
     */
    suspend fun embed(request: EmbeddingRequest): EmbeddingResponse

    /**
     * List all models available from this provider.
     */
    suspend fun listModels(): List<AiModel>

    /**
     * Close the underlying HTTP client and release resources.
     * Should be called when the client is no longer needed.
     */
    fun close()
}
