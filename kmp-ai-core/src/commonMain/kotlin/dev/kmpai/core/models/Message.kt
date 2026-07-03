package dev.kmpai.core.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The role of the message sender. */
@Serializable
enum class Role {
    @SerialName("system") system,
    @SerialName("user") user,
    @SerialName("assistant") assistant,
    @SerialName("tool") tool,
}

/**
 * A single message in a conversation.
 *
 * @param role Who sent this message.
 * @param content The message content (text or multimodal parts).
 * @param name Optional name for the participant (used by some providers).
 * @param toolCallId For tool result messages, the ID of the tool call being answered.
 * @param toolCalls Tool calls requested by the assistant (present on assistant messages).
 */
@Serializable
data class Message(
    val role: Role,
    val content: MessageContent,
    val name: String? = null,
    @SerialName("tool_call_id") val toolCallId: String? = null,
    @SerialName("tool_calls") val toolCalls: List<ToolCall>? = null,
)

/** Content of a message — either plain text or a list of typed parts. */
@Serializable
sealed class MessageContent {
    /** Simple text-only message content. */
    @Serializable
    data class Text(val text: String) : MessageContent()

    /** Multimodal content with text, image, and/or audio parts. */
    @Serializable
    data class Parts(val parts: List<ContentPart>) : MessageContent()
}

/** A single content part within a multipart message. */
@Serializable
sealed class ContentPart {
    @Serializable
    data class TextPart(val text: String) : ContentPart()

    @Serializable
    data class ImagePart(val imageUrl: ImageUrl) : ContentPart()

    @Serializable
    data class AudioPart(
        val audioUrl: String,
        val format: String = "mp3",
    ) : ContentPart()
}

/** Reference to an image by URL or base64-encoded data URI. */
@Serializable
data class ImageUrl(
    val url: String,
    /** Level of detail for image analysis: "auto", "low", or "high". */
    val detail: String? = "auto",
)

/** Convenience extension to extract text from MessageContent. */
fun MessageContent.asText(): String? = when (this) {
    is MessageContent.Text -> text
    is MessageContent.Parts -> parts.filterIsInstance<ContentPart.TextPart>()
        .joinToString("\n") { it.text }
        .takeIf { it.isNotEmpty() }
}
