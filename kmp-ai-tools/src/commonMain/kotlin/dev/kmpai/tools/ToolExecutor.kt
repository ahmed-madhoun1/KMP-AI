package dev.kmpai.tools

import dev.kmpai.core.AiClient
import dev.kmpai.core.models.ChatRequest
import dev.kmpai.core.models.ChatResponse
import dev.kmpai.core.models.FinishReason
import dev.kmpai.core.models.Message
import dev.kmpai.core.models.MessageContent
import dev.kmpai.core.models.Role
import dev.kmpai.core.models.ToolCall
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Parses tool call arguments and provides typed accessors.
 */
class ToolArguments(private val json: JsonObject) {

    /** Get a required string argument. */
    fun getString(key: String): String =
        json[key]?.jsonPrimitive?.content
            ?: error("Required argument '$key' not found in tool call")

    /** Get an optional string argument. */
    fun getStringOrNull(key: String): String? =
        json[key]?.jsonPrimitive?.content

    /** Get a required boolean argument. */
    fun getBoolean(key: String): Boolean =
        json[key]?.jsonPrimitive?.content?.toBooleanStrict()
            ?: error("Required argument '$key' not found in tool call")

    /** Get an optional boolean argument. */
    fun getBooleanOrNull(key: String): Boolean? =
        json[key]?.jsonPrimitive?.content?.toBooleanStrictOrNull()

    /** Get a required integer argument. */
    fun getInt(key: String): Int =
        json[key]?.jsonPrimitive?.content?.toInt()
            ?: error("Required argument '$key' not found in tool call")

    /** Get an optional integer argument. */
    fun getIntOrNull(key: String): Int? =
        json[key]?.jsonPrimitive?.content?.toIntOrNull()

    /** Get a required double argument. */
    fun getDouble(key: String): Double =
        json[key]?.jsonPrimitive?.content?.toDouble()
            ?: error("Required argument '$key' not found in tool call")

    /** Check if an argument exists. */
    fun has(key: String): Boolean = json.containsKey(key)

    /** Get the raw [JsonObject]. */
    fun raw(): JsonObject = json
}

/**
 * Executes tool calls by dispatching to registered handler functions.
 *
 * ```kotlin
 * val executor = ToolExecutor {
 *     handle("get_weather") { args ->
 *         val city = args.getString("city")
 *         """{"temperature": 22, "condition": "sunny"}"""
 *     }
 *     handle("search_web") { args ->
 *         val query = args.getString("query")
 *         // call your search API...
 *         """{"results": []}"""
 *     }
 * }
 * ```
 */
class ToolExecutor private constructor(
    private val handlers: Map<String, suspend (ToolArguments) -> String>,
) {
    /** Execute a tool call and return the JSON result string. */
    suspend fun execute(toolCall: ToolCall): String {
        val handler = handlers[toolCall.function.name]
            ?: return """{"error": "Unknown tool: ${toolCall.function.name}"}"""
        val argsJson = try {
            Json.decodeFromString<JsonObject>(toolCall.function.arguments)
        } catch (_: Exception) {
            JsonObject(emptyMap())
        }
        return try {
            handler(ToolArguments(argsJson))
        } catch (e: Exception) {
            """{"error": "${e.message}"}"""
        }
    }

    class Builder {
        private val handlers = mutableMapOf<String, suspend (ToolArguments) -> String>()

        fun handle(toolName: String, handler: suspend (ToolArguments) -> String) {
            handlers[toolName] = handler
        }

        fun build() = ToolExecutor(handlers.toMap())
    }

    companion object {
        operator fun invoke(block: Builder.() -> Unit): ToolExecutor =
            Builder().apply(block).build()
    }
}

/**
 * Run an agentic tool-use loop: send the request, detect tool calls,
 * execute them, and continue until the model stops or [maxRounds] is reached.
 *
 * ```kotlin
 * val response = client.chatWithTools(
 *     request = ChatRequest(model = "gpt-4o", messages = messages, tools = tools),
 *     executor = executor,
 *     maxRounds = 5,
 * )
 * ```
 */
suspend fun AiClient.chatWithTools(
    request: ChatRequest,
    executor: ToolExecutor,
    maxRounds: Int = 10,
): ChatResponse {
    val messages = request.messages.toMutableList()
    var currentRequest = request.copy(messages = messages)
    var rounds = 0

    while (rounds < maxRounds) {
        val response = chat(currentRequest)
        val choice = response.choices.firstOrNull() ?: return response

        // No tool calls — model is done
        if (choice.finishReason != FinishReason.tool_calls || choice.message.toolCalls.isNullOrEmpty()) {
            return response
        }

        // Add assistant message with tool calls to history
        messages.add(choice.message)

        // Execute each tool call and add results to history
        for (toolCall in choice.message.toolCalls) {
            val result = executor.execute(toolCall)
            messages.add(
                Message(
                    role = Role.tool,
                    content = MessageContent.Text(result),
                    toolCallId = toolCall.id,
                )
            )
        }

        currentRequest = currentRequest.copy(messages = messages.toList())
        rounds++
    }

    // Max rounds reached — return last response
    return chat(currentRequest)
}
