# KMP AI — Kotlin Multiplatform SDK: Complete Implementation Plan

---

## 1. Project Overview

**KMP AI** is an open-source Kotlin Multiplatform library that exposes a unified, type-safe, coroutine-first API for integrating AI model providers (OpenAI, Anthropic, Google Gemini, Ollama, etc.) into KMP applications targeting Android, iOS, JVM, Desktop, and WASM.

---

## 2. Technology Stack

| Layer | Choice | Reason |
|---|---|---|
| Language | Kotlin 2.x | KMP support, coroutines, sealed classes |
| Build system | Gradle 8.x (Kotlin DSL) | Official KMP support |
| Serialization | `kotlinx.serialization` | Multiplatform JSON |
| HTTP client | `ktor-client` | Multiplatform, streaming |
| Async | `kotlinx.coroutines` | First-class KMP |
| Testing | `kotlin.test` + Kotest | Multiplatform tests |
| Logging | `kermit` | Multiplatform logging |
| Publishing | Vanniktech Maven Publish Plugin | Maven Central |
| CI | GitHub Actions | Automated builds & tests |

---

## 3. Repository Structure

```
kmp-ai/
├── build.gradle.kts                  # Root build
├── settings.gradle.kts               # Module declarations
├── gradle/
│   ├── libs.versions.toml            # Version catalog
│   └── wrapper/
├── buildSrc/                         # Convention plugins
│   └── src/main/kotlin/
│       ├── kmp-library.gradle.kts    # Shared KMP target config
│       └── publishing.gradle.kts    # Shared publishing config
│
├── kmp-ai-core/                      # Core interfaces & models
├── kmp-ai-openai/                    # OpenAI provider
├── kmp-ai-anthropic/                 # Anthropic provider
├── kmp-ai-gemini/                    # Google Gemini provider
├── kmp-ai-ollama/                    # Ollama (local) provider
├── kmp-ai-streaming/                 # Streaming utilities
├── kmp-ai-tools/                     # Tool/function calling support
├── kmp-ai-multimodal/               # Image/audio input support
│
├── sample-android/                   # Android sample app
├── sample-ios/                       # iOS sample (SwiftUI)
├── sample-desktop/                   # Desktop (Compose) sample
│
└── docs/                             # MkDocs documentation
```

---

## 4. Gradle / Build Configuration

### 4.1 `settings.gradle.kts`

```kotlin
rootProject.name = "kmp-ai"

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

include(
    ":kmp-ai-core",
    ":kmp-ai-openai",
    ":kmp-ai-anthropic",
    ":kmp-ai-gemini",
    ":kmp-ai-ollama",
    ":kmp-ai-streaming",
    ":kmp-ai-tools",
    ":kmp-ai-multimodal",
    ":sample-android",
    ":sample-desktop",
)
```

### 4.2 `gradle/libs.versions.toml` (Version Catalog)

```toml
[versions]
kotlin = "2.1.0"
ktor = "3.1.0"
kotlinx-coroutines = "1.9.0"
kotlinx-serialization = "1.7.3"
kermit = "2.0.4"
kotest = "5.9.1"
android-minSdk = "24"
android-compileSdk = "35"
vanniktech-publish = "0.30.0"

[libraries]
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-cio = { module = "io.ktor:ktor-client-cio", version.ref = "ktor" }
ktor-client-darwin = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }
ktor-client-android = { module = "io.ktor:ktor-client-android", version.ref = "ktor" }
ktor-client-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }
ktor-client-logging = { module = "io.ktor:ktor-client-logging", version.ref = "ktor" }

coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinx-coroutines" }
serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" }

kermit = { module = "co.touchlab:kermit", version.ref = "kermit" }

kotest-framework-engine = { module = "io.kotest:kotest-framework-engine", version.ref = "kotest" }
kotest-assertions = { module = "io.kotest:kotest-assertions-core", version.ref = "kotest" }

[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
android-library = { id = "com.android.library", version = "8.7.0" }
vanniktech-publish = { id = "com.vanniktech.maven.publish", version.ref = "vanniktech-publish" }
```

