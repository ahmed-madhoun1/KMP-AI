package dev.kmpai.core

import dev.kmpai.core.models.ChatRequest
import dev.kmpai.core.models.Message
import dev.kmpai.core.models.MessageContent
import dev.kmpai.core.models.Role
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChatRequestTest {

    @Test
    fun `ChatRequest holds model and messages`() {
        val request = ChatRequest(
            model = "gpt-4o",
            messages = listOf(
                Message(Role.user, MessageContent.Text("Hello"))
            )
        )
        assertEquals("gpt-4o", request.model)
        assertEquals(1, request.messages.size)
        assertEquals(Role.user, request.messages.first().role)
    }

    @Test
    fun `stream defaults to false`() {
        val request = ChatRequest(model = "gpt-4o", messages = emptyList())
        assertEquals(false, request.stream)
    }

    @Test
    fun `optional fields default to null`() {
        val request = ChatRequest(model = "test", messages = emptyList())
        assertNull(request.temperature)
        assertNull(request.maxTokens)
        assertNull(request.tools)
        assertNull(request.stop)
    }

    @Test
    fun `extraParams defaults to empty map`() {
        val request = ChatRequest(model = "test", messages = emptyList())
        assertTrue(request.extraParams.isEmpty())
    }
}
