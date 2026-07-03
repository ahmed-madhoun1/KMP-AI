package dev.kmpai.gemini

/**
 * Well-known Google Gemini model identifiers.
 *
 * Prefer these constants over raw strings to catch typos at compile time.
 * Updated for June 2026.
 */
object GeminiModels {

    // ── Gemini 2.5 series (2025-2026) ────────────────────────────────────────
    const val GEMINI_2_5_PRO        = "gemini-2.5-pro"
    const val GEMINI_2_5_FLASH      = "gemini-2.5-flash"
    const val GEMINI_2_5_FLASH_LITE = "gemini-2.5-flash-lite"

    // ── Gemini 2.0 series ────────────────────────────────────────────────────
    const val GEMINI_2_0_FLASH      = "gemini-2.0-flash"
    const val GEMINI_2_0_FLASH_LITE = "gemini-2.0-flash-lite"

    // ── Gemini 1.5 series ────────────────────────────────────────────────────
    const val GEMINI_1_5_PRO   = "gemini-1.5-pro"
    const val GEMINI_1_5_FLASH = "gemini-1.5-flash"

    // ── Embeddings ───────────────────────────────────────────────────────────
    const val TEXT_EMBEDDING_004 = "text-embedding-004"
    const val EMBEDDING_001      = "embedding-001"
}
