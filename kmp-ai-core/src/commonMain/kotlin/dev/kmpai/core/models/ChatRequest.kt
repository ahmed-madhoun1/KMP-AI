package dev.kmpai.core.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A chat completion request.
 *
 * @param model The model ID to use (e.g. "gpt-4o", "claude-3-5-sonnet-20241022").
 * @param messages The conversation history.
 * @param temperature Sampling temperature (0.0–2.0). Higher = more random.
 * @param maxTokens Maximum number of tokens to generate.
 * @param topP Nucleus sampling probability (0.0–1.0).
 * @param frequencyPenalty Penalize repeated tokens by frequency (-2.0–2.0).
 * @param presencePenalty Penalize tokens already in the prompt (-2.0–2.0).
 * @param stop Sequences at which to stop generation.
 * @param stream Whether to stream the response. Set automatically by [AiClient.chatStream].
 * @param tools Tools (functions) the model can call.
 * @param toolChoice How the model should decide to use tools.
 * @param responseFormat Force a specific output format.
 * @param seed Deterministic seed for reproducible outputs (provider-dependent).
 * @param user An end-user identifier for abuse monitoring.
 * @param extraParams Provider-specific parameters not covered by the common API.
 */
@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double? = null,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    @SerialName("top_p") val topP: Double? = null,
    @SerialName("frequency_penalty") val frequencyPenalty: Double? = null,
    @SerialName("presence_penalty") val presencePenalty: Double? = null,
    val stop: List<String>? = null,
    val stream: Boolean = false,
    val tools: List<Tool>? = null,
    @SerialName("tool_choice") val toolChoice: ToolChoice? = null,
    @SerialName("response_format") val responseFormat: ResponseFormat? = null,
    val seed: Int? = null,
    val user: String? = null,
    val extraParams: Map<String, String> = emptyMap(),
)

/** Controls the response format (e.g. JSON mode). */
@Serializable
sealed class ResponseFormat {
    @Serializable
    object Text : ResponseFormat()

    @Serializable
    object JsonObject : ResponseFormat()

    @Serializable
    data class JsonSchema(
        val name: String,
        val schema: kotlinx.serialization.json.JsonObject,
        val strict: Boolean = true,
    ) : ResponseFormat()
}
