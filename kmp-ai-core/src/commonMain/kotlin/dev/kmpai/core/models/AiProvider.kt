package dev.kmpai.core.models

import kotlinx.serialization.Serializable

/**
 * Identifies an AI provider (e.g. OpenAI, Anthropic).
 */
@Serializable
data class AiProvider(
    val id: String,
    val name: String,
    val baseUrl: String,
)
