package dev.kmpai.openai.internal.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

// ── Request DTOs ──────────────────────────────────────────────────────────────

@Serializable
internal data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val temperature: Double? = null,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    @SerialName("top_p") val topP: Double? = null,
    @SerialName("frequency_penalty") val frequencyPenalty: Double? = null,
    @SerialName("presence_penalty") val presencePenalty: Double? = null,
    val stop: List<String>? = null,
    val stream: Boolean? = null,
    @SerialName("stream_options") val streamOptions: StreamOptions? = null,
    val tools: List<OpenAiTool>? = null,
    @SerialName("tool_choice") val toolChoice: JsonElement? = null,
    @SerialName("response_format") val responseFormat: JsonObject? = null,
    val seed: Int? = null,
    val user: String? = null,
)

@Serializable
internal data class StreamOptions(
    @SerialName("include_usage") val includeUsage: Boolean = true,
)

@Serializable
internal data class OpenAiMessage(
    val role: String,
    val content: JsonElement? = null,
    val name: String? = null,
    @SerialName("tool_call_id") val toolCallId: String? = null,
    @SerialName("tool_calls") val toolCalls: List<OpenAiToolCall>? = null,
)

@Serializable
internal data class OpenAiTool(
    val type: String = "function",
    val function: OpenAiFunctionDef,
)

@Serializable
internal data class OpenAiFunctionDef(
    val name: String,
    val description: String,
    val parameters: JsonObject,
    val strict: Boolean? = null,
)

@Serializable
internal data class OpenAiToolCall(
    val id: String,
    val type: String = "function",
    val function: OpenAiFunctionCall,
)

@Serializable
internal data class OpenAiFunctionCall(
    val name: String,
    val arguments: String,
)

@Serializable
internal data class OpenAiEmbeddingRequest(
    val model: String,
    val input: List<String>,
    val dimensions: Int? = null,
    @SerialName("encoding_format") val encodingFormat: String = "float",
    val user: String? = null,
)

// ── Response DTOs ─────────────────────────────────────────────────────────────

@Serializable
internal data class OpenAiChatResponse(
    val id: String,
    val model: String,
    val choices: List<OpenAiChoice>,
    val usage: OpenAiUsage? = null,
    val created: Long = 0L,
)

@Serializable
internal data class OpenAiChoice(
    val index: Int,
    val message: OpenAiMessage,
    @SerialName("finish_reason") val finishReason: String? = null,
)

@Serializable
internal data class OpenAiUsage(
    @SerialName("prompt_tokens") val promptTokens: Int = 0,
    @SerialName("completion_tokens") val completionTokens: Int = 0,
    @SerialName("total_tokens") val totalTokens: Int = 0,
)

@Serializable
internal data class OpenAiStreamResponse(
    val id: String,
    val choices: List<OpenAiStreamChoice>,
    val usage: OpenAiUsage? = null,
)

@Serializable
internal data class OpenAiStreamChoice(
    val index: Int,
    val delta: OpenAiDelta,
    @SerialName("finish_reason") val finishReason: String? = null,
)

@Serializable
internal data class OpenAiDelta(
    val role: String? = null,
    val content: String? = null,
    @SerialName("tool_calls") val toolCalls: List<OpenAiToolCallDelta>? = null,
)

@Serializable
internal data class OpenAiToolCallDelta(
    val index: Int,
    val id: String? = null,
    val type: String? = null,
    val function: OpenAiFunctionCallDelta? = null,
)

@Serializable
internal data class OpenAiFunctionCallDelta(
    val name: String? = null,
    val arguments: String? = null,
)

@Serializable
internal data class OpenAiEmbeddingResponse(
    val model: String,
    val data: List<OpenAiEmbeddingData>,
    val usage: OpenAiUsage,
)

@Serializable
internal data class OpenAiEmbeddingData(
    val index: Int,
    val embedding: List<Double>,
)

@Serializable
internal data class OpenAiModelsResponse(
    val data: List<OpenAiModelData>,
)

@Serializable
internal data class OpenAiModelData(
    val id: String,
    val created: Long = 0L,
    @SerialName("owned_by") val ownedBy: String = "",
)

@Serializable
internal data class OpenAiErrorResponse(
    val error: OpenAiError,
)

@Serializable
internal data class OpenAiError(
    val message: String,
    val type: String? = null,
    val code: String? = null,
)