### 4.3 Convention Plugin: `buildSrc/src/main/kotlin/kmp-library.gradle.kts`

```kotlin
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions { jvmTarget = "17" }
        }
        publishLibraryVariants("release")
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    jvm {
        compilations.all {
            kotlinOptions { jvmTarget = "17" }
        }
    }

    // Optional: WASM JS for future browser support
    // wasmJs { browser() }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.coroutines.core)
            implementation(libs.serialization.json)
            implementation(libs.kermit)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotest.framework.engine)
            implementation(libs.kotest.assertions)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.android)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.cio)
        }
    }
}

android {
    namespace = "dev.kmpai.${project.name.replace("-", ".")}"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}
```

---

## 5. Module: `kmp-ai-core`

This is the heart of the SDK — pure interfaces and models, no HTTP, no provider logic.

### 5.1 Core AI Client Interface

```kotlin
// kmp-ai-core/src/commonMain/kotlin/dev/kmpai/core/AiClient.kt

package dev.kmpai.core

import kotlinx.coroutines.flow.Flow

/**
 * The unified AI client interface. All providers implement this.
 */
interface AiClient {

    /** The provider this client connects to. */
    val provider: AiProvider

    /**
     * Send a chat completion request and receive a single response.
     */
    suspend fun chat(request: ChatRequest): ChatResponse

    /**
     * Send a chat completion request and receive a streaming response.
     * Emits [StreamChunk] as tokens arrive.
     */
    fun chatStream(request: ChatRequest): Flow<StreamChunk>

    /**
     * Generate an embedding vector for the given input.
     */
    suspend fun embed(request: EmbeddingRequest): EmbeddingResponse

    /**
     * List models available from this provider.
     */
    suspend fun listModels(): List<AiModel>

    /**
     * Close underlying resources (HTTP client, connections).
     */
    fun close()
}
```

### 5.2 Request & Response Models

```kotlin
// kmp-ai-core/src/commonMain/kotlin/dev/kmpai/core/models/ChatRequest.kt

package dev.kmpai.core.models

import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double? = null,
    val maxTokens: Int? = null,
    val topP: Double? = null,
    val frequencyPenalty: Double? = null,
    val presencePenalty: Double? = null,
    val stop: List<String>? = null,
    val stream: Boolean = false,
    val tools: List<Tool>? = null,          // Function calling
    val toolChoice: ToolChoice? = null,
    val responseFormat: ResponseFormat? = null,
    val extraParams: Map<String, String> = emptyMap(), // Provider-specific
)

@Serializable
data class Message(
    val role: Role,
    val content: MessageContent,
    val name: String? = null,
    val toolCallId: String? = null,
)

@Serializable
sealed class MessageContent {
    @Serializable data class Text(val text: String) : MessageContent()
    @Serializable data class Parts(val parts: List<ContentPart>) : MessageContent()
}

@Serializable
sealed class ContentPart {
    @Serializable data class TextPart(val text: String) : ContentPart()
    @Serializable data class ImagePart(val imageUrl: ImageUrl) : ContentPart()
    @Serializable data class AudioPart(val audioUrl: String, val format: String) : ContentPart()
}

@Serializable
data class ImageUrl(
    val url: String,               // https:// or data:image/...;base64,...
    val detail: String? = "auto",
)

@Serializable
enum class Role { system, user, assistant, tool }

@Serializable
data class ChatResponse(
    val id: String,
    val model: String,
    val provider: AiProvider,
    val choices: List<Choice>,
    val usage: Usage?,
    val created: Long,
)

@Serializable
data class Choice(
    val index: Int,
    val message: Message,
    val finishReason: FinishReason?,
)

@Serializable
enum class FinishReason { stop, length, tool_calls, content_filter, error }

@Serializable
data class Usage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
)

// Streaming
@Serializable
data class StreamChunk(
    val id: String,
    val delta: MessageDelta,
    val finishReason: FinishReason?,
    val usage: Usage?,
)

@Serializable
data class MessageDelta(
    val role: Role? = null,
    val content: String? = null,
    val toolCalls: List<ToolCallDelta>? = null,
)
```

