package dev.kmpai.openai.internal.mappers

import dev.kmpai.core.internal.sharedJson
import dev.kmpai.core.models.AiModel
import dev.kmpai.core.models.AiProvider
import dev.kmpai.core.models.ChatRequest
import dev.kmpai.core.models.ChatResponse
import dev.kmpai.core.models.Choice
import dev.kmpai.core.models.ContentPart
import dev.kmpai.core.models.EmbeddingRequest
import dev.kmpai.core.models.EmbeddingResponse
import dev.kmpai.core.models.FinishReason
import dev.kmpai.core.models.FunctionCall
import dev.kmpai.core.models.FunctionCallDelta
import dev.kmpai.core.models.Message
import dev.kmpai.core.models.MessageContent
import dev.kmpai.core.models.MessageDelta
import dev.kmpai.core.models.Role
import dev.kmpai.core.models.StreamChunk
import dev.kmpai.core.models.ToolCall
import dev.kmpai.core.models.ToolCallDelta
import dev.kmpai.core.models.Usage
import dev.kmpai.openai.internal.dto.OpenAiChatRequest
import dev.kmpai.openai.internal.dto.OpenAiChatResponse
import dev.kmpai.openai.internal.dto.OpenAiEmbeddingRequest
import dev.kmpai.openai.internal.dto.OpenAiEmbeddingResponse
import dev.kmpai.openai.internal.dto.OpenAiFunctionCall
import dev.kmpai.openai.internal.dto.OpenAiFunctionDef
import dev.kmpai.openai.internal.dto.OpenAiMessage
import dev.kmpai.openai.internal.dto.OpenAiModelData
import dev.kmpai.openai.internal.dto.OpenAiStreamResponse
import dev.kmpai.openai.internal.dto.OpenAiTool
import dev.kmpai.openai.internal.dto.OpenAiToolCall
import dev.kmpai.openai.internal.dto.StreamOptions
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal fun ChatRequest.toOpenAiRequest(): OpenAiChatRequest = OpenAiChatRequest(
    model = model,
    messages = messages.map { it.toOpenAiMessage() },
    temperature = temperature,
    maxTokens = maxTokens,
    topP = topP,
    frequencyPenalty = frequencyPenalty,
    presencePenalty = presencePenalty,
    stop = stop,
    stream = if (stream) true else null,
    streamOptions = if (stream) StreamOptions(includeUsage = true) else null,
    tools = tools?.map { t ->
        OpenAiTool(
            function = OpenAiFunctionDef(
                name = t.function.name,
                description = t.function.description,
                parameters = t.function.parameters,
                strict = t.function.strict,
            )
        )
    },
    seed = seed,
    user = user,
)

internal fun Message.toOpenAiMessage(): OpenAiMessage {
    val contentJson = when (content) {
        is MessageContent.Text -> JsonPrimitive(content.text)
        is MessageContent.Parts -> JsonArray(content.parts.map { part ->
            when (part) {
                is ContentPart.TextPart -> buildJsonObject {
                    put("type", "text")
                    put("text", part.text)
                }
                is ContentPart.ImagePart -> buildJsonObject {
                    put("type", "image_url")
                    put("image_url", buildJsonObject {
                        put("url", part.imageUrl.url)
                        part.imageUrl.detail?.let { put("detail", it) }
                    })
                }
                is ContentPart.AudioPart -> buildJsonObject {
                    put("type", "input_audio")
                    put("input_audio", buildJsonObject {
                        put("data", part.audioUrl)
                        put("format", part.format)
                    })
                }
            }
        })
    }
    return OpenAiMessage(
        role = role.name,
        content = contentJson,
        name = name,
        toolCallId = toolCallId,
        toolCalls = toolCalls?.map { tc ->
            OpenAiToolCall(
                id = tc.id,
                function = OpenAiFunctionCall(tc.function.name, tc.function.arguments)
            )
        },
    )
}

internal fun OpenAiChatResponse.toCoreResponse(provider: AiProvider): ChatResponse = ChatResponse(
    id = id,
    model = model,
    provider = provider,
    choices = choices.map { c ->
        Choice(
            index = c.index,
            message = Message(
                role = Role.valueOf(c.message.role ?: "assistant"),
                content = when (val raw = c.message.content) {
                    is kotlinx.serialization.json.JsonPrimitive -> MessageContent.Text(raw.content)
                    null -> MessageContent.Text("")
                    else -> MessageContent.Text(raw.toString())
                },
                toolCalls = c.message.toolCalls?.map { tc ->
                    ToolCall(
                        id = tc.id,
                        function = FunctionCall(tc.function.name, tc.function.arguments)
                    )
                },
            ),
            finishReason = c.finishReason?.let { fr ->
                FinishReason.entries.firstOrNull { it.name == fr }
            },
        )
    },
    usage = usage?.let { Usage(it.promptTokens, it.completionTokens, it.totalTokens) },
    created = created,
)

internal fun OpenAiStreamResponse.toStreamChunk(): StreamChunk {
    val choice = choices.firstOrNull()
    return StreamChunk(
        id = id,
        delta = MessageDelta(
            role = choice?.delta?.role?.let { r -> Role.entries.firstOrNull { it.name == r } },
            content = choice?.delta?.content,
            toolCalls = choice?.delta?.toolCalls?.map { tc ->
                ToolCallDelta(
                    index = tc.index,
                    id = tc.id,
                    type = tc.type,
                    function = tc.function?.let { f ->
                        FunctionCallDelta(name = f.name, arguments = f.arguments)
                    },
                )
            },
        ),
        finishReason = choice?.finishReason?.let { fr ->
            FinishReason.entries.firstOrNull { it.name == fr }
        },
        usage = usage?.let { Usage(it.promptTokens, it.completionTokens, it.totalTokens) },
    )
}

internal fun EmbeddingRequest.toOpenAiRequest(): OpenAiEmbeddingRequest = OpenAiEmbeddingRequest(
    model = model,
    input = input,
    dimensions = dimensions,
    encodingFormat = encodingFormat,
    user = user,
)

internal fun OpenAiEmbeddingResponse.toCoreResponse(): EmbeddingResponse = EmbeddingResponse(
    model = model,
    embeddings = data.sortedBy { it.index }.map { it.embedding },
    usage = Usage(usage.promptTokens, usage.completionTokens, usage.totalTokens),
)

internal fun OpenAiModelData.toAiModel(provider: AiProvider): AiModel = AiModel(
    id = id,
    provider = provider,
    supportsStreaming = true,
    supportsTools = id.startsWith("gpt-") || id.startsWith("o"),
    supportsVision = id.contains("vision") || id.startsWith("gpt-4o") || id.startsWith("gpt-4-turbo"),
    supportsEmbeddings = id.startsWith("text-embedding"),
)
