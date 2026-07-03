package dev.kmpai.core.models

import kotlinx.serialization.Serializable

/**
 * Metadata about a model available from an AI provider.
 */
@Serializable
data class AiModel(
    val id: String,
    val provider: AiProvider,
    val contextWindow: Int? = null,
    val maxOutputTokens: Int? = null,
    val supportsStreaming: Boolean = true,
    val supportsTools: Boolean = false,
    val supportsVision: Boolean = false,
    val supportsEmbeddings: Boolean = false,
)