### 5.3 Tool / Function Calling Models

```kotlin
// kmp-ai-core/src/commonMain/kotlin/dev/kmpai/core/models/Tool.kt

@Serializable
data class Tool(
    val type: String = "function",
    val function: FunctionDefinition,
)

@Serializable
data class FunctionDefinition(
    val name: String,
    val description: String,
    val parameters: JsonObject,   // JSON Schema
    val strict: Boolean? = null,
)

@Serializable
sealed class ToolChoice {
    @Serializable object Auto : ToolChoice()
    @Serializable object None : ToolChoice()
    @Serializable object Required : ToolChoice()
    @Serializable data class Specific(val name: String) : ToolChoice()
}

@Serializable
data class ToolCall(
    val id: String,
    val type: String = "function",
    val function: FunctionCall,
)

@Serializable
data class FunctionCall(
    val name: String,
    val arguments: String,  // JSON string
)
```

### 5.4 Embedding Models

```kotlin
@Serializable
data class EmbeddingRequest(
    val model: String,
    val input: List<String>,
    val dimensions: Int? = null,
    val encodingFormat: String = "float",
)

@Serializable
data class EmbeddingResponse(
    val model: String,
    val embeddings: List<List<Double>>,
    val usage: Usage,
)
```

### 5.5 Provider & Model Metadata

```kotlin
@Serializable
data class AiProvider(
    val id: String,
    val name: String,
    val baseUrl: String,
)

@Serializable
data class AiModel(
    val id: String,
    val provider: AiProvider,
    val contextWindow: Int? = null,
    val maxOutputTokens: Int? = null,
    val supportsStreaming: Boolean = true,
    val supportsTools: Boolean = false,
    val supportsVision: Boolean = false,
)
```

### 5.6 Error Hierarchy

```kotlin
// kmp-ai-core/src/commonMain/kotlin/dev/kmpai/core/error/AiException.kt

sealed class AiException(message: String, cause: Throwable? = null) :
    Exception(message, cause) {

    /** HTTP 401 — bad or missing API key */
    class AuthenticationException(message: String) : AiException(message)

    /** HTTP 429 — rate limit hit */
    class RateLimitException(
        message: String,
        val retryAfterSeconds: Int? = null,
    ) : AiException(message)

    /** HTTP 4xx (not 401/429) — bad request */
    class InvalidRequestException(
        message: String,
        val statusCode: Int,
        val providerErrorCode: String? = null,
    ) : AiException(message)

    /** HTTP 5xx — provider side failure */
    class ProviderException(message: String, val statusCode: Int) : AiException(message)

    /** Network / timeout failure */
    class NetworkException(message: String, cause: Throwable) : AiException(message, cause)

    /** Response could not be parsed */
    class SerializationException(message: String, cause: Throwable? = null) : AiException(message, cause)

    /** Feature not supported by this provider */
    class UnsupportedOperationException(message: String) : AiException(message)
}
```

### 5.7 Configuration

```kotlin
data class AiClientConfig(
    val apiKey: String,
    val baseUrl: String? = null,        // Override default provider URL
    val organization: String? = null,   // OpenAI org ID
    val timeoutMs: Long = 30_000,
    val maxRetries: Int = 3,
    val retryDelayMs: Long = 1_000,
    val httpLoggingEnabled: Boolean = false,
    val defaultHeaders: Map<String, String> = emptyMap(),
)
```

---

## 6. Module: `kmp-ai-streaming`

Provides Flow utilities for streaming responses.

