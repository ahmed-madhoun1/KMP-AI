package dev.kmpai.core

import dev.kmpai.core.models.ChatRequest
import dev.kmpai.core.models.ChatResponse
import dev.kmpai.core.models.Message
import dev.kmpai.core.models.MessageContent
import dev.kmpai.core.models.Role

/**
 * A stateful conversation session that maintains message history automatically.
 *
 * Usage:
 * ```kotlin
 * val session = client.session(model = "gpt-4o")
 * val r1 = session.send("What is Kotlin?")
 * val r2 = session.send("Can you give an example?") // remembers prior context
 * ```
 */
class ConversationSession(
    private val client: AiClient,
    private val model: String,
    systemPrompt: String? = null,
    private val temperature: Double? = null,
    private val maxTokens: Int? = null,
) {
    private val _history: MutableList<Message> = mutableListOf()

    /** The full conversation history so far. */
    val history: List<Message> get() = _history.toList()

    init {
        if (systemPrompt != null) {
            _history.add(Message(Role.system, MessageContent.Text(systemPrompt)))
        }
    }

    /**
     * Send a user message and receive the assistant's reply.
     * Both the user message and the response are appended to [history].
     */
    suspend fun send(text: String): ChatResponse {
        _history.add(Message(Role.user, MessageContent.Text(text)))
        val response = client.chat(
            ChatRequest(
                model = model,
                messages = _history.toList(),
                temperature = temperature,
                maxTokens = maxTokens,
            )
        )
        val assistantMessage = response.choices.first().message
        _history.add(assistantMessage)
        return response
    }

    /** Clear all messages (including the system prompt). */
    fun clear() = _history.clear()

    /** Clear all messages but keep the system prompt. */
    fun clearExceptSystem() {
        val system = _history.firstOrNull { it.role == Role.system }
        _history.clear()
        if (system != null) _history.add(system)
    }
}

/** Create a [ConversationSession] from this client. */
fun AiClient.session(
    model: String,
    systemPrompt: String? = null,
    temperature: Double? = null,
    maxTokens: Int? = null,
): ConversationSession = ConversationSession(
    client = this,
    model = model,
    systemPrompt = systemPrompt,
    temperature = temperature,
    maxTokens = maxTokens,
)
