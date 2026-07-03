package dev.kmpai.multimodal

import dev.kmpai.core.models.ContentPart
import dev.kmpai.core.models.ImageUrl
import dev.kmpai.core.models.Message
import dev.kmpai.core.models.MessageContent
import dev.kmpai.core.models.Role

// ── Convenience extensions for building multimodal messages ──────────────────

/**
 * Create a user message containing an image URL and optional text prompt.
 *
 * ```kotlin
 * val msg = imageMessage("https://example.com/photo.jpg", "Describe this image")
 * ```
 */
fun imageMessage(
    url: String,
    prompt: String = "Describe this image.",
    detail: String = "auto",
): Message {
    return Message(
        role = Role.user,
        content = MessageContent.Parts(
            listOf(
                ContentPart.TextPart(prompt),
                ContentPart.ImagePart(ImageUrl(url, detail)),
            )
        )
    )
}

/**
 * Create a user message from a base64-encoded image.
 *
 * @param base64 The base64-encoded image data (without the data URI prefix).
 * @param mimeType MIME type, e.g. "image/jpeg", "image/png", "image/webp".
 * @param prompt The text prompt accompanying the image.
 * @param detail Vision detail level: "auto", "low", or "high".
 */
fun imageBase64Message(
    base64: String,
    mimeType: String = "image/jpeg",
    prompt: String = "Describe this image.",
    detail: String = "auto",
): Message {
    val dataUri = "data:$mimeType;base64,$base64"
    return Message(
        role = Role.user,
        content = MessageContent.Parts(
            listOf(
                ContentPart.TextPart(prompt),
                ContentPart.ImagePart(ImageUrl(dataUri, detail)),
            )
        )
    )
}

/**
 * Create a user message containing multiple images.
 *
 * @param imageUrls List of image URLs to include.
 * @param prompt The text prompt to accompany the images.
 */
fun multiImageMessage(
    imageUrls: List<String>,
    prompt: String,
    detail: String = "auto",
): Message {
    val parts = buildList {
        add(ContentPart.TextPart(prompt))
        imageUrls.forEach { url ->
            add(ContentPart.ImagePart(ImageUrl(url, detail)))
        }
    }
    return Message(role = Role.user, content = MessageContent.Parts(parts))
}

/**
 * Encode a [ByteArray] to a base64 string suitable for use in data URIs.
 * Platform-specific implementation is expected — this is the expect declaration.
 */
expect fun ByteArray.toBase64(): String

/**
 * Create a user message from raw image bytes.
 *
 * @param bytes Raw image bytes.
 * @param mimeType MIME type, e.g. "image/jpeg".
 * @param prompt Accompanying text prompt.
 */
fun imageBytesMessage(
    bytes: ByteArray,
    mimeType: String = "image/jpeg",
    prompt: String = "Describe this image.",
    detail: String = "auto",
): Message = imageBase64Message(
    base64 = bytes.toBase64(),
    mimeType = mimeType,
    prompt = prompt,
    detail = detail,
)
