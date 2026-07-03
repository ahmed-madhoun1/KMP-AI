package dev.kmpai.gemini.internal.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// ── Request ───────────────────────────────────────────────────────────────────

@Serializable
internal data class GeminiGenerateRequest(
    val contents: List<GeminiContent>,
    @SerialName("systemInstruction") val systemInstruction: GeminiContent? = null,
    @SerialName("generationConfig") val generationConfig: GeminiGenerationConfig? = null,
    val tools: List<GeminiTool>? = null,
)

@Serializable
internal data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>,
)

@Serializable
internal data class GeminiPart(
    val text: String? = null,
    @SerialName("inlineData") val inlineData: GeminiInlineData? = null,
    @SerialName("fileData") val fileData: GeminiFileData? = null,
    @SerialName("functionCall") val functionCall: GeminiFunctionCall? = null,
    @SerialName("functionResponse") val functionResponse: GeminiFunctionResponse? = null,
)

@Serializable
internal data class GeminiInlineData(
    @SerialName("mimeType") val mimeType: String,
    val data: String, // base64
)

@Serializable
internal data class GeminiFileData(
    @SerialName("mimeType") val mimeType: String,
    @SerialName("fileUri") val fileUri: String,
)

@Serializable
internal data class GeminiGenerationConfig(
    val temperature: Double? = null,
    @SerialName("maxOutputTokens") val maxOutputTokens: Int? = null,
    @SerialName("topP") val topP: Double? = null,
    @SerialName("stopSequences") val stopSequences: List<String>? = null,
    @SerialName("candidateCount") val candidateCount: Int? = null,
)

@Serializable
internal data class GeminiTool(
    @SerialName("functionDeclarations") val functionDeclarations: List<GeminiFunctionDeclaration>,
)

@Serializable
internal data class GeminiFunctionDeclaration(
    val name: String,
    val description: String,
    val parameters: JsonObject? = null,
)

@Serializable
internal data class GeminiFunctionCall(
    val name: String,
    val args: JsonObject? = null,
)

@Serializable
internal data class GeminiFunctionResponse(
    val name: String,
    val response: JsonObject,
)

@Serializable
internal data class GeminiEmbedRequest(
    val model: String,
    val content: GeminiContent,
    @SerialName("taskType") val taskType: String? = null,
)

@Serializable
internal data class GeminiBatchEmbedRequest(
    val requests: List<GeminiEmbedRequest>,
)

// ── Response ──────────────────────────────────────────────────────────────────

@Serializable
internal data class GeminiGenerateResponse(
    val candidates: List<GeminiCandidate>? = null,
    @SerialName("usageMetadata") val usageMetadata: GeminiUsageMetadata? = null,
)

@Serializable
internal data class GeminiCandidate(
    val index: Int = 0,
    val content: GeminiContent? = null,
    @SerialName("finishReason") val finishReason: String? = null,
)

@Serializable
internal data class GeminiUsageMetadata(
    @SerialName("promptTokenCount") val promptTokenCount: Int = 0,
    @SerialName("candidatesTokenCount") val candidatesTokenCount: Int = 0,
    @SerialName("totalTokenCount") val totalTokenCount: Int = 0,
)

@Serializable
internal data class GeminiEmbedResponse(
    val embedding: GeminiEmbeddingValues,
)

@Serializable
internal data class GeminiBatchEmbedResponse(
    val embeddings: List<GeminiEmbeddingValues>,
)

@Serializable
internal data class GeminiEmbeddingValues(
    val values: List<Double>,
)

@Serializable
internal data class GeminiModelsResponse(
    val models: List<GeminiModelData>,
)

@Serializable
internal data class GeminiModelData(
    val name: String,
    @SerialName("displayName") val displayName: String = "",
    @SerialName("supportedGenerationMethods") val supportedGenerationMethods: List<String> = emptyList(),
    @SerialName("inputTokenLimit") val inputTokenLimit: Int? = null,
    @SerialName("outputTokenLimit") val outputTokenLimit: Int? = null,
)
