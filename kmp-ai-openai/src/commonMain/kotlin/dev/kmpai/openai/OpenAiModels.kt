package dev.kmpai.openai

/**
 * Well-known OpenAI model identifiers.
 *
 * Prefer these constants over raw strings to catch typos at compile time.
 * Updated for June 2026.
 */
object OpenAiModels {

    // ── GPT-4.1 series (2025) ────────────────────────────────────────────────
    const val GPT_4_1         = "gpt-4.1"
    const val GPT_4_1_MINI    = "gpt-4.1-mini"
    const val GPT_4_1_NANO    = "gpt-4.1-nano"

    // ── GPT-4o series ────────────────────────────────────────────────────────
    const val GPT_4O          = "gpt-4o"
    const val GPT_4O_MINI     = "gpt-4o-mini"

    // ── o-series reasoning models ────────────────────────────────────────────
    const val O3              = "o3"
    const val O3_MINI         = "o3-mini"
    const val O4_MINI         = "o4-mini"

    // ── Embeddings ───────────────────────────────────────────────────────────
    const val TEXT_EMBEDDING_3_LARGE = "text-embedding-3-large"
    const val TEXT_EMBEDDING_3_SMALL = "text-embedding-3-small"
    const val TEXT_EMBEDDING_ADA_002 = "text-embedding-ada-002"
}
