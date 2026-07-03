# Contributing to KMP AI

Thank you for your interest in contributing! Here's how to get started.

## Development Setup

1. **Prerequisites**
   - JDK 17+
   - Android Studio / IntelliJ IDEA with Kotlin Multiplatform plugin
   - Xcode (macOS only, required for iOS targets)

2. **Clone and build**
   ```bash
   git clone https://github.com/ahmed-madhoun1/KMP-AI.git
   cd KMP-AI
   ./gradlew build
   ```

3. **Run tests**
   ```bash
   ./gradlew allTests
   ```

## Project Structure

- `kmp-ai-core` — Core interfaces and models. No provider-specific code here.
- `kmp-ai-openai` — OpenAI provider
- `kmp-ai-anthropic` — Anthropic provider
- `kmp-ai-gemini` — Google Gemini provider
- `kmp-ai-ollama` — Ollama (local) provider
- `kmp-ai-streaming` — Flow utilities for streaming
- `kmp-ai-tools` — Tool/function calling DSL
- `kmp-ai-multimodal` — Image and audio helpers

## Adding a New Provider

1. Create a new module `kmp-ai-<provider>/`
2. Apply the `kmp-library` and `publishing` convention plugins
3. Add the module to `settings.gradle.kts`
4. Implement `AiClient` from `kmp-ai-core`
5. Create internal DTO classes matching the provider's wire format
6. Add mapper functions between core models and DTOs
7. Add a factory extension on `KmpAi`
8. Write tests using Ktor's `MockEngine`

## Code Style

- Follow the Kotlin coding conventions
- Use `internal` for implementation details, `public` for the API surface
- No `println` or `System.out` — use Kermit logger
- Every public API needs KDoc

## Pull Request Process

1. Fork the repository
2. Create a feature branch from `main`
3. Write tests for your changes
4. Run `./gradlew allTests` and ensure everything passes
5. Open a pull request with a clear description

## License

By contributing, you agree that your contributions will be licensed under the Apache License 2.0.
