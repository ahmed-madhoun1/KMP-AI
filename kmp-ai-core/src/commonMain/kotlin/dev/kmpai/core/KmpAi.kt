package dev.kmpai.core

/**
 * Entry point for creating AI clients.
 *
 * Each provider has a factory function that accepts a configuration DSL:
 *
 * ```kotlin
 * // OpenAI
 * val client = KmpAi.openAi {
 *     apiKey = "sk-..."
 *     timeout = 60_000
 *     logging = true
 * }
 *
 * // Anthropic
 * val client = KmpAi.anthropic {
 *     apiKey = "sk-ant-..."
 * }
 *
 * // Google Gemini
 * val client = KmpAi.gemini {
 *     apiKey = "AIza..."
 * }
 *
 * // Ollama (local)
 * val client = KmpAi.ollama {
 *     baseUrl = "http://localhost:11434"
 * }
 * ```
 *
 * Provider-specific factory functions are defined in their respective modules
 * (kmp-ai-openai, kmp-ai-anthropic, etc.) as extension functions on this object.
 */
object KmpAi
