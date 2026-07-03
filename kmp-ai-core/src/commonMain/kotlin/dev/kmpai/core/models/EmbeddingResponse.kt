package dev.kmpai.core.models

import kotlinx.serialization.Serializable

/** Response containing embedding vectors for the requested inputs. */
@Serializable
data class EmbeddingResponse(
    val model: String,
    /** One embedding vector per input string, in the same order as the request. */
    val embeddings: List<List<Double>>,
    val usage: Usage,
)
