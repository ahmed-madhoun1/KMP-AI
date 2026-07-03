package dev.kmpai.core

import dev.kmpai.core.models.ContentPart
import dev.kmpai.core.models.ImageUrl
import dev.kmpai.core.models.Message
import dev.kmpai.core.models.MessageContent
import dev.kmpai.core.models.Role

/**
 * DSL builder for constructing multimodal [Message] objects.
 *
 * ```kotlin
 * val msg = message(Role.user) {
 *     text("Describe this image:")
 *     image("https://example.com/photo.jpg", detail = "high")
 * }
 * ```
 */
@DslMarker
annotation class MessageDsl

@MessageDsl
class MessageBuilder(private val role: Role) {
    private val parts = mutableListOf<ContentPart>()
    var name: String? = null

    /** Add a text part. */
    fun text(text: String) {
        parts.add(ContentPart.TextPart(text))
    }

    /** Add an image by URL. */
    fun image(url: String, detail: String = "auto") {
        parts.add(ContentPart.ImagePart(ImageUrl(url, detail)))
    }

    /** Add an image as a base64-encoded data URI. */
    fun imageBase64(base64: String, mimeType: String = "image/jpeg", detail: String = "auto") {
        val dataUri = "data:$mimeType;base64,$base64"
        parts.add(ContentPart.ImagePart(ImageUrl(dataUri, detail)))
    }

    /** Add audio content by URL. */
    fun audio(url: String, format: String = "mp3") {
        parts.add(ContentPart.AudioPart(url, format))
    }

    fun build(): Message {
        val content = when {
            parts.size == 1 && parts[0] is ContentPart.TextPart ->
                MessageContent.Text((parts[0] as ContentPart.TextPart).text)
            else -> MessageContent.Parts(parts.toList())
        }
        return Message(role = role, content = content, name = name)
    }
}

/** Build a [Message] using the [MessageBuilder] DSL. */
fun message(role: Role, block: MessageBuilder.() -> Unit): Message =
    MessageBuilder(role).apply(block).build()

/** Build a conversation (list of messages) using a simple DSL. */
fun conversation(block: ConversationBuilder.() -> Unit): List<Message> =
    ConversationBuilder().apply(block).messages

@MessageDsl
class ConversationBuilder {
    val messages = mutableListOf<Message>()

    fun system(text: String) = messages.add(Message(Role.system, MessageContent.Text(text)))
    fun user(text: String) = messages.add(Message(Role.user, MessageContent.Text(text)))
    fun assistant(text: String) = messages.add(Message(Role.assistant, MessageContent.Text(text)))

    fun user(block: MessageBuilder.() -> Unit) = messages.add(message(Role.user, block))
    fun assistant(block: MessageBuilder.() -> Unit) = messages.add(message(Role.assistant, block))
}