```kotlin
// Collect a full response from a streaming flow
suspend fun Flow<StreamChunk>.collectToResponse(): String {
    val sb = StringBuilder()
    collect { chunk ->
        chunk.delta.content?.let { sb.append(it) }
    }
    return sb.toString()
}

// Map stream to text deltas only
fun Flow<StreamChunk>.textDeltas(): Flow<String> =
    transform { chunk ->
        chunk.delta.content?.let { emit(it) }
    }

// Detect and handle tool calls in a stream
fun Flow<StreamChunk>.toolCallsFlow(): Flow<ToolCall> =
    transform { chunk ->
        chunk.delta.toolCalls?.forEach { emit(it.toToolCall()) }
    }
```

---

## 7. Module: `kmp-ai-openai`

### 7.1 Module `build.gradle.kts`

```kotlin
plugins {
    id("kmp-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":kmp-ai-core"))
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.client.logging)
        }
    }
}
```

### 7.2 OpenAI Client Implementation

```kotlin
// kmp-ai-openai/src/commonMain/kotlin/dev/kmpai/openai/OpenAiClient.kt

class OpenAiClient(
    private val config: AiClientConfig,
    httpClientEngine: HttpClientEngine? = null,
) : AiClient {

    override val provider = AiProvider(
        id = "openai",
        name = "OpenAI",
        baseUrl = config.baseUrl ?: "https://api.openai.com/v1",
    )

    private val httpClient = buildHttpClient(config, httpClientEngine)

    override suspend fun chat(request: ChatRequest): ChatResponse {
        val dto = request.toOpenAiDto()
        return try {
            val response: OpenAiChatResponse = httpClient.post("${provider.baseUrl}/chat/completions") {
                setBody(dto)
            }.body()
            response.toCoreResponse(provider)
        } catch (e: ClientRequestException) {
            throw e.toAiException()
        }
    }

    override fun chatStream(request: ChatRequest): Flow<StreamChunk> = flow {
        val dto = request.copy(stream = true).toOpenAiDto()
        httpClient.preparePost("${provider.baseUrl}/chat/completions") {
            setBody(dto)
        }.execute { response ->
            response.bodyAsChannel().consumeAsFlow()
                .parseServerSentEvents()
                .filterNot { it.data == "[DONE]" }
                .map { json.decodeFromString<OpenAiStreamChunk>(it.data) }
                .map { it.toStreamChunk() }
                .collect { emit(it) }
        }
    }

    override suspend fun embed(request: EmbeddingRequest): EmbeddingResponse {
        val dto = request.toOpenAiDto()
        return httpClient.post("${provider.baseUrl}/embeddings") {
            setBody(dto)
        }.body<OpenAiEmbeddingResponse>().toCoreResponse()
    }

    override suspend fun listModels(): List<AiModel> {
        return httpClient.get("${provider.baseUrl}/models")
            .body<OpenAiModelsResponse>()
            .data.map { it.toAiModel(provider) }
    }

    override fun close() = httpClient.close()
}
```

### 7.3 OpenAI DTOs (internal)

```kotlin
// Internal DTOs that map to OpenAI's wire format

@Serializable
internal data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val temperature: Double? = null,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    val stream: Boolean? = null,
    val tools: List<OpenAiTool>? = null,
    // ...
)

// Mapper extensions
internal fun ChatRequest.toOpenAiDto(): OpenAiChatRequest = OpenAiChatRequest(
    model = model,
    messages = messages.map { it.toOpenAiMessage() },
    temperature = temperature,
    maxTokens = maxTokens,
    stream = if (stream) true else null,
    // ...
)
```

---

## 8. Module: `kmp-ai-anthropic`

