package dev.kmpai.core.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request to generate embedding vectors for one or more inputs.
 *
 * @param model The embedding model ID.
 * @param input List of strings to embed. Most providers accept 1–2048 inputs per request.
 * @param dimensions Optional output dimensionality (supported by some models).
 * @param encodingFormat Output format: "float" (default) or "base64".
 * @param user Optional end-user identifier.
 */
@Serializable
data class EmbeddingRequest(
    val model: String,
    val input: List<String>,
    val dimensions: Int? = null,
    @SerialName("encoding_format") val encodingFormat: String = "float",
    val user: String? = null,
)
