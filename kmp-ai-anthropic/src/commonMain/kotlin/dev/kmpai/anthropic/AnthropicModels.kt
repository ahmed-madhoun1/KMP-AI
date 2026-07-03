package dev.kmpai.anthropic

/**
 * Well-known Anthropic Claude model identifiers.
 *
 * Prefer these constants over raw strings to catch typos at compile time.
 * Updated for June 2026.
 */
object AnthropicModels {

    // ── Claude 4 series (2025-2026) ──────────────────────────────────────────
    const val CLAUDE_OPUS_4   = "claude-opus-4-0"
    const val CLAUDE_SONNET_4 = "claude-sonnet-4-5"
    const val CLAUDE_HAIKU_4  = "claude-haiku-4-0"

    // ── Claude 3.7 series (2025) ─────────────────────────────────────────────
    const val CLAUDE_3_7_SONNET = "claude-3-7-sonnet-20250219"

    // ── Claude 3.5 series ────────────────────────────────────────────────────
    const val CLAUDE_3_5_SONNET = "claude-3-5-sonnet-20241022"
    const val CLAUDE_3_5_HAIKU  = "claude-3-5-haiku-20241022"

    // ── Claude 3 series ──────────────────────────────────────────────────────
    const val CLAUDE_3_OPUS    = "claude-3-opus-20240229"
    const val CLAUDE_3_SONNET  = "claude-3-sonnet-20240229"
    const val CLAUDE_3_HAIKU   = "claude-3-haiku-20240307"
}
