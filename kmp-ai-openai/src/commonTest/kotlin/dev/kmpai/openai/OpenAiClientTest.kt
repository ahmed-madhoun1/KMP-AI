package dev.kmpai.openai

import dev.kmpai.core.AiClientConfig
import dev.kmpai.core.models.ChatRequest
import dev.kmpai.core.models.Message
import dev.kmpai.core.models.MessageContent
import dev.kmpai.core.models.Role
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class OpenAiClientTest {

    private val mockChatResponse = """
        {
          "id": "chatcmpl-test123",
          "object": "chat.completion",
          "created": 1234567890,
          "model": "gpt-4o",
          "choices": [{
            "index": 0,
            "message": {
              "role": "assistant",
              "content": "Hello! How can I help you today?"
            },
            "finish_reason": "stop"
          }],
          "usage": {
            "prompt_tokens": 10,
            "completion_tokens": 9,
            "total_tokens": 19
          }
        }
    """.trimIndent()

    private val mockErrorResponse = """
        {
          "error": {
            "message": "Invalid API key",
            "type": "invalid_request_error",
            "code": "invalid_api_key"
          }
        }
    """.trimIndent()

    private fun createClient(
        responseBody: String = mockChatResponse,
        status: HttpStatusCode = HttpStatusCode.OK,
    ): OpenAiClient {
        val engine = MockEngine { _ ->
            respond(
                content = responseBody,
                status = status,
                headers = headersOf("Content-Type", "application/json"),
            )
        }
        return OpenAiClient(
            config = AiClientConfig(apiKey = "sk-test"),
            httpClientEngine = engine,
        )
    }

    @Test
    fun `chat returns parsed response`() = runTest {
        val client = createClient()
        val response = client.chat(
            ChatRequest(
                model = OpenAiModels.GPT_4O,
                messages = listOf(Message(Role.user, MessageContent.Text("Hello!"))),
            )
        )
        assertEquals("chatcmpl-test123", response.id)
        assertEquals("gpt-4o", response.model)
        assertEquals(1, response.choices.size)
        assertEquals("Hello! How can I help you today?", response.choices[0].message.content.let {
            (it as MessageContent.Text).text
        })
        assertEquals("openai", response.provider.id)
        assertNotNull(response.usage)
        assertEquals(19, response.usage!!.totalTokens)
    }

    @Test
    fun `chat throws AuthenticationException on 401`() = runTest {
        val client = createClient(
            responseBody = mockErrorResponse,
            status = HttpStatusCode.Unauthorized,
        )
        assertFailsWith<dev.kmpai.core.error.AiException.AuthenticationException> {
            client.chat(
                ChatRequest(
                    model = OpenAiModels.GPT_4O,
                    messages = listOf(Message(Role.user, MessageContent.Text("Hello!"))),
                )
            )
        }
    }

    @Test
    fun `provider id is openai`() {
        val client = createClient()
        assertEquals("openai", client.provider.id)
        assertEquals("OpenAI", client.provider.name)
    }
}
