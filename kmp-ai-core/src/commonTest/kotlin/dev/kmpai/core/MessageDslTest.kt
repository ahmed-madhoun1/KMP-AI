package dev.kmpai.core

import dev.kmpai.core.models.ContentPart
import dev.kmpai.core.models.MessageContent
import dev.kmpai.core.models.Role
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MessageDslTest {

    @Test
    fun `single text part becomes Text content`() {
        val msg = message(Role.user) { text("Hello!") }
        assertIs<MessageContent.Text>(msg.content)
        assertEquals("Hello!", (msg.content as MessageContent.Text).text)
    }

    @Test
    fun `text + image becomes Parts content`() {
        val msg = message(Role.user) {
            text("Describe this:")
            image("https://example.com/photo.jpg")
        }
        assertIs<MessageContent.Parts>(msg.content)
        val parts = (msg.content as MessageContent.Parts).parts
        assertEquals(2, parts.size)
        assertIs<ContentPart.TextPart>(parts[0])
        assertIs<ContentPart.ImagePart>(parts[1])
    }

    @Test
    fun `conversation DSL builds message list in order`() {
        val messages = conversation {
            system("You are a helpful assistant.")
            user("Hello!")
            assistant("Hi there!")
            user("How are you?")
        }
        assertEquals(4, messages.size)
        assertEquals(Role.system, messages[0].role)
        assertEquals(Role.user, messages[1].role)
        assertEquals(Role.assistant, messages[2].role)
        assertEquals(Role.user, messages[3].role)
    }
}
