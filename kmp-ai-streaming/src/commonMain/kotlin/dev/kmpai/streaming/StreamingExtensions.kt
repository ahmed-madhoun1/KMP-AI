package dev.kmpai.streaming

import dev.kmpai.core.models.FinishReason
import dev.kmpai.core.models.StreamChunk
import dev.kmpai.core.models.ToolCall
import dev.kmpai.core.models.FunctionCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.transform

/**
 * Collect all text deltas from a streaming response into a single string.
 *
 * ```kotlin
 * val fullText = client.chatStream(request).collectToText()
 * ```
 */
suspend fun Flow<StreamChunk>.collectToText(): String {
    val sb = StringBuilder()
    collect { chunk ->
        chunk.delta.content?.let { sb.append(it) }
    }
    return sb.toString()
}

/**
 * Emit only the text delta string from each chunk, filtering out chunks
 * with no text content (e.g. tool call chunks, finish chunks).
 *
 * ```kotlin
 * client.chatStream(request).textDeltas().collect { token -> print(token) }
 * ```
 */
fun Flow<StreamChunk>.textDeltas(): Flow<String> = transform { chunk ->
    chunk.delta.content?.let { if (it.isNotEmpty()) emit(it) }
}

/**
 * Accumulate text deltas into a running full-text string.
 * Useful for Compose [collectAsState] bindings.
 *
 * ```kotlin
 * val text by client.chatStream(request)
 *     .accumulatedText()
 *     .collectAsState(initial = "")
 * ```
 */
fun Flow<StreamChunk>.accumulatedText(): Flow<String> =
    textDeltas().runningFold("") { acc, token -> acc + token }

/**
 * Emit only chunks that signal the stream has ended, i.e. those with a non-null
 * [StreamChunk.finishReason].
 */
fun Flow<StreamChunk>.finishChunks(): Flow<StreamChunk> =
    filter { it.finishReason != null }

/**
 * Collect tool calls from a streaming response.
 * Tool call arguments may arrive in fragments — this assembles them.
 */
fun Flow<StreamChunk>.toolCallsFlow(): Flow<ToolCall> = transform { chunk ->
    chunk.delta.toolCalls?.forEach { delta ->
        if (delta.id != null && delta.function?.name != null) {
            emit(ToolCall(
                id = delta.id,
                function = FunctionCall(
                    name = delta.function.name ?: "",
                    arguments = delta.function.arguments ?: "{}",
                ),
            ))
        }
    }
}

/**
 * Emit [true] when the stream finishes with [FinishReason.stop],
 * [false] when it finishes for any other reason (length, tool call, etc.).
 */
fun Flow<StreamChunk>.completionStatus(): Flow<Boolean> =
    finishChunks().map { it.finishReason == FinishReason.stop }
