package dev.kmpai.openai

/** Well-known OpenAI model identifiers. */
object OpenAiModels {
    // GPT-4o
    const val GPT_4O = "gpt-4o"
    const val GPT_4O_MINI = "gpt-4o-mini"
    const val GPT_4O_MINI_REALTIME = "gpt-4o-mini-realtime-preview"

    // o-series reasoning models
    const val O1 = "o1"
    const val O1_MINI = "o1-mini"
    const val O1_PREVIEW = "o1-preview"
    const val O3_MINI = "o3-mini"

    // GPT-4 Turbo
    const val GPT_4_TURBO = "gpt-4-turbo"
    const val GPT_4_TURBO_PREVIEW = "gpt-4-turbo-preview"

    // GPT-3.5
    const val GPT_3_5_TURBO = "gpt-3.5-turbo"

    // Embeddings
    const val TEXT_EMBEDDING_3_LARGE = "text-embedding-3-large"
    const val TEXT_EMBEDDING_3_SMALL = "text-embedding-3-small"
    const val TEXT_EMBEDDING_ADA_002 = "text-embedding-ada-002"
}