```kotlin
class AnthropicClient(
    private val config: AiClientConfig,
    httpClientEngine: HttpClientEngine? = null,
) : AiClient {

    override val provider = AiProvider(
        id = "anthropic",
        name = "Anthropic",
        baseUrl = config.baseUrl ?: "https://api.anthropic.com/v1",
    )

    // Anthropic uses x-api-key header instead of Bearer
    private val httpClient = buildHttpClient(config, httpClientEngine) {
        defaultRequest {
            header("x-api-key", config.apiKey)
            header("anthropic-version", "2023-06-01")
        }
    }

    override suspend fun chat(request: ChatRequest): ChatResponse {
        // Anthropic separates system message from conversation
        val systemMessage = request.messages
            .firstOrNull { it.role == Role.system }
            ?.content?.asText()

        val conversationMessages = request.messages
            .filter { it.role != Role.system }

        val dto = AnthropicMessageRequest(
            model = request.model,
            system = systemMessage,
            messages = conversationMessages.map { it.toAnthropicMessage() },
            maxTokens = request.maxTokens ?: 1024,
            // ...
        )

        return httpClient.post("${provider.baseUrl}/messages") {
            setBody(dto)
        }.body<AnthropicMessageResponse>().toCoreResponse(provider)
    }

    // streaming, embed, listModels ...
}
```

---

## 9. Module: `kmp-ai-gemini`

```kotlin
class GeminiClient(
    private val config: AiClientConfig,
    httpClientEngine: HttpClientEngine? = null,
) : AiClient {

    override val provider = AiProvider(
        id = "gemini",
        name = "Google Gemini",
        baseUrl = config.baseUrl ?: "https://generativelanguage.googleapis.com/v1beta",
    )

    // Gemini uses API key as query param
    override suspend fun chat(request: ChatRequest): ChatResponse {
        val modelPath = "models/${request.model}:generateContent"
        return httpClient.post("${provider.baseUrl}/$modelPath") {
            parameter("key", config.apiKey)
            setBody(request.toGeminiDto())
        }.body<GeminiResponse>().toCoreResponse(provider)
    }

    // Gemini streaming uses SSE with query param ?alt=sse
    override fun chatStream(request: ChatRequest): Flow<StreamChunk> = flow {
        val modelPath = "models/${request.model}:streamGenerateContent"
        httpClient.preparePost("${provider.baseUrl}/$modelPath") {
            parameter("key", config.apiKey)
            parameter("alt", "sse")
            setBody(request.toGeminiDto())
        }.execute { /* parse SSE */ }
    }
}
```

---

## 10. Module: `kmp-ai-ollama`

```kotlin
class OllamaClient(
    baseUrl: String = "http://localhost:11434",
    httpClientEngine: HttpClientEngine? = null,
) : AiClient {

    override val provider = AiProvider(
        id = "ollama",
        name = "Ollama",
        baseUrl = baseUrl,
    )

    // No API key needed for local Ollama
    private val config = AiClientConfig(apiKey = "", baseUrl = baseUrl)

    override suspend fun listModels(): List<AiModel> {
        return httpClient.get("${provider.baseUrl}/api/tags")
            .body<OllamaTagsResponse>()
            .models.map { it.toAiModel(provider) }
    }

    // chat, chatStream using /api/chat endpoint
}
```

---

## 11. Module: `kmp-ai-tools`

Type-safe tool/function definition DSL.

```kotlin
// Usage example:
val weatherTool = tool("get_weather") {
    description("Get current weather for a city")
    parameters {
        string("city") {
            description("The city name")
            required()
        }
        string("unit") {
            description("Temperature unit")
            enum("celsius", "fahrenheit")
        }
    }
}

// Execution
val toolExecutor = ToolExecutor {
    handle("get_weather") { args ->
        val city = args.getString("city")
        val unit = args.getStringOrNull("unit") ?: "celsius"
        // call weather API...
        """{"temperature": 22, "condition": "sunny"}"""
    }
}

// Automatic tool loop
val response = client.chatWithTools(
    request = ChatRequest(model = "gpt-4o", messages = messages, tools = listOf(weatherTool)),
    executor = toolExecutor,
    maxRounds = 5,
)
```

---

