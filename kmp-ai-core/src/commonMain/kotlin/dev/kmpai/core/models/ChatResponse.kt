package dev.kmpai.core.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A complete chat completion response. */
@Serializable
data class ChatResponse(
    val id: String,
    val model: String,
    val provider: AiProvider,
    val choices: List<Choice>,
    val usage: Usage? = null,
    val created: Long = 0L,
)

/** A single completion choice in a [ChatResponse]. */
@Serializable
data class Choice(
    val index: Int,
    val message: Message,
    @SerialName("finish_reason") val finishReason: FinishReason? = null,
)

/** Reason why the model stopped generating. */
@Serializable
enum class FinishReason {
    @SerialName("stop") stop,
    @SerialName("length") length,
    @SerialName("tool_calls") tool_calls,
    @SerialName("content_filter") content_filter,
    @SerialName("error") error,
}

/** Token usage statistics. */
@Serializable
data class Usage(
    @SerialName("prompt_tokens") val promptTokens: Int = 0,
    @SerialName("completion_tokens") val completionTokens: Int = 0,
    @SerialName("total_tokens") val totalTokens: Int = 0,
)

// ── Streaming ─────────────────────────────────────────────────────────────────

/** A single streaming chunk emitted by [AiClient.chatStream]. */
@Serializable
data class StreamChunk(
    val id: String,
    val delta: MessageDelta,
    @SerialName("finish_reason") val finishReason: FinishReason? = null,
    val usage: Usage? = null,
)

/** The incremental content delta within a [StreamChunk]. */
@Serializable
data class MessageDelta(
    val role: Role? = null,
    val content: String? = null,
    @SerialName("tool_calls") val toolCalls: List<ToolCallDelta>? = null,
)
