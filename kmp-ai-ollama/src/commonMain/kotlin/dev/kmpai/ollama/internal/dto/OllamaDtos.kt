package dev.kmpai.ollama.internal.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Request ───────────────────────────────────────────────────────────────────

@Serializable
internal data class OllamaChatRequest(
    val model: String,
    val messages: List<OllamaMessage>,
    val stream: Boolean = false,
    val options: OllamaOptions? = null,
    val tools: List<OllamaTool>? = null,
)

@Serializable
internal data class OllamaMessage(
    val role: String,
    val content: String,
    val images: List<String>? = null, // base64 encoded
)

@Serializable
internal data class OllamaOptions(
    val temperature: Double? = null,
    @SerialName("num_predict") val numPredict: Int? = null,
    @SerialName("top_p") val topP: Double? = null,
    val stop: List<String>? = null,
)

@Serializable
internal data class OllamaTool(
    val type: String = "function",
    val function: OllamaFunctionDef,
)

@Serializable
internal data class OllamaFunctionDef(
    val name: String,
    val description: String,
    val parameters: kotlinx.serialization.json.JsonObject,
)

@Serializable
internal data class OllamaEmbedRequest(
    val model: String,
    val input: List<String>,
)

// ── Response ──────────────────────────────────────────────────────────────────

@Serializable
internal data class OllamaChatResponse(
    val model: String,
    val message: OllamaMessage? = null,
    @SerialName("done_reason") val doneReason: String? = null,
    val done: Boolean = false,
    @SerialName("prompt_eval_count") val promptEvalCount: Int? = null,
    @SerialName("eval_count") val evalCount: Int? = null,
)

@Serializable
internal data class OllamaEmbedResponse(
    val model: String,
    val embeddings: List<List<Double>>,
)

@Serializable
internal data class OllamaTagsResponse(
    val models: List<OllamaModelInfo>,
)

@Serializable
internal data class OllamaModelInfo(
    val name: String,
    val model: String = "",
    val size: Long = 0L,
    @SerialName("parameter_size") val parameterSize: String? = null,
)
