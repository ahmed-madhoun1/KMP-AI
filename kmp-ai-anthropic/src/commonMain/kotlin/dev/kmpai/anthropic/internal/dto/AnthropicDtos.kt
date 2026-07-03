package dev.kmpai.anthropic.internal.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

// ── Request ───────────────────────────────────────────────────────────────────

@Serializable
internal data class AnthropicMessageRequest(
    val model: String,
    val messages: List<AnthropicMessage>,
    @SerialName("max_tokens") val maxTokens: Int = 1024,
    val system: String? = null,
    val temperature: Double? = null,
    @SerialName("top_p") val topP: Double? = null,
    val stop: List<String>? = null,
    val stream: Boolean? = null,
    val tools: List<AnthropicTool>? = null,
    @SerialName("tool_choice") val toolChoice: JsonObject? = null,
)

@Serializable
internal data class AnthropicMessage(
    val role: String,
    val content: JsonElement,
)

@Serializable
internal data class AnthropicTool(
    val name: String,
    val description: String,
    @SerialName("input_schema") val inputSchema: JsonObject,
)

// ── Response ──────────────────────────────────────────────────────────────────

@Serializable
internal data class AnthropicMessageResponse(
    val id: String,
    val model: String,
    val type: String,
    val role: String,
    val content: List<AnthropicContent>,
    @SerialName("stop_reason") val stopReason: String? = null,
    val usage: AnthropicUsage? = null,
)

@Serializable
internal data class AnthropicContent(
    val type: String,
    val text: String? = null,
    val id: String? = null,
    val name: String? = null,
    val input: JsonObject? = null,
)

@Serializable
internal data class AnthropicUsage(
    @SerialName("input_tokens") val inputTokens: Int = 0,
    @SerialName("output_tokens") val outputTokens: Int = 0,
)

// ── Streaming ─────────────────────────────────────────────────────────────────

@Serializable
internal data class AnthropicStreamEvent(
    val type: String,
    val index: Int? = null,
    val delta: AnthropicDelta? = null,
    val message: AnthropicMessageResponse? = null,
    val usage: AnthropicUsage? = null,
)

@Serializable
internal data class AnthropicDelta(
    val type: String? = null,
    val text: String? = null,
    @SerialName("stop_reason") val stopReason: String? = null,
)

@Serializable
internal data class AnthropicErrorResponse(
    val type: String,
    val error: AnthropicError,
)

@Serializable
internal data class AnthropicError(
    val type: String,
    val message: String,
)