## 12. Module: `kmp-ai-multimodal`

Helpers for image and audio inputs.

```kotlin
// Image from URL
val imageMessage = message(Role.user) {
    text("Describe this image:")
    image("https://example.com/photo.jpg", detail = ImageDetail.High)
}

// Image from bytes (base64 encoded automatically)
val imageMessage2 = message(Role.user) {
    text("What's in this photo?")
    imageBytes(byteArray, mimeType = "image/jpeg")
}

// Audio input
val audioMessage = message(Role.user) {
    audioBytes(audioByteArray, format = "mp3")
}
```

---

## 13. Public API / Developer Experience

### 13.1 Factory / Builder Pattern

```kotlin
// Fluent factory for each provider
val openAiClient = KmpAi.openAi {
    apiKey = "sk-..."
    organization = "org-..."
    timeout = 30.seconds
    retries = 3
    logging = true
}

val anthropicClient = KmpAi.anthropic {
    apiKey = "sk-ant-..."
}

val geminiClient = KmpAi.gemini {
    apiKey = "AIza..."
}

val ollamaClient = KmpAi.ollama {
    baseUrl = "http://192.168.1.100:11434"
}
```

### 13.2 Conversation Builder DSL

```kotlin
val conversation = conversation {
    system("You are a helpful assistant specializing in Kotlin.")
    user("What is a sealed class?")
    assistant("A sealed class is...")
    user("Can you show an example?")
}

val response = client.chat(
    ChatRequest(model = "gpt-4o", messages = conversation)
)
```

### 13.3 Stateful Chat Session

```kotlin
val session = client.session(model = "claude-3-5-sonnet-20241022")

session.send("Hello, who are you?")  // Returns ChatResponse
session.send("What did I just ask?") // Remembers context
val history = session.history         // All messages so far
session.clear()                       // Reset
```

### 13.4 Streaming DSL

```kotlin
// Collect to string
val text = client.chatStream(request).collectToResponse()

// Process token by token
client.chatStream(request).textDeltas().collect { token ->
    print(token)
}

// With Compose / Flow
@Composable
fun StreamingText(client: AiClient) {
    val text by client.chatStream(request)
        .textDeltas()
        .runningFold("") { acc, token -> acc + token }
        .collectAsState(initial = "")
    
    Text(text)
}
```

---

## 14. Model Presets

```kotlin
object OpenAiModels {
    const val GPT_4O = "gpt-4o"
    const val GPT_4O_MINI = "gpt-4o-mini"
    const val GPT_4_TURBO = "gpt-4-turbo"
    const val O1 = "o1"
    const val O3_MINI = "o3-mini"
    const val TEXT_EMBEDDING_3_LARGE = "text-embedding-3-large"
    const val TEXT_EMBEDDING_3_SMALL = "text-embedding-3-small"
}

object AnthropicModels {
    const val CLAUDE_3_5_SONNET = "claude-3-5-sonnet-20241022"
    const val CLAUDE_3_5_HAIKU = "claude-3-5-haiku-20241022"
    const val CLAUDE_3_OPUS = "claude-3-opus-20240229"
}

object GeminiModels {
    const val GEMINI_2_0_FLASH = "gemini-2.0-flash"
    const val GEMINI_1_5_PRO = "gemini-1.5-pro"
    const val GEMINI_1_5_FLASH = "gemini-1.5-flash"
    const val TEXT_EMBEDDING_004 = "text-embedding-004"
}
```

---

## 15. Testing Strategy

### 15.1 Unit Tests (common)

```kotlin
// kmp-ai-core/src/commonTest/kotlin/dev/kmpai/core/ChatRequestTest.kt
class ChatRequestTest : StringSpec({
    "messages are included in request" {
        val request = ChatRequest(
            model = "gpt-4o",
            messages = listOf(Message(Role.user, MessageContent.Text("Hello")))
        )
        request.messages shouldHaveSize 1
    }
})
```

