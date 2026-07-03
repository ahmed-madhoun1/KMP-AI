package dev.kmpai.core.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** A tool (function) that the model can call. */
@Serializable
data class Tool(
    val type: String = "function",
    val function: FunctionDefinition,
)

/** Defines the name, description, and parameter schema for a tool. */
@Serializable
data class FunctionDefinition(
    val name: String,
    val description: String,
    /** JSON Schema object describing the function parameters. */
    val parameters: JsonObject,
    /** Whether to enforce strict JSON schema adherence (OpenAI-specific). */
    val strict: Boolean? = null,
)

/** Controls whether and how the model uses tools. */
@Serializable
sealed class ToolChoice {
    /** Let the model decide whether to call a tool. */
    @Serializable
    object Auto : ToolChoice()

    /** The model must not call any tools. */
    @Serializable
    object None : ToolChoice()

    /** The model must call at least one tool. */
    @Serializable
    object Required : ToolChoice()

    /** Force the model to call a specific tool by name. */
    @Serializable
    data class Specific(val name: String) : ToolChoice()
}

/** A tool call requested by the assistant. */
@Serializable
data class ToolCall(
    val id: String,
    val type: String = "function",
    val function: FunctionCall,
)

/** The function name and arguments for a tool call. */
@Serializable
data class FunctionCall(
    val name: String,
    /** JSON-encoded string of the function arguments. */
    val arguments: String,
)

/** A partial tool call delta during streaming. */
@Serializable
data class ToolCallDelta(
    val index: Int,
    val id: String? = null,
    val type: String? = null,
    val function: FunctionCallDelta? = null,
)

/** Streaming fragment of a function call. */
@Serializable
data class FunctionCallDelta(
    val name: String? = null,
    val arguments: String? = null,
)