### 15.2 Mock HTTP Engine for Provider Tests

```kotlin
// kmp-ai-openai/src/commonTest/kotlin/dev/kmpai/openai/OpenAiClientTest.kt
class OpenAiClientTest : StringSpec({
    "returns parsed chat response" {
        val mockEngine = MockEngine { request ->
            respond(
                content = ByteReadChannel(MOCK_OPENAI_RESPONSE),
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "application/json")
            )
        }
        val client = OpenAiClient(
            config = AiClientConfig(apiKey = "test-key"),
            httpClientEngine = mockEngine,
        )
        val response = client.chat(
            ChatRequest(model = "gpt-4o", messages = listOf(
                Message(Role.user, MessageContent.Text("Hi"))
            ))
        )
        response.choices shouldHaveSize 1
        response.provider.id shouldBe "openai"
    }
})
```

### 15.3 Integration Tests (optional, behind flag)

```kotlin
// Requires real API key, run with -Pintegration=true
@OptIn(ExperimentalCoroutinesApi::class)
class OpenAiIntegrationTest : StringSpec({
    val apiKey = System.getenv("OPENAI_API_KEY") ?: return@StringSpec

    "streams a response" {
        val client = KmpAi.openAi { this.apiKey = apiKey }
        val text = client.chatStream(
            ChatRequest(model = "gpt-4o-mini",
                messages = listOf(Message(Role.user, MessageContent.Text("Say hi"))))
        ).collectToResponse()
        text shouldNotBeEmpty()
        client.close()
    }
})
```

---

## 16. Publishing to Maven Central

### 16.1 `buildSrc/src/main/kotlin/publishing.gradle.kts`

```kotlin
plugins {
    id("com.vanniktech.maven.publish")
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    signAllPublications()

    coordinates(
        groupId = "dev.kmpai",
        artifactId = project.name,
        version = project.version.toString(),
    )

    pom {
        name = "KMP AI — ${project.name}"
        description = "Kotlin Multiplatform AI SDK — ${project.name}"
        url = "https://github.com/your-org/kmp-ai"
        licenses {
            license {
                name = "Apache License 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0"
            }
        }
        developers {
            developer { id = "yourhandle"; name = "Your Name" }
        }
        scm {
            url = "https://github.com/your-org/kmp-ai"
        }
    }
}
```

### 16.2 Artifact coordinates (consumers add these)

```kotlin
// build.gradle.kts (consumer app)
dependencies {
    // Core only (bring your own provider)
    implementation("dev.kmpai:kmp-ai-core:0.1.0")

    // Specific provider
    implementation("dev.kmpai:kmp-ai-openai:0.1.0")
    implementation("dev.kmpai:kmp-ai-anthropic:0.1.0")
    implementation("dev.kmpai:kmp-ai-gemini:0.1.0")
    implementation("dev.kmpai:kmp-ai-ollama:0.1.0")

    // Optional extras
    implementation("dev.kmpai:kmp-ai-tools:0.1.0")
    implementation("dev.kmpai:kmp-ai-multimodal:0.1.0")
    implementation("dev.kmpai:kmp-ai-streaming:0.1.0")
}
```

---

## 17. CI/CD (GitHub Actions)

### `.github/workflows/build.yml`

```yaml
name: Build & Test

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  build:
    runs-on: macos-latest  # Required for iOS targets
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3

      - name: Run tests
        run: ./gradlew allTests

      - name: Build all modules
        run: ./gradlew build

      - name: Upload test results
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: test-results
          path: '**/build/reports/tests/'
```

### `.github/workflows/publish.yml`

```yaml
name: Publish to Maven Central

on:
  release:
    types: [published]

jobs:
  publish:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '17', distribution: 'temurin' }
      - uses: gradle/actions/setup-gradle@v3
      - name: Publish
        run: ./gradlew publishAllPublicationsToMavenCentral
        env:
          ORG_GRADLE_PROJECT_mavenCentralUsername: ${{ secrets.MAVEN_CENTRAL_USERNAME }}
          ORG_GRADLE_PROJECT_mavenCentralPassword: ${{ secrets.MAVEN_CENTRAL_PASSWORD }}
          ORG_GRADLE_PROJECT_signingInMemoryKey: ${{ secrets.SIGNING_KEY }}
          ORG_GRADLE_PROJECT_signingInMemoryKeyPassword: ${{ secrets.SIGNING_PASSWORD }}
```

---

## 18. Documentation

Use **MkDocs + Material theme** with API reference generated from KDoc via **Dokka**.

```
docs/
├── mkdocs.yml
├── docs/
│   ├── index.md                  # Overview + quick start
│   ├── getting-started.md        # Installation, first call
│   ├── providers/
│   │   ├── openai.md
│   │   ├── anthropic.md
│   │   ├── gemini.md
│   │   └── ollama.md
│   ├── guides/
│   │   ├── streaming.md
│   │   ├── tool-calling.md
│   │   ├── multimodal.md
│   │   ├── embeddings.md
│   │   └── error-handling.md
│   └── api/                      # Dokka-generated HTML
```

**Dokka setup:**
```kotlin
// root build.gradle.kts
plugins {
    id("org.jetbrains.dokka") version "1.9.20"
}
```

---

## 19. Phased Roadmap

| Phase | Milestone | Deliverables |
|---|---|---|
| **0** | Project setup | Repo, Gradle multi-module, CI, buildSrc convention plugins |
| **1** | Core API | `kmp-ai-core` interfaces, all models, error hierarchy, config |
| **2** | OpenAI provider | `kmp-ai-openai` with chat, streaming, embeddings, tool calls |
| **3** | Streaming utilities | `kmp-ai-streaming` Flow extensions |
| **4** | Anthropic provider | `kmp-ai-anthropic` with chat, streaming |
| **5** | Gemini provider | `kmp-ai-gemini` with chat, streaming, multimodal |
| **6** | Ollama provider | `kmp-ai-ollama` for local/on-device models |
| **7** | Tool calling | `kmp-ai-tools` DSL + auto tool loop |
| **8** | Multimodal | `kmp-ai-multimodal` image/audio helpers |
| **9** | Sessions | `ConversationSession` stateful wrapper |
| **10** | Samples | Android, iOS, Desktop reference apps |
| **11** | Documentation | MkDocs site, Dokka API docs |
| **12** | v0.1.0 release | Maven Central publish, GitHub release |

---

## 20. Recommended Tools & IDEs

| Tool | Purpose |
|---|---|
| IntelliJ IDEA / Android Studio | Primary IDE for Kotlin |
| Xcode | Required for iOS compilation (macOS only) |
| Kotlin Multiplatform Plugin | IDE plugin for KMP projects |
| Gradle Wrapper | Always use `./gradlew`, never system Gradle |
| GitHub | Source hosting + CI |
| Sonatype Central Portal | Maven Central publishing |
| MkDocs Material | Documentation site |

---

## 21. Key Design Decisions

1. **No reflection** — all serialization uses `kotlinx.serialization` with compile-time processors. Works on iOS (no JVM reflection).
2. **Ktor for HTTP** — the only production-ready multiplatform HTTP client. CIO engine for JVM/Desktop, Darwin for iOS, Android engine for Android.
3. **`extraParams` escape hatch** — every `ChatRequest` accepts a `Map<String, String>` for provider-specific parameters that don't have a typed field yet.
4. **Internal DTOs per provider** — each provider module has its own `@Serializable` DTOs that match the wire format exactly, plus mapper extensions to/from core models. Core never knows about provider-specific JSON shapes.
5. **No global state** — clients are stateless objects. Consumers manage lifecycle (create once, close when done). No static singletons.
6. **Suspend + Flow only** — no callbacks, no RxJava, no Combine. Kotlin-native async everywhere.
